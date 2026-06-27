#!/usr/bin/env bash
#
# Build the jar locally and ship it to the EC2 instance, then restart the
# service. Run from the project root (the folder with pom.xml).
#
#   EC2_HOST=1.2.3.4 KEY=~/keys/money-tracker.pem ./deploy/aws/deploy.sh
#
# Requires: maven (or use ./mvnw), ssh, scp. On Windows run this in Git Bash.
set -euo pipefail

EC2_HOST="${EC2_HOST:?Set EC2_HOST to your instance's Elastic IP}"
KEY="${KEY:?Set KEY to the path of your .pem key file}"
EC2_USER="${EC2_USER:-ec2-user}"
REMOTE_DIR="/opt/money-tracker"

echo "==> Building jar (skipping tests for speed; drop -DskipTests to run them)"
./mvnw -q clean package -DskipTests

JAR="$(ls target/money-tracker-backend-*.jar | grep -v plain | head -n1)"
echo "==> Built ${JAR}"

echo "==> Uploading to ${EC2_USER}@${EC2_HOST}:/tmp/app.jar"
scp -i "${KEY}" "${JAR}" "${EC2_USER}@${EC2_HOST}:/tmp/app.jar"

echo "==> Installing and restarting service"
ssh -i "${KEY}" "${EC2_USER}@${EC2_HOST}" "sudo install -o moneytracker -g moneytracker -m 644 /tmp/app.jar ${REMOTE_DIR}/app.jar && sudo systemctl restart money-tracker && sleep 3 && sudo systemctl --no-pager status money-tracker | head -n 12"

echo "==> Done. Tail logs with:"
echo "    ssh -i ${KEY} ${EC2_USER}@${EC2_HOST} 'sudo journalctl -u money-tracker -f'"
