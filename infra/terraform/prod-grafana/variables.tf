variable "grafana_url" {
  description = "The PROD Grafana Cloud stack (docs/PROD_RUNBOOK.md)."
  type        = string
  default     = "https://friendlysamosa508.grafana.net"
}

variable "grafana_auth" {
  description = "Token of a stack service account with the Editor role. Passed as TF_VAR_grafana_auth, never written to a file (README.md)."
  type        = string
  sensitive   = true
}

variable "prometheus_data_source" {
  description = "Name of the stack's own Prometheus data source, where Alloy's remote write lands."
  type        = string
  default     = "grafanacloud-friendlysamosa508-prom"
}

variable "alert_email" {
  description = "Where Grafana Cloud emails PROD alerts. Set in the git-ignored terraform.tfvars, like prod/'s alarm_email."
  type        = string
}
