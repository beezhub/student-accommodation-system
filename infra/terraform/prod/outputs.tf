output "public_ip" {
  description = "The Elastic IP. The hostname's A record points here (#336, #344)."
  value       = aws_eip.app.public_ip
}

output "instance_id" {
  description = "For Session Manager (aws ssm start-session --target <id>) and SSM Run Command."
  value       = aws_instance.app.id
}

output "database_endpoint" {
  description = "host:port for DB_URL: jdbc:postgresql://<endpoint>/studentaccommodation?sslmode=require"
  value       = aws_db_instance.main.endpoint
}

output "database_password_secret_arn" {
  description = "Secrets Manager secret holding the RDS master username and password."
  value       = local.db_secret_arn
}

output "documents_bucket" {
  value = aws_s3_bucket.this["documents"].bucket
}

output "backups_bucket" {
  value = aws_s3_bucket.this["backups"].bucket
}

output "ecr_repositories" {
  value = { for name, repo in aws_ecr_repository.this : name => repo.repository_url }
}

output "github_ecr_push_role_arn" {
  description = "For aws-actions/configure-aws-credentials in the image build (#337)."
  value       = aws_iam_role.github_ecr_push.arn
}

output "github_deploy_role_arn" {
  description = "For aws-actions/configure-aws-credentials in the PROD deploy (#338)."
  value       = aws_iam_role.github_prod_deploy.arn
}

output "deploy_settings_path" {
  description = "SSM Parameter Store path the deploy reads the host .env from."
  value       = "/${var.name}/"
}

output "github_nonprod_pull_role_arn" {
  description = "For aws-actions/configure-aws-credentials in the NONPROD deploy (#337)."
  value       = aws_iam_role.github_nonprod_pull.arn
}
