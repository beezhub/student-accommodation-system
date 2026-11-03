# Images built once on GitHub's ARM runners and pulled by digest by NONPROD and PROD (#337).
locals {
  repositories = ["student-accommodation-backend", "student-accommodation-frontend"]
}

resource "aws_ecr_repository" "this" {
  for_each = toset(local.repositories)
  name     = each.value

  # Deploys pull by digest, which never moves, so a re-pushed tag cannot change what runs.
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }
}

resource "aws_ecr_lifecycle_policy" "this" {
  for_each   = aws_ecr_repository.this
  repository = each.value.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep the last 20 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 20
      }
      action = { type = "expire" }
    }]
  })
}
