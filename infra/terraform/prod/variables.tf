variable "region" {
  description = "Cape Town: student data stays in South Africa (POPIA). docs/PROD_ENVIRONMENT_PLAN.md."
  type        = string
  default     = "af-south-1"
}

variable "name" {
  description = "Prefix for resource names."
  type        = string
  default     = "sas-prod"
}

variable "hostname" {
  description = "Public hostname Caddy serves. Its A record points at the Elastic IP (#336, #344)."
  type        = string
  default     = "accommodation.beezhub.co.za"
}

variable "sending_domain" {
  description = "SES identity mail is sent from (no-reply@<domain>). Created and verified in #330."
  type        = string
  default     = "beezhub.co.za"
}

variable "alarm_email" {
  description = "Where CloudWatch alarms are emailed. AWS sends a confirmation link that must be clicked once."
  type        = string
}

variable "github_repository" {
  description = "owner/name of the repository whose workflows may assume the GitHub roles."
  type        = string
  default     = "beezhub/student-accommodation-system"
}

variable "instance_type" {
  description = "Graviton, so the arm64 images NONPROD runs work unchanged. Sized by #334."
  type        = string
  default     = "t4g.small"
}

variable "db_instance_class" {
  description = "RDS class. Sized by #334."
  type        = string
  default     = "db.t4g.micro"
}

variable "documents_bucket" {
  description = "Student documents and building images. S3 bucket names are global, hence the prefix."
  type        = string
  default     = "beezhub-sas-prod-documents"
}

variable "backups_bucket" {
  description = "Weekly pg_dump copies, kept outside RDS (#339)."
  type        = string
  default     = "beezhub-sas-prod-backups"
}
