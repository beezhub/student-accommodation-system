terraform {
  required_version = "~> 1.16.6"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "6.69.1"
    }
  }

  # Created by ../bootstrap. Native S3 locking (use_lockfile), so no DynamoDB table.
  backend "s3" {
    bucket       = "beezhub-sas-terraform-state"
    key          = "prod/terraform.tfstate"
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
      Environment = "prod"
      ManagedBy   = "terraform"
    }
  }
}
