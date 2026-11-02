# The SES identity was created and verified by hand in #330, before this configuration existed.
# The import block adopts it instead of creating a second one; the DKIM records it needs are in
# the beezhub.co.za zone (#344).
import {
  to = aws_sesv2_email_identity.sending_domain
  id = var.sending_domain
}

resource "aws_sesv2_email_identity" "sending_domain" {
  email_identity = var.sending_domain

  dkim_signing_attributes {
    next_signing_key_length = "RSA_2048_BIT"
  }

  lifecycle {
    prevent_destroy = true
  }
}

# The backend sends through Spring's SMTP client, and SES SMTP authenticates with credentials
# derived from an IAM user's access key: an instance role cannot be used. The user can send mail
# from no-reply@ and do nothing else.
resource "aws_iam_user" "smtp" {
  name = "${var.name}-ses-smtp"
}

data "aws_iam_policy_document" "smtp" {
  statement {
    actions   = ["ses:SendRawEmail"]
    resources = [aws_sesv2_email_identity.sending_domain.arn]

    condition {
      test     = "StringEquals"
      variable = "ses:FromAddress"
      values   = ["no-reply@${var.sending_domain}"]
    }
  }
}

resource "aws_iam_user_policy" "smtp" {
  name   = "send-from-no-reply"
  user   = aws_iam_user.smtp.name
  policy = data.aws_iam_policy_document.smtp.json
}

resource "aws_iam_access_key" "smtp" {
  user = aws_iam_user.smtp.name
}

# Read by the deploy to write the host .env (#336, #338). The password is a SecureString; it is
# also in Terraform state, which is why that state lives only in the encrypted state bucket.
resource "aws_ssm_parameter" "mail_host" {
  name  = "/${var.name}/MAIL_HOST"
  type  = "String"
  value = "email-smtp.${var.region}.amazonaws.com"
}

resource "aws_ssm_parameter" "mail_username" {
  name  = "/${var.name}/MAIL_USERNAME"
  type  = "String"
  value = aws_iam_access_key.smtp.id
}

resource "aws_ssm_parameter" "mail_password" {
  name  = "/${var.name}/MAIL_PASSWORD"
  type  = "SecureString"
  value = aws_iam_access_key.smtp.ses_smtp_password_v4
}
