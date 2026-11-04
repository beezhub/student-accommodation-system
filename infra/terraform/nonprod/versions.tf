terraform {
  required_version = "~> 1.16.6"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "6.69.1"
    }
  }

  # The same bucket as PROD with its own key, so a NONPROD plan never touches a PROD resource.
  backend "s3" {
    bucket       = "beezhub-sas-terraform-state"
    key          = "nonprod/terraform.tfstate"
    region       = "af-south-1"
    encrypt      = true
    use_lockfile = true
  }
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project     = "student-accommodation"
      Environment = "nonprod"
      ManagedBy   = "terraform"
    }
  }
}
