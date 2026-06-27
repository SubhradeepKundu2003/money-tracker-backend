# Deploying Money Tracker to AWS (Free Tier, single EC2)

This guide deploys the backend to **one `t3.micro` EC2 instance** with PostgreSQL
installed on the same box. On a new AWS account this stays within the **12-month
Free Tier**, so your $100 credit is basically untouched for the 6 months you need.

There is **no load balancer** — that's the single biggest cost trap, and you
don't need it for one server.

**What you'll end up with:** your API at `http://<your-elastic-ip>:8080`.

---

## Part 0 — Protect your money FIRST (5 minutes, do not skip)

Before launching anything, set a budget so AWS emails you long before the credit runs out.

1. Sign in to the AWS Console → search **Billing and Cost Management** → **Budgets**.
2. **Create budget** → **Customize (advanced)** → **Cost budget**.
3. Period: **Monthly**. Budgeted amount: **$10**.
4. Add alert thresholds at **50%, 80%, 100%** of actual cost → enter your email.
5. Create. You'll now get an email if spending ever approaches $10/month.

> The setup below should cost ~$0/month on Free Tier. This budget is your safety net.

---

## Part 1 — Launch the EC2 instance

1. Console → **EC2** → make sure the **Region** (top-right) is one near you and
   keep using the *same* region for everything.
2. **Launch instance**.
   - **Name:** `money-tracker`
   - **AMI:** *Amazon Linux 2023* (Free Tier eligible)
   - **Instance type:** `t3.micro` (must say "Free tier eligible")
   - **Key pair:** *Create new key pair* → name `money-tracker` → type **RSA**,
     format **.pem** → download it. **Save this file** (e.g. `~/keys/money-tracker.pem`);
     you can't re-download it.
   - **Network settings → Edit → Security group** (this is your firewall). Add inbound rules:
     | Type        | Port | Source              | Why                          |
     |-------------|------|---------------------|------------------------------|
     | SSH         | 22   | **My IP**           | so you can log in            |
     | Custom TCP  | 8080 | Anywhere (0.0.0.0/0)| so clients can reach the API |
   - **Storage:** 8 GiB (default) is fine and Free Tier covers up to 30 GiB.
3. **Launch instance**.

### Give it a fixed IP (Elastic IP)
So the address survives reboots:
1. EC2 → **Elastic IPs** → **Allocate Elastic IP address** → Allocate.
2. Select it → **Actions → Associate** → choose your `money-tracker` instance → Associate.
3. Note this IP — it's your server address from now on. (Keep it *associated*; an
   unattached Elastic IP is the one thing here that costs a few cents/day.)

---

## Part 2 — Connect and bootstrap the server

On Windows, open **Git Bash** (or PowerShell — both have `ssh`/`scp`).

```bash
# Lock down the key file permissions (Git Bash / macOS / Linux)
chmod 400 ~/keys/money-tracker.pem

# Connect (replace with YOUR Elastic IP)
ssh -i ~/keys/money-tracker.pem ec2-user@<ELASTIC_IP>
```

Upload and run the bootstrap script. From **another** local terminal in the project root:

```bash
scp -i ~/keys/money-tracker.pem \
  deploy/aws/setup-server.sh deploy/aws/money-tracker.service \
  ec2-user@<ELASTIC_IP>:/tmp/
```

Back in the SSH session:

```bash
cd /tmp
chmod +x setup-server.sh
sudo ./setup-server.sh
```

This installs Java 21 + PostgreSQL, creates 2 GB swap, creates the database, and
writes the systemd service. **Copy the database password it prints** at the end
(it's also saved into `/opt/money-tracker/money-tracker.env`).

---

## Part 3 — First deploy

From your **local machine**, project root, build and ship the jar:

```bash
EC2_HOST=<ELASTIC_IP> KEY=~/keys/money-tracker.pem ./deploy/aws/deploy.sh
```

> Windows note: run `deploy.sh` in **Git Bash**. If `./mvnw` isn't executable,
> use `bash deploy/aws/deploy.sh` or build with `mvnw.cmd clean package -DskipTests`
> then `scp` the jar from `target/` manually.

**The very first deploy needs the database tables created.** The `prod` profile
runs with `ddl-auto: validate`, which fails on an empty database. On the server:

```bash
sudo nano /opt/money-tracker/money-tracker.env
#   uncomment:  SPRING_JPA_HIBERNATE_DDL_AUTO=update
#   set APP_CORS_ORIGINS to your frontend URL (or leave * for now)
sudo systemctl restart money-tracker
sudo journalctl -u money-tracker -f      # watch it start; Ctrl-C to stop watching
```

Once it's up and the tables exist, **re-comment** that line and restart again so
production runs in safe `validate` mode:

```bash
sudo nano /opt/money-tracker/money-tracker.env   # re-add the # in front of the ddl-auto line
sudo systemctl restart money-tracker
sudo systemctl enable money-tracker              # start automatically on reboot
```

---

## Part 4 — Verify

```bash
# From your laptop:
curl http://<ELASTIC_IP>:8080/v3/api-docs        # should return JSON
```

Or open `http://<ELASTIC_IP>:8080/swagger-ui.html` in a browser.

---

## Everyday operations

| Task                  | Command (on the server)                          |
|-----------------------|--------------------------------------------------|
| Deploy a new version  | `./deploy/aws/deploy.sh` (from your laptop)      |
| View logs             | `sudo journalctl -u money-tracker -f`            |
| Restart               | `sudo systemctl restart money-tracker`           |
| Status                | `sudo systemctl status money-tracker`            |
| DB backup             | `sudo -u postgres pg_dump moneytracker > ~/backup.sql` |

---

## Cost summary

| Resource                 | Free Tier (new account)     | After Free Tier ends |
|--------------------------|-----------------------------|----------------------|
| EC2 t3.micro (24/7)      | Free (750 hrs/mo, 12 months)| ~$7.50/mo            |
| 8 GB EBS storage         | Free (≤30 GB)               | ~$0.80/mo            |
| Elastic IP (attached)    | Free while attached         | Free while attached  |
| PostgreSQL               | $0 (on the same instance)   | $0                   |
| **Total**                | **~$0/mo**                  | ~$8/mo               |

For your 6-month window on a new account: **effectively free**, $100 credit intact.

---

## If you ever want to stop all charges

- **Pause (keep data):** EC2 → select instance → **Instance state → Stop**.
  Stopped instances don't bill for compute. (A few cents/day for the Elastic IP
  + storage continue.) **Start** it again anytime.
- **Tear everything down:** **Terminate** the instance, then **release** the
  Elastic IP (Elastic IPs → select → Release). That stops every charge.

---

## Optional: a real domain + HTTPS later

When you have a domain, the cheapest path that keeps the no-load-balancer setup:
install **Caddy** on the instance as a reverse proxy in front of port 8080 — it
fetches and renews a free Let's Encrypt certificate automatically. Point an A
record at your Elastic IP, open port 443 in the security group, and set
`APP_CORS_ORIGINS` to your `https://` frontend origin. Ask and I'll write that up.
