# NONPROD runs on Oracle Cloud, so the backend has no AWS instance role and uses this user's access
# key instead (#378). The key sits in a file on that host, so it can do only what the backend calls.

locals {
  mail_from = "nonprod@${var.sending_domain}"
}

# Read rather than created: the identity belongs to ../prod. A missing one stops the plan here.
data "aws_sesv2_email_identity" "sending_domain" {
  email_identity = var.sending_domain
}

resource "aws_iam_user" "app" {
  name = "${var.name}-app"
}

data "aws_iam_policy_document" "app" {
  # No s3:CreateBucket: Terraform owns the bucket and the backend runs with S3_CREATE_BUCKET=false.
  # ListBucket covers the health check's HeadBucket and the orphan cleanup job's listing.
  statement {
    sid       = "DocumentsBucket"
    actions   = ["s3:ListBucket"]
    resources = [aws_s3_bucket.documents.arn]
  }

  # GetObject also covers HeadObject and the presigned download links.
  statement {
    sid       = "DocumentsObjects"
    actions   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
    resources = ["${aws_s3_bucket.documents.arn}/*"]
  }

  # Sending only from one address, so NONPROD mail can never pass as PROD's no-reply@.
  statement {
    sid       = "SendEmail"
    actions   = ["ses:SendEmail", "ses:SendRawEmail"]
    resources = [data.aws_sesv2_email_identity.sending_domain.arn]

    condition {
      test     = "StringEquals"
      variable = "ses:FromAddress"
      values   = [local.mail_from]
    }
  }

  # Spring Boot's mail health check asks SES whether sending is enabled (SesApiMailSender). This
  # action has no resource ARN, so it cannot be scoped.
  statement {
    sid       = "EmailHealth"
    actions   = ["ses:GetAccount"]
    resources = ["*"]
  }
}

resource "aws_iam_user_policy" "app" {
  name   = "documents-and-email"
  user   = aws_iam_user.app.name
  policy = data.aws_iam_policy_document.app.json
}

resource "aws_iam_access_key" "app" {
  user = aws_iam_user.app.name
}

# A person copies these into /opt/sas/.env (#382). The secret is a SecureString; it is also in
# Terraform state, which is why that state lives only in the encrypted state bucket.
resource "aws_ssm_parameter" "access_key_id" {
  name  = "/${var.name}/AWS_ACCESS_KEY_ID"
  type  = "String"
  value = aws_iam_access_key.app.id
}

resource "aws_ssm_parameter" "secret_access_key" {
  name  = "/${var.name}/AWS_SECRET_ACCESS_KEY"
  type  = "SecureString"
  value = aws_iam_access_key.app.secret
}
