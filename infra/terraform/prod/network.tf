# One public subnet for the server and two private ones for RDS. An RDS subnet group needs two
# availability zones even for a single-AZ instance. There is no NAT gateway: the private subnets
# have no route out, which is all RDS needs, and the server reaches the internet through its
# Elastic IP.

data "aws_availability_zones" "available" {
  state = "available"
}

locals {
  az_primary   = data.aws_availability_zones.available.names[0]
  az_secondary = data.aws_availability_zones.available.names[1]
}

resource "aws_vpc" "main" {
  cidr_block           = "10.40.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = { Name = var.name }
}

# A new VPC's default security group allows all traffic between its members. Nothing uses it, so
# it is emptied rather than left open.
resource "aws_default_security_group" "default" {
  vpc_id = aws_vpc.main.id
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = { Name = var.name }
}

resource "aws_subnet" "public" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.40.0.0/24"
  availability_zone = local.az_primary

  # Only the Elastic IP gives the server a public address.
  map_public_ip_on_launch = false

  tags = { Name = "${var.name}-public" }
}

resource "aws_subnet" "private_primary" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.40.10.0/24"
  availability_zone = local.az_primary

  tags = { Name = "${var.name}-private-${local.az_primary}" }
}

resource "aws_subnet" "private_secondary" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.40.11.0/24"
  availability_zone = local.az_secondary

  tags = { Name = "${var.name}-private-${local.az_secondary}" }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = { Name = "${var.name}-public" }
}

resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

# Local route only.
resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  tags = { Name = "${var.name}-private" }
}

resource "aws_route_table_association" "private_primary" {
  subnet_id      = aws_subnet.private_primary.id
  route_table_id = aws_route_table.private.id
}

resource "aws_route_table_association" "private_secondary" {
  subnet_id      = aws_subnet.private_secondary.id
  route_table_id = aws_route_table.private.id
}

# The server: 80 and 443 from anywhere, nothing else. No port 22; access is SSM Session Manager.
#
# Rule descriptions accept only letters, digits, spaces and ._-:/()#,@[]+=&;{}!$*. An apostrophe
# ("Let's Encrypt") is rejected by the EC2 API at apply time, not by terraform validate.
resource "aws_security_group" "app" {
  name        = "${var.name}-app"
  description = "PROD server: HTTP and HTTPS in, anything out"
  vpc_id      = aws_vpc.main.id
}

resource "aws_vpc_security_group_ingress_rule" "app_http" {
  security_group_id = aws_security_group.app.id
  description       = "HTTP: Lets Encrypt HTTP-01 challenge and the redirect to HTTPS"
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_vpc_security_group_ingress_rule" "app_https" {
  security_group_id = aws_security_group.app.id
  description       = "HTTPS"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_vpc_security_group_egress_rule" "app_all" {
  security_group_id = aws_security_group.app.id
  description       = "ECR, S3, SES, SSM, Lets Encrypt, Grafana Cloud, Google Maps, OS updates"
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
}

# RDS: PostgreSQL from the server's security group only. No egress rules: RDS starts nothing.
resource "aws_security_group" "db" {
  name        = "${var.name}-db"
  description = "PROD database: PostgreSQL from the app server only"
  vpc_id      = aws_vpc.main.id
}

resource "aws_vpc_security_group_ingress_rule" "db_from_app" {
  security_group_id            = aws_security_group.db.id
  description                  = "PostgreSQL from the app server"
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
  referenced_security_group_id = aws_security_group.app.id
}
