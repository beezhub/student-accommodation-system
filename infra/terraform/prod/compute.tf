# Canonical's current Ubuntu 24.04 arm64 image, read from the parameter AWS publishes for it.
data "aws_ssm_parameter" "ubuntu" {
  name = "/aws/service/canonical/ubuntu/server/24.04/stable/current/arm64/hvm/ebs-gp3/ami-id"
}

resource "aws_instance" "app" {
  ami                    = data.aws_ssm_parameter.ubuntu.insecure_value
  instance_type          = var.instance_type
  subnet_id              = aws_subnet.public.id
  vpc_security_group_ids = [aws_security_group.app.id]
  iam_instance_profile   = aws_iam_instance_profile.app.name
  user_data              = file("${path.module}/user_data.sh")

  # A person decides when the PROD server is destroyed, not a mistyped plan.
  disable_api_termination = true

  # Standard credits: a long busy spell slows the server down instead of adding charges.
  credit_specification {
    cpu_credits = "standard"
  }

  # IMDSv2 only. Hop limit 2 because the backend reads its S3 credentials from the instance role
  # inside a container on a bridge network, one hop further than the host (#331).
  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
  }

  root_block_device {
    volume_type           = "gp3"
    volume_size           = 15
    encrypted             = true
    delete_on_termination = true
  }

  tags = { Name = var.name }

  lifecycle {
    # Canonical publishes a new image every few weeks. Without this every one would replace the
    # server; the running one is patched in place by unattended-upgrades instead.
    ignore_changes = [ami, user_data]
  }
}

resource "aws_eip" "app" {
  domain   = "vpc"
  instance = aws_instance.app.id

  tags = { Name = var.name }
}
