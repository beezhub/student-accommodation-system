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

# Counts builds, not manifests (#355). Each build pushes three: the index tagged sha-<commit>,
# which deploys pull by digest, and two untagged children, the arm64 image and BuildKit's
# provenance attestation. A count over every manifest kept 6 or 7 builds, not 20.
#
# Rule 1 keeps the 20 newest builds by their tag. images.yml pushes no tag other than sha-, so
# nothing tagged falls outside it. Rule 2 removes untagged manifests a day after push; ECR does
# not expire a manifest that a kept index still references, so a kept build keeps its children,
# and an expired build's children go with the next run.
resource "aws_ecr_lifecycle_policy" "this" {
  for_each   = aws_ecr_repository.this
  repository = each.value.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep the last 20 builds (sha- tags)"
        selection = {
          tagStatus     = "tagged"
          tagPrefixList = ["sha-"]
          countType     = "imageCountMoreThan"
          countNumber   = 20
        }
        action = { type = "expire" }
      },
      {
        rulePriority = 2
        description  = "Expire untagged manifests 1 day after push"
        selection = {
          tagStatus   = "untagged"
          countType   = "sinceImagePushed"
          countUnit   = "days"
          countNumber = 1
        }
        action = { type = "expire" }
      },
    ]
  })
}
