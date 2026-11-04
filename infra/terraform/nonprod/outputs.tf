output "documents_bucket" {
  description = "S3_BUCKET in the NONPROD .env (#382)."
  value       = aws_s3_bucket.documents.bucket
}

output "storage_public_origin" {
  description = "STORAGE_PUBLIC_ORIGIN in the NONPROD .env: presigned links point at S3 itself, so the CSP must allow it."
  value       = "https://${aws_s3_bucket.documents.bucket}.s3.${var.region}.amazonaws.com"
}

output "mail_from" {
  description = "MAIL_FROM in the NONPROD .env, the only sender the app user may use."
  value       = local.mail_from
}

output "access_key_parameters" {
  description = "Where to read the app user's key. Values stay in SSM, never in Terraform output."
  value       = [aws_ssm_parameter.access_key_id.name, aws_ssm_parameter.secret_access_key.name]
}
