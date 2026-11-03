data "aws_caller_identity" "current" {}

locals {
  account_id      = data.aws_caller_identity.current.account_id
  ecr_arns        = [for repo in aws_ecr_repository.this : repo.arn]
  ssm_path_arn    = "arn:aws:ssm:${var.region}:${local.account_id}:parameter/${var.name}/*"
  db_secret_arn   = aws_db_instance.main.master_user_secret[0].secret_arn
  documents_arn   = aws_s3_bucket.this["documents"].arn
  backups_arn     = aws_s3_bucket.this["backups"].arn
  github_oidc_url = "token.actions.githubusercontent.com"
}

# ---------------------------------------------------------------------------------------------
# The server's role
# ---------------------------------------------------------------------------------------------

data "aws_iam_policy_document" "ec2_assume" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "app" {
  name               = "${var.name}-app"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume.json
}

# Session Manager instead of SSH, and the target for the deploy's SSM Run Command.
resource "aws_iam_role_policy_attachment" "app_ssm" {
  role       = aws_iam_role.app.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

data "aws_iam_policy_document" "app" {
  statement {
    sid       = "EcrLogin"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid       = "EcrPull"
    actions   = ["ecr:BatchGetImage", "ecr:GetDownloadUrlForLayer", "ecr:BatchCheckLayerAvailability"]
    resources = local.ecr_arns
  }

  # The backend's storage (#331). No s3:CreateBucket: Terraform owns the bucket and the backend
  # runs with S3_CREATE_BUCKET=false. ListBucket covers the health check's HeadBucket and the orphan
  # cleanup job's listing.
  statement {
    sid       = "DocumentsBucket"
    actions   = ["s3:ListBucket"]
    resources = [local.documents_arn]
  }

  statement {
    sid       = "DocumentsObjects"
    actions   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
    resources = ["${local.documents_arn}/*"]
  }

  # Weekly pg_dump copies (#339): write them, and read them back for a restore. No delete; the
  # lifecycle rule expires old dumps.
  statement {
    sid       = "BackupsBucket"
    actions   = ["s3:ListBucket"]
    resources = [local.backups_arn]
  }

  statement {
    sid       = "BackupsObjects"
    actions   = ["s3:GetObject", "s3:PutObject"]
    resources = ["${local.backups_arn}/*"]
  }

  # The deploy writes /opt/sas/.env from these on the host.
  statement {
    sid       = "DeploySettings"
    actions   = ["ssm:GetParameter", "ssm:GetParameters", "ssm:GetParametersByPath"]
    resources = [local.ssm_path_arn]
  }

  statement {
    sid       = "DatabasePassword"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [local.db_secret_arn]
  }
}

resource "aws_iam_role_policy" "app" {
  name   = "app"
  role   = aws_iam_role.app.id
  policy = data.aws_iam_policy_document.app.json
}

resource "aws_iam_instance_profile" "app" {
  name = "${var.name}-app"
  role = aws_iam_role.app.name
}

# ---------------------------------------------------------------------------------------------
# GitHub Actions, through OIDC: no AWS key is stored in GitHub
# ---------------------------------------------------------------------------------------------

resource "aws_iam_openid_connect_provider" "github" {
  url            = "https://${local.github_oidc_url}"
  client_id_list = ["sts.amazonaws.com"]
}

data "aws_iam_policy_document" "github_assume" {
  for_each = {
    # The image build (#337) runs on pushes to the two long-lived branches.
    ecr_push = [
      "repo:${var.github_repository}:ref:refs/heads/develop",
      "repo:${var.github_repository}:ref:refs/heads/main",
    ]
    # The PROD deploy (#338) runs in the production environment.
    prod_deploy = ["repo:${var.github_repository}:environment:production"]
    # The NONPROD deploy runs in the nonprod environment and hands the Oracle host an ECR login.
    nonprod_pull = ["repo:${var.github_repository}:environment:nonprod"]
  }

  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.github_oidc_url}:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.github_oidc_url}:sub"
      values   = each.value
    }
  }
}

resource "aws_iam_role" "github_ecr_push" {
  name               = "${var.name}-github-ecr-push"
  assume_role_policy = data.aws_iam_policy_document.github_assume["ecr_push"].json
}

data "aws_iam_policy_document" "github_ecr_push" {
  statement {
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:CompleteLayerUpload",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
    ]
    resources = local.ecr_arns
  }
}

resource "aws_iam_role_policy" "github_ecr_push" {
  name   = "push-images"
  role   = aws_iam_role.github_ecr_push.id
  policy = data.aws_iam_policy_document.github_ecr_push.json
}

resource "aws_iam_role" "github_prod_deploy" {
  name               = "${var.name}-github-deploy"
  assume_role_policy = data.aws_iam_policy_document.github_assume["prod_deploy"].json
}

# Run a shell script on the PROD server and read its result. Nothing else.
data "aws_iam_policy_document" "github_prod_deploy" {
  statement {
    actions = ["ssm:SendCommand"]
    resources = [
      aws_instance.app.arn,
      "arn:aws:ssm:${var.region}::document/AWS-RunShellScript",
    ]
  }

  statement {
    actions   = ["ssm:GetCommandInvocation", "ssm:ListCommandInvocations"]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "github_prod_deploy" {
  name   = "run-deploy"
  role   = aws_iam_role.github_prod_deploy.id
  policy = data.aws_iam_policy_document.github_prod_deploy.json
}

# NONPROD runs on Oracle Cloud, so it has no instance role. Its deploy job assumes this role and
# passes the host a 12-hour ECR login token over ssh (#337): pull the two images, nothing else.
resource "aws_iam_role" "github_nonprod_pull" {
  name               = "sas-nonprod-ecr-pull"
  assume_role_policy = data.aws_iam_policy_document.github_assume["nonprod_pull"].json
}

data "aws_iam_policy_document" "github_nonprod_pull" {
  statement {
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    actions   = ["ecr:BatchGetImage", "ecr:GetDownloadUrlForLayer", "ecr:BatchCheckLayerAvailability"]
    resources = local.ecr_arns
  }
}

resource "aws_iam_role_policy" "github_nonprod_pull" {
  name   = "pull-images"
  role   = aws_iam_role.github_nonprod_pull.id
  policy = data.aws_iam_policy_document.github_nonprod_pull.json
}
