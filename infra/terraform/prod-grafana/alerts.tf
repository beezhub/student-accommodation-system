# PROD alerts in Grafana Cloud (#340). They read what Alloy sends (tools/alloy/grafana-cloud.alloy):
# the backend's /actuator/prometheus and the server's own memory, swap, disk and CPU. Everything is
# filtered on env="prod", so a second environment shipping to this stack (#381) cannot set them off.
#
# What Grafana Cloud cannot see is in prod/monitoring.tf as CloudWatch alarms: EC2 status checks,
# CPU credits, and the RDS database.
#
# Each rule is one instant PromQL query (A) compared with a threshold (B). The query collapses to a
# single number with min() or max(), so an alert names the problem, not a container or a CPU.

data "grafana_data_source" "prometheus" {
  name = var.prometheus_data_source
}

resource "grafana_folder" "prod" {
  title = "PROD alerts"
}

# Rules route to this contact point directly (notification_settings below) instead of through the
# stack's notification policy tree, which grafana_notification_policy would overwrite whole.
resource "grafana_contact_point" "email" {
  name = "PROD alert email"

  email {
    addresses    = [var.alert_email]
    single_email = true
  }
}

locals {
  backend = "job=\"student-accommodation-backend\",env=\"prod\""
  # Scrapes of /actuator/prometheus are requests too: four a minute that say nothing about users.
  requests   = "http_server_requests_seconds_count{${local.backend},uri!~\"/actuator.*\"}"
  errors_5xx = "http_server_requests_seconds_count{${local.backend},uri!~\"/actuator.*\",status=~\"5..\"}"

  rule_groups = {
    server = [
      {
        title     = "Server memory almost full"
        expr      = "min(node_memory_MemAvailable_bytes{env=\"prod\"} / node_memory_MemTotal_bytes{env=\"prod\"})"
        op        = "lt"
        threshold = 0.10
        for       = "10m"
        severity  = "critical"
        no_data   = "Alerting"
        summary   = "Under 10% of the PROD server's memory has been available for 10 minutes. Containers are close to being killed for memory."
      },
      {
        # Swappiness is low (prod/user_data.sh), so swap in steady use means memory has run out,
        # even while the memory alert above has not fired yet.
        title     = "Server using swap"
        expr      = "max(1 - node_memory_SwapFree_bytes{env=\"prod\"} / node_memory_SwapTotal_bytes{env=\"prod\"})"
        op        = "gt"
        threshold = 0.50
        for       = "15m"
        severity  = "warning"
        no_data   = "Alerting"
        summary   = "Over half of the PROD server's 2 GB of swap has been in use for 15 minutes. The site slows down while it swaps."
      },
      {
        title     = "Server disk almost full"
        expr      = "min(node_filesystem_avail_bytes{env=\"prod\",mountpoint=\"/\"} / node_filesystem_size_bytes{env=\"prod\",mountpoint=\"/\"})"
        op        = "lt"
        threshold = 0.15
        for       = "15m"
        severity  = "warning"
        no_data   = "Alerting"
        summary   = "Under 15% of the PROD server's disk is free. Old images are the usual cause: docker image prune."
      },
    ]

    backend = [
      {
        # No data alerts too: it means Alloy, or its push to Grafana Cloud, has stopped, and then
        # none of the other rules can fire either.
        title     = "Backend not answering"
        expr      = "max(up{${local.backend}})"
        op        = "lt"
        threshold = 1
        for       = "3m"
        severity  = "critical"
        no_data   = "Alerting"
        summary   = "Alloy has not been able to scrape the PROD backend for 3 minutes, or no metrics are reaching Grafana Cloud at all."
      },
      {
        # Under 3 requests a minute the ratio is noise (one error in ten is 10%), so the query
        # returns nothing and the rule stays quiet. No data is therefore normal here, not a fault.
        title     = "Backend 5xx error rate above 1%"
        expr      = "(sum(rate(${local.errors_5xx}[5m])) / sum(rate(${local.requests}[5m]))) and on() (sum(rate(${local.requests}[5m])) > 0.05)"
        op        = "gt"
        threshold = 0.01
        for       = "5m"
        severity  = "critical"
        no_data   = "OK"
        summary   = "Over 1% of PROD requests have failed with a 5xx for 5 minutes. Check the backend logs in Grafana Cloud for the errors."
      },
    ]
  }
}

resource "grafana_rule_group" "prod" {
  for_each = local.rule_groups

  name             = each.key
  folder_uid       = grafana_folder.prod.uid
  interval_seconds = 60

  dynamic "rule" {
    for_each = each.value

    content {
      name           = rule.value.title
      condition      = "B"
      for            = rule.value.for
      no_data_state  = rule.value.no_data
      exec_err_state = "Error"
      labels         = { severity = rule.value.severity, env = "prod" }
      annotations    = { summary = rule.value.summary }

      notification_settings {
        contact_point   = grafana_contact_point.email.name
        group_by        = ["alertname"]
        repeat_interval = "4h"
      }

      data {
        ref_id         = "A"
        datasource_uid = data.grafana_data_source.prometheus.uid

        relative_time_range {
          from = 600
          to   = 0
        }

        model = jsonencode({
          refId   = "A"
          expr    = rule.value.expr
          instant = true
          range   = false
        })
      }

      data {
        ref_id         = "B"
        datasource_uid = "__expr__"

        relative_time_range {
          from = 0
          to   = 0
        }

        model = jsonencode({
          refId      = "B"
          type       = "threshold"
          expression = "A"
          datasource = { type = "__expr__", uid = "__expr__" }
          conditions = [{
            evaluator = { type = rule.value.op, params = [rule.value.threshold] }
          }]
        })
      }
    }
  }
}
