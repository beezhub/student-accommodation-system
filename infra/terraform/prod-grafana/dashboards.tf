# PROD's copies of the dashboards NONPROD's Grafana provisions from tools/grafana/dashboards (#340).
# The JSON files stay the single source: each one is read as it is and changed on the way to the
# stack, so NONPROD's Grafana and its files are untouched.
#
# Two changes, both because Grafana Cloud is not NONPROD's Grafana:
# - Every panel and query points at the stack's Prometheus data source. The files name NONPROD's
#   (uid "prometheus", once "Prometheus"), which does not exist here.
# - Panels on any other data source are dropped. The only one today is the PostgreSQL row count in
#   backend-dashboard.json: the database is RDS in private subnets, out of Grafana Cloud's reach
#   (docs/PROD_ENVIRONMENT_PLAN.md).
#
# The queries carry no labels, and NONPROD shares this stack (#381). Every dashboard gets an ad hoc
# filter, which Grafana adds to each query on the data source, starting at env=prod. To see NONPROD,
# change the filter at the top of the dashboard; the saved copy still opens on PROD.

locals {
  dashboards_dir = "${path.module}/../../../tools/grafana/dashboards"

  prometheus = {
    type = "prometheus"
    uid  = data.grafana_data_source.prometheus.uid
  }

  env_filter = {
    name       = "env"
    type       = "adhoc"
    datasource = local.prometheus
    filters    = [{ key = "env", operator = "=", value = "prod" }]
  }

  source_dashboards = {
    for f in fileset(local.dashboards_dir, "*.json") :
    trimsuffix(f, ".json") => jsondecode(file("${local.dashboards_dir}/${f}"))
  }

  prod_dashboards = {
    for name, d in local.source_dashboards : name => merge(d, {
      # Grafana assigns its own id; a uid prefix keeps PROD's apart if NONPROD joins the stack.
      id  = null
      uid = "prod-${d.uid}"

      templating = { list = [local.env_filter] }

      panels = [
        for p in d.panels : merge(p, {
          datasource = local.prometheus
          targets    = [for t in p.targets : merge(t, { datasource = local.prometheus })]
        }) if p.datasource.type == "prometheus"
      ]
    })
  }
}

resource "grafana_folder" "prod_dashboards" {
  title = "PROD dashboards"
}

resource "grafana_dashboard" "prod" {
  for_each = local.prod_dashboards

  folder      = grafana_folder.prod_dashboards.uid
  config_json = jsonencode(each.value)
  overwrite   = true
}
