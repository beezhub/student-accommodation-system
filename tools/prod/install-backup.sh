#!/bin/bash
# Runs ON the PROD server, as root (tools/prod/ssmrun.sh). Installs backup-db.sh from the deployed
# tree and a systemd timer that runs it daily, then runs it once so the first backup exists now.
set -euo pipefail

install -m 750 /opt/sas/tools/prod/backup-db.sh /usr/local/sbin/sas-backup-db

cat > /etc/systemd/system/sas-backup-db.service <<'EOF'
[Unit]
Description=Daily pg_dump of the PROD database to the backups bucket (#339)
Wants=network-online.target
After=network-online.target

[Service]
Type=oneshot
ExecStart=/usr/local/sbin/sas-backup-db
# Output goes to the journal, which Alloy does not ship; read it with journalctl -u sas-backup-db.
Nice=10
EOF

# 01:30 UTC is 03:30 in South Africa, after RDS's own backup window (00:00 to 00:30 UTC). Persistent
# runs a missed backup at the next boot.
cat > /etc/systemd/system/sas-backup-db.timer <<'EOF'
[Unit]
Description=Run sas-backup-db daily

[Timer]
OnCalendar=*-*-* 01:30:00 UTC
RandomizedDelaySec=10m
Persistent=true

[Install]
WantedBy=timers.target
EOF

systemctl daemon-reload
systemctl enable --now sas-backup-db.timer
systemctl start sas-backup-db.service
journalctl -u sas-backup-db.service --since "5 minutes ago" --no-pager -o cat | grep -E 'action=dbBackup|error|Error' | tail -3
systemctl list-timers sas-backup-db.timer --no-pager | head -2
