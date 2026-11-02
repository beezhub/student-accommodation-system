#!/bin/bash
# First-boot setup for the PROD server. Runs once, as root, through cloud-init. It prepares the host;
# it does not deploy the application, which is the first deploy's job (#336). Output is in
# /var/log/cloud-init-output.log.
set -euxo pipefail

export DEBIAN_FRONTEND=noninteractive

# 2 GB of swap behind 2 GB of RAM (docs/PROD_ENVIRONMENT_PLAN.md). Low swappiness: swap is a
# cushion for deadline-day peaks, not working memory.
if [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi
echo 'vm.swappiness=10' > /etc/sysctl.d/90-swappiness.conf
sysctl --system

apt-get update
apt-get install -y ca-certificates curl gnupg unzip unattended-upgrades postgresql-client

# Docker Engine and the compose plugin from Docker's own repository; Ubuntu's docker.io package
# lags behind and has no compose v2 plugin.
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  > /etc/apt/sources.list.d/docker.list

# Caddy terminates TLS on the host, as on NONPROD (tools/caddy/Caddyfile). Configured in #336.
curl -fsSL https://dl.cloudsmith.io/public/caddy/stable/gpg.key \
  | gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -fsSL https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt \
  > /etc/apt/sources.list.d/caddy-stable.list

apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin caddy

# AWS CLI v2, for ECR login and for reading the deploy's settings from SSM and Secrets Manager.
if ! command -v aws >/dev/null; then
  curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-$(uname -m).zip" -o /tmp/awscliv2.zip
  unzip -q /tmp/awscliv2.zip -d /tmp
  /tmp/aws/install
  rm -rf /tmp/aws /tmp/awscliv2.zip
fi

usermod -aG docker ubuntu
install -d -o ubuntu -g ubuntu /opt/sas

# Security updates install themselves; the server is never rebooted without a person deciding to.
cat > /etc/apt/apt.conf.d/20auto-upgrades <<'EOF'
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";
EOF
