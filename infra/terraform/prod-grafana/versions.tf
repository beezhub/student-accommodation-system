terraform {
  required_version = "~> 1.16.6"

  required_providers {
    grafana = {
      source  = "grafana/grafana"
      version = "4.50.0"
    }
  }

  # Same bucket as prod/, its own key. Kept apart from prod/ so an AWS change never needs the
  # Grafana token, and a Grafana change never plans against the AWS environment.
  backend "s3" {
    bucket       = "beezhub-sas-terraform-state"
    key          = "prod-grafana/terraform.tfstate"
    region       = "af-south-1"
    encrypt      = true
    use_lockfile = true
  }
}

provider "grafana" {
  url  = var.grafana_url
  auth = var.grafana_auth
}
