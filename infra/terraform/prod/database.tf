resource "aws_db_subnet_group" "main" {
  name       = var.name
  subnet_ids = [aws_subnet.private_primary.id, aws_subnet.private_secondary.id]
}

# RDS for PostgreSQL 16 already defaults rds.force_ssl to 1. Set here so the requirement is written
# down: the backend's DB_URL carries sslmode=require (docker-compose.aws.yml).
resource "aws_db_parameter_group" "postgres16" {
  name   = "${var.name}-postgres16"
  family = "postgres16"

  # AWS records this parameter as pending-reboot. Leaving Terraform's default of immediate gives a
  # diff on every plan, though the value is 1 either way.
  parameter {
    name         = "rds.force_ssl"
    value        = "1"
    apply_method = "pending-reboot"
  }
}

resource "aws_db_instance" "main" {
  identifier     = var.name
  engine         = "postgres"
  engine_version = "16"
  instance_class = var.db_instance_class

  db_name  = "studentaccommodation"
  username = "sas_admin"

  # RDS generates the password and keeps it in Secrets Manager, so it is never in Terraform state.
  manage_master_user_password = true

  allocated_storage     = 20
  max_allocated_storage = 40
  storage_type          = "gp3"
  storage_encrypted     = true

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.db.id]
  parameter_group_name   = aws_db_parameter_group.postgres16.name
  publicly_accessible    = false
  multi_az               = false

  # Same zone as the server: traffic between zones is billed.
  availability_zone = local.az_primary

  # Automated backups with point-in-time recovery inside the window (#339). Times are UTC:
  # backups 02:00-02:30 and maintenance Sunday 03:00-03:30 South African time.
  backup_retention_period = 7
  backup_window           = "00:00-00:30"
  maintenance_window      = "sun:01:00-sun:01:30"
  copy_tags_to_snapshot   = true

  auto_minor_version_upgrade = true
  apply_immediately          = false

  deletion_protection       = true
  skip_final_snapshot       = false
  final_snapshot_identifier = "${var.name}-final"
}
