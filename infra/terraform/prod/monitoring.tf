# Health alarms for what Grafana Cloud cannot see from inside the server (#340). Five alarms at
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
