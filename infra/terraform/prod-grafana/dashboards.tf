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
# The queries carry no env filter. Only PROD ships to this stack today; if NONPROD joins it (#381),
# these dashboards need an env variable before their numbers mean anything.

locals {
  dashboards_dir = "${path.module}/../../../tools/grafana/dashboards"

  prometheus = {
    type = "prometheus"
    uid  = data.grafana_data_source.prometheus.uid
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
