variable "region" {
  description = "The same region as PROD, so NONPROD exercises the same S3 and SES endpoints."
  type        = string
  default     = "af-south-1"
}

variable "name" {
  description = "Prefix for resource names."
  type        = string
  default     = "sas-nonprod"
}

variable "sending_domain" {
  description = "SES identity mail is sent from. Owned by ../prod; NONPROD only sends through it."
  type        = string
  default     = "beezhub.co.za"
}

variable "documents_bucket" {
  description = "Test documents and building images. S3 bucket names are global, hence the prefix."
  type        = string
  default     = "beezhub-sas-nonprod-documents"
}
