#!/bin/bash
# Runs ON the PROD server, as root, once (and again only if the Caddyfile changes). Installs the same
# Caddyfile NONPROD uses (tools/caddy/Caddyfile) with PROD's hostname and Let's Encrypt contact.
# The hostname's A record must already point at the Elastic IP, or the HTTP-01 challenge fails.
set -euo pipefail
APP_HOSTNAME=accommodation.beezhub.co.za
ACME_EMAIL=boitumelotshehla@gmail.com

install -m 644 /opt/sas/tools/caddy/Caddyfile /etc/caddy/Caddyfile
mkdir -p /etc/systemd/system/caddy.service.d
cat > /etc/systemd/system/caddy.service.d/override.conf <<EOF
[Service]
Environment=APP_HOSTNAME=$APP_HOSTNAME
Environment=ACME_EMAIL=$ACME_EMAIL
EOF
systemctl daemon-reload
# restart, not reload: apt started caddy with the packaged Caddyfile (tools/caddy/Caddyfile header).
systemctl restart caddy
systemctl is-active caddy

for i in $(seq 1 40); do
  if journalctl -u caddy --since "2 minutes ago" --no-pager -o cat | grep -q '"certificate obtained successfully"'; then
    echo "certificate obtained after ~$((i*3))s"; exit 0
  fi
  if journalctl -u caddy --since "10 minutes ago" --no-pager -o cat | grep -q '"certificate already exists\|"loading certificate"'; then
    echo "certificate already in Caddy's store"; exit 0
  fi
  sleep 3
done
echo "no certificate yet:"; journalctl -u caddy --since "2 minutes ago" --no-pager -o cat | grep '"level":"error"' | tail -3
exit 1
