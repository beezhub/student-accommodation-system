# Health alarms for what Grafana Cloud cannot see from inside the server (#340). Six alarms at
# $0.10 each a month, plus the backup alarm and its custom metric.

resource "aws_sns_topic" "alarms" {
  name = "${var.name}-alarms"
}

# AWS emails a confirmation link; nothing is delivered until it is clicked.
resource "aws_sns_topic_subscription" "alarms_email" {
  topic_arn = aws_sns_topic.alarms.arn
  protocol  = "email"
  endpoint  = var.alarm_email
}

# A failed system check means AWS's host is at fault. The recover action moves the instance to
# healthy hardware, keeping its ID, Elastic IP and disk.
resource "aws_cloudwatch_metric_alarm" "ec2_system_check" {
  alarm_name          = "${var.name}-ec2-system-check"
  alarm_description   = "AWS hardware or network fault under the PROD server; recovering automatically"
  namespace           = "AWS/EC2"
  metric_name         = "StatusCheckFailed_System"
  dimensions          = { InstanceId = aws_instance.app.id }
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 2
  threshold           = 0
  comparison_operator = "GreaterThanThreshold"
  alarm_actions       = ["arn:aws:automate:${var.region}:ec2:recover", aws_sns_topic.alarms.arn]
  ok_actions          = [aws_sns_topic.alarms.arn]
}

# A failed instance check is inside the server (kernel, disk full, memory): a person looks.
resource "aws_cloudwatch_metric_alarm" "ec2_instance_check" {
  alarm_name          = "${var.name}-ec2-instance-check"
  alarm_description   = "The PROD server's own operating system is failing its status check"
  namespace           = "AWS/EC2"
  metric_name         = "StatusCheckFailed_Instance"
  dimensions          = { InstanceId = aws_instance.app.id }
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 3
  threshold           = 0
  comparison_operator = "GreaterThanThreshold"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  ok_actions          = [aws_sns_topic.alarms.arn]
}

resource "aws_cloudwatch_metric_alarm" "rds_cpu" {
  alarm_name          = "${var.name}-rds-cpu"
  alarm_description   = "PROD database CPU above 80% for 15 minutes"
  namespace           = "AWS/RDS"
  metric_name         = "CPUUtilization"
  dimensions          = { DBInstanceIdentifier = aws_db_instance.main.identifier }
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 3
  threshold           = 80
  comparison_operator = "GreaterThanThreshold"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  ok_actions          = [aws_sns_topic.alarms.arn]
}

resource "aws_cloudwatch_metric_alarm" "rds_storage" {
  alarm_name          = "${var.name}-rds-free-storage"
  alarm_description   = "PROD database has under 4 GB of free storage"
  namespace           = "AWS/RDS"
  metric_name         = "FreeStorageSpace"
  dimensions          = { DBInstanceIdentifier = aws_db_instance.main.identifier }
  statistic           = "Minimum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 4 * 1024 * 1024 * 1024
  comparison_operator = "LessThanThreshold"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  ok_actions          = [aws_sns_topic.alarms.arn]
}

# db.t4g.micro has 1 GB. Below 100 MB free it is swapping, and a bigger class is due (#334).
resource "aws_cloudwatch_metric_alarm" "rds_memory" {
  alarm_name          = "${var.name}-rds-free-memory"
  alarm_description   = "PROD database has under 100 MB of free memory for 15 minutes"
  namespace           = "AWS/RDS"
  metric_name         = "FreeableMemory"
  dimensions          = { DBInstanceIdentifier = aws_db_instance.main.identifier }
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 3
  threshold           = 100 * 1024 * 1024
  comparison_operator = "LessThanThreshold"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  ok_actions          = [aws_sns_topic.alarms.arn]
}

# The daily pg_dump (tools/prod/backup-db.sh) puts BackupSucceeded=1 after each upload. No datapoint
# for two days in a row means the backup has stopped, whatever the reason: the timer, the dump, the
# upload or the server itself. Missing data counts as breaching for exactly that reason.
resource "aws_cloudwatch_metric_alarm" "db_backup_missing" {
  alarm_name          = "${var.name}-db-backup-missing"
  alarm_description   = "No successful daily database dump to the backups bucket for 2 days (#339)"
  namespace           = "SAS/Backups"
  metric_name         = "BackupSucceeded"
  dimensions          = { Database = var.name }
  statistic           = "Sum"
  period              = 86400
  evaluation_periods  = 2
  datapoints_to_alarm = 2
  threshold           = 1
  comparison_operator = "LessThanThreshold"
  treat_missing_data  = "breaching"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  ok_actions          = [aws_sns_topic.alarms.arn]
}

# t4g.small in standard mode earns 24 CPU credits an hour and holds at most 576. At zero it is held
# to its 20% baseline, which looks like a slow site rather than an outage. 100 credits is about an
# hour of both vCPUs flat out, so there is time to act: find the cause, or switch to unlimited mode.
resource "aws_cloudwatch_metric_alarm" "ec2_cpu_credits" {
  alarm_name          = "${var.name}-ec2-cpu-credits"
  alarm_description   = "PROD server has under 100 CPU credits left; at zero it slows to 20% CPU (#340)"
  namespace           = "AWS/EC2"
  metric_name         = "CPUCreditBalance"
  dimensions          = { InstanceId = aws_instance.app.id }
  statistic           = "Minimum"
  period              = 300
  evaluation_periods  = 2
  threshold           = 100
  comparison_operator = "LessThanThreshold"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  ok_actions          = [aws_sns_topic.alarms.arn]
}

# Data out to the internet: the first 100 GB a month is free across the whole account, then it is
# $0.154/GB (docs/PROD_ENVIRONMENT_PLAN.md). A CloudWatch alarm cannot see it: documents download
# straight from S3 through presigned URLs, which never pass the server's NetworkOut. Cost
# Explorer's usage groups count both paths. Not filtered to PROD, because the free 100 GB is the
# account's, whatever uses it. Budgets that only notify are free.
resource "aws_budgets_budget" "data_out" {
  name         = "${var.name}-data-out"
  budget_type  = "USAGE"
  limit_amount = "100"
  limit_unit   = "GB"
  time_unit    = "MONTHLY"

  cost_filter {
    name   = "UsageTypeGroup"
    values = ["EC2: Data Transfer - Internet (Out)", "S3: Data Transfer - Internet (Out)"]
  }

  # 80 GB used: time to look before the free allowance runs out.
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 80
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.alarm_email]
  }

  # On course to pass 100 GB this month, which is when it starts to cost money.
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
    notification_type          = "FORECASTED"
    subscriber_email_addresses = [var.alarm_email]
  }
}
