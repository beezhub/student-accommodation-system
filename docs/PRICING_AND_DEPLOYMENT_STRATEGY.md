# Student Accommodation System - Pricing & Deployment Strategy

**Last Updated:** January 2026  
**Status:** Production Ready

---

## Table of Contents
1. [Executive Summary](#executive-summary)
2. [Revised Pricing Structure](#revised-pricing-structure)
3. [Deployment Strategy](#deployment-strategy)
4. [Cost Analysis](#cost-analysis)
5. [Migration Path](#migration-path)
6. [Technical Implementation](#technical-implementation)
7. [Scaling Roadmap](#scaling-roadmap)

---

## Executive Summary

### Business Model
- **SaaS subscription** with tiered pricing based on property size
- **One-time onboarding** fees for setup and training
- **Per-bed pricing** for properties exceeding tier limits
- **Target market:** Student accommodation providers in South Africa

### Key Financial Metrics
- **Infrastructure cost:** ~R300–400/mo (Phase 1)
- **Profit margin:** 75–90% across all tiers
- **Break-even:** 1 client (any tier)
- **Year 1 target revenue:** R80,000–R120,000

### Deployment Approach
- **Phase 1 (now):** Single VPS deployment (Hetzner Johannesburg)
- **Phase 2 (growth):** Hybrid (managed database, app on VPS)
- **Phase 3 (scale):** Full cloud migration (AWS/Azure/GCP)

---

## Revised Pricing Structure

### Onboarding Fees (Once-off)

| Tier | Onboarding Fee | What's Included |
|------|----------------|-----------------|
| **Starter** | **R3,500** | • Account setup and configuration<br>• 1.5hr training session (virtual)<br>• Data import (up to 50 students)<br>• Email support for 30 days |
| **Professional** | **R7,500** | • Account setup and configuration<br>• 3hr training session (virtual + on-site option)<br>• Data import (up to 200 students)<br>• Custom document types setup<br>• Integration assistance<br>• Email + phone support for 60 days |
| **Enterprise** | **R15,000+** | • Custom setup and configuration<br>• On-site training (full day)<br>• Full data migration (unlimited students)<br>• Custom integrations (payment, ERP, etc.)<br>• Dedicated support for 90 days<br>• Priority feature requests |

### Monthly Subscriptions

| Tier | Monthly Fee | Beds Included | Extra Beds | Best For |
|------|-------------|---------------|------------|----------|
| **Starter** | **R1,200/mo** | Up to 50 | +R20/bed | Small properties, single locations |
| **Professional** | **R2,800/mo** | Up to 150 | +R16/bed | Medium properties, multiple buildings |
| **Enterprise** | **R6,500/mo** | Up to 400 | +R12/bed | Large properties, multiple locations |

### What Each Tier Includes

#### All Tiers Include:
- ✅ Student application management
- ✅ Room/bed allocation system
- ✅ Document upload and verification
- ✅ Email notifications
- ✅ Admin dashboard
- ✅ Application workflow management
- ✅ Reporting and analytics
- ✅ Secure file storage
- ✅ SSL/TLS encryption
- ✅ Regular backups (daily)
- ✅ 99.5% uptime SLA
- ✅ Email support (business hours)

#### Professional Adds:
- ✅ Custom document types
- ✅ Advanced reporting
- ✅ Phone support
- ✅ Multi-property management
- ✅ Priority email support

#### Enterprise Adds:
- ✅ Custom integrations (API access)
- ✅ Dedicated account manager
- ✅ 24/7 phone support
- ✅ Custom feature development (priority queue)
- ✅ On-premise deployment option (additional cost)
- ✅ 99.9% uptime SLA

---

## First Month Cost (Onboarding + Subscription)

| Tier | Onboarding | First Month Sub | **Total Month 1** | Month 2+ |
|------|-----------|-----------------|-------------------|----------|
| Starter | R3,500 | R1,200 | **R4,700** | R1,200 |
| Professional | R7,500 | R2,800 | **R10,300** | R2,800 |
| Enterprise | R15,000 | R6,500 | **R21,500** | R6,500 |

---

## Pricing Examples

### Example 1: 30-Bed Property (Your Current Client)
- **Tier:** Starter (fits within 50 beds)
- **Monthly subscription:** R1,200
- **Onboarding fee:** R3,500
- **Month 1 total:** R4,700
- **Month 2+ total:** R1,200/mo
- **Year 1 revenue:** R3,500 + (R1,200 × 12) = **R17,900**

### Example 2: 80-Bed Property
- **Tier:** Professional (exceeds Starter's 50 beds)
- **Monthly subscription:** R2,800
- **Onboarding fee:** R7,500
- **Month 1 total:** R10,300
- **Month 2+ total:** R2,800/mo
- **Year 1 revenue:** R7,500 + (R2,800 × 12) = **R41,100**

### Example 3: 180-Bed Property
- **Tier:** Professional (150 + 30 extra beds)
- **Monthly subscription:** R2,800 + (30 × R16) = R3,280
- **Onboarding fee:** R7,500
- **Month 1 total:** R10,780
- **Month 2+ total:** R3,280/mo
- **Year 1 revenue:** R7,500 + (R3,280 × 12) = **R46,860**

### Example 4: 450-Bed Property
- **Tier:** Enterprise (400 + 50 extra beds)
- **Monthly subscription:** R6,500 + (50 × R12) = R7,100
- **Onboarding fee:** R15,000
- **Month 1 total:** R22,100
- **Month 2+ total:** R7,100/mo
- **Year 1 revenue:** R15,000 + (R7,100 × 12) = **R100,200**

---

## Year 1 Revenue Projection (Conservative)

| Quarter | New Clients | Avg Tier | Onboarding Revenue | Subscription Revenue | **Quarterly Total** |
|---------|-------------|----------|-------------------|---------------------|-------------------|
| **Q1** | 1 | Starter | R3,500 | R3,600 (3 months) | **R7,100** |
| **Q2** | 2 | Professional | R15,000 | R11,200 | **R26,200** |
| **Q3** | 3 | Mixed | R18,500 | R25,200 | **R43,700** |
| **Q4** | 3 | Mixed | R18,500 | R42,000 | **R60,500** |

**Year 1 Total Revenue:** **~R137,500**
- Onboarding fees: R55,500
- Recurring subscriptions: R82,000

**Year 2+ Recurring Revenue (9 clients):** **~R180,000–R250,000/year**

---

## Deployment Strategy

### Philosophy
- **Start cheap, scale smart:** Begin with minimal infrastructure, migrate to cloud incrementally
- **Zero vendor lock-in:** Use containerization and S3-compatible APIs
- **Production-ready from day 1:** Proper backups, monitoring, SSL, security
- **Pay for value, not overhead:** Avoid managed services until they save more time/money than they cost

---

## Phase 1: Initial Deployment (Now – 5 clients)

### Infrastructure

#### Compute
- **Provider:** Hetzner Cloud
- **Region:** Johannesburg (CPX21)
- **Specs:** 3 vCPU / 4 GB RAM / 80 GB NVMe SSD
- **Cost:** ~€8.49/mo (≈R170/mo)

#### Object Storage
- **Provider:** Cloudflare R2 (S3-compatible)
- **Why R2:** Zero egress fees, S3 API compatibility
- **Expected usage:** 5–50 GB/mo
- **Cost:** ~$1–5/mo (≈R20–95/mo)

#### Email Service
- **Provider:** AWS SES (Simple Email Service)
- **Expected volume:** 100–1,000 emails/mo
- **Cost:** ~$0.10–1/mo (≈R2–20/mo)

#### DNS & Domain
- **Provider:** Cloudflare (free DNS)
- **Domain:** ~R150–300/year
- **Cost:** ~R15–25/mo (amortized)

#### Backups
- **Database:** Daily `pg_dump` → Cloudflare R2
- **Retention:** 30 days
- **VPS snapshots:** Weekly (Hetzner snapshots)
- **Cost:** ~R40–100/mo

#### SSL/TLS
- **Provider:** Let's Encrypt (via Caddy)
- **Cost:** Free

### Total Monthly Cost (Phase 1)
| Item | Cost (ZAR) |
|------|------------|
| VPS (Hetzner CPX21) | R170 |
| Object Storage (R2) | R20–95 |
| Email (SES) | R2–20 |
| DNS/Domain | R15–25 |
| Backups | R40–100 |
| **Total** | **R247–R410/mo** |

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│ Hetzner VPS (Johannesburg)                              │
│                                                           │
│  ┌─────────────────────────────────────────────────┐    │
│  │ Caddy (Reverse Proxy + Auto SSL)                │    │
│  │ • HTTPS termination                              │    │
│  │ • /api → Backend                                 │    │
│  │ • / → Frontend (static files)                    │    │
│  └─────────────────────────────────────────────────┘    │
│                                                           │
│  ┌─────────────────┐  ┌─────────────────────────────┐   │
│  │ Angular Frontend│  │ Spring Boot Backend         │   │
│  │ (Nginx)         │  │ • REST API                  │   │
│  │ • Static files  │  │ • Business logic            │   │
│  │                 │  │ • Auth (JWT)                │   │
│  └─────────────────┘  └─────────────────────────────┘   │
│                                                           │
│  ┌─────────────────────────────────────────────────┐    │
│  │ PostgreSQL 16                                    │    │
│  │ • Persistent volume: /var/lib/postgresql/data   │    │
│  │ • Daily backups → R2                             │    │
│  └─────────────────────────────────────────────────┘    │
│                                                           │
└─────────────────────────────────────────────────────────┘
                           │
                           │ S3 API calls
                           ▼
              ┌─────────────────────────┐
              │ Cloudflare R2           │
              │ • Student documents     │
              │ • Application files     │
              │ • DB backups            │
              └─────────────────────────┘
                           │
                           │ SMTP
                           ▼
              ┌─────────────────────────┐
              │ AWS SES                 │
              │ • Application emails    │
              │ • Notifications         │
              └─────────────────────────┘
```

### Services (Docker Compose)
1. **Caddy** (reverse proxy, SSL)
2. **Nginx** (serves Angular frontend)
3. **Spring Boot** (backend API)
4. **PostgreSQL 16** (database)

### Security
- ✅ TLS 1.3 (automatic via Caddy)
- ✅ Firewall (UFW): only 80, 443, 22 open
- ✅ SSH key-only authentication
- ✅ Fail2ban (brute force protection)
- ✅ Regular security updates (unattended-upgrades)
- ✅ Database credentials via environment variables
- ✅ JWT secret rotation capability

### Monitoring
- ✅ UptimeRobot (free tier) for uptime monitoring
- ✅ Email alerts for downtime
- ✅ Docker health checks
- ✅ Log rotation (logrotate)

---

## Phase 2: Growth Mode (5–20 clients)

### When to Migrate
- ✅ Revenue exceeds R25,000/mo (3+ months consecutively)
- ✅ VPS CPU/RAM consistently >70% utilization
- ✅ Database size >50 GB
- ✅ Support tickets related to performance >2/month

### Changes
1. **Move database to managed Postgres**
   - AWS RDS / Azure Database for PostgreSQL / GCP Cloud SQL
   - Automated backups, point-in-time recovery
   - Read replicas for reporting (if needed)
   - Cost: +R500–1,200/mo

2. **Keep application on VPS** (still cost-effective)
   - Upgrade to CPX31 (4 vCPU / 8 GB RAM) if needed
   - Cost: +R100–150/mo

3. **Add CDN** (optional, for faster frontend delivery)
   - Cloudflare CDN (free/cheap)
   - Serve static files from S3 + CloudFront/Azure CDN
   - Cost: R50–300/mo

### Total Monthly Cost (Phase 2)
- **R1,200–R2,500/mo**
- Supports 5–20 clients comfortably

---

## Phase 3: Scale Mode (20+ clients)

### When to Migrate
- ✅ Revenue exceeds R80,000/mo
- ✅ >20 active clients
- ✅ Need multi-region deployment
- ✅ Compliance requires specific cloud provider

### Full Cloud Migration

#### Option A: AWS (Cape Town)
- **Compute:** ECS Fargate or EC2 Auto Scaling Group
- **Database:** RDS PostgreSQL Multi-AZ
- **Storage:** S3 (native)
- **CDN:** CloudFront
- **Email:** SES (already using)
- **Load Balancer:** Application Load Balancer
- **Estimated cost:** R5,000–R15,000/mo

#### Option B: Azure (South Africa)
- **Compute:** Azure Container Instances or App Service
- **Database:** Azure Database for PostgreSQL (Flexible Server)
- **Storage:** Azure Blob Storage
- **CDN:** Azure CDN
- **Email:** SendGrid or SES
- **Load Balancer:** Azure Application Gateway
- **Estimated cost:** R4,500–R14,000/mo

#### Option C: GCP (Europe – no Africa region)
- **Compute:** Cloud Run or GKE
- **Database:** Cloud SQL for PostgreSQL
- **Storage:** Google Cloud Storage
- **CDN:** Cloud CDN
- **Email:** SendGrid or SES
- **Load Balancer:** Cloud Load Balancing
- **Estimated cost:** R4,000–R13,000/mo

### Features Unlocked at Scale
- ✅ Auto-scaling (handle traffic spikes)
- ✅ Multi-region deployment (disaster recovery)
- ✅ 99.99% uptime SLA
- ✅ Advanced monitoring (CloudWatch, Azure Monitor, Stackdriver)
- ✅ CI/CD pipelines (automated deployments)
- ✅ Infrastructure as Code (Terraform/Pulumi)

---

## Cost Analysis

### Profit Margin Analysis (Phase 1 Costs: R350/mo)

| Tier | Monthly Fee | Infrastructure Cost | **Net Profit** | **Margin** |
|------|-------------|-------------------|----------------|------------|
| Starter | R1,200 | R350 | R850 | 71% |
| Professional | R2,800 | R350 | R2,450 | 88% |
| Enterprise | R6,500 | R350 | R6,150 | 95% |

### Break-Even Analysis
- **Fixed costs:** R350/mo (infrastructure)
- **Variable costs:** ~10–20 hours support/client/year (your time)
- **Break-even:** 1 Starter client covers all infrastructure costs with profit

### Scaling Economics
As you add clients:
- **Infrastructure costs grow sub-linearly** (1 VPS can handle 5–10 clients)
- **Margins improve** until you need to scale infrastructure
- **Sweet spot:** 5–15 clients on Phase 1 infrastructure (75–85% margins)

---

## Migration Path (Zero Tech Debt)

### Why This Deployment Avoids Lock-In

#### 1. Containerization
- All services in Docker containers
- `docker-compose.yml` → Kubernetes YAML (simple conversion)
- Same containers run on Hetzner, AWS ECS, Azure ACI, GCP Cloud Run

#### 2. S3-Compatible Storage
- MinIO SDK supports AWS S3, Azure Blob (via S3 gateway), GCS, R2, Backblaze B2
- **Current:** Cloudflare R2
- **Future:** Swap endpoint/credentials only (zero code changes)

```yaml
# Development (docker-compose.yml)
MINIO_ENDPOINT: http://minio:9000

# Production Phase 1 (Hetzner + R2)
MINIO_ENDPOINT: https://your-account.r2.cloudflarestorage.com

# Production Phase 3 (AWS S3)
MINIO_ENDPOINT: https://s3.af-south-1.amazonaws.com
```

#### 3. Managed Email (Provider-Agnostic)
- AWS SES works from any server (just SMTP credentials)
- Can switch to SendGrid, Mailgun, etc. with config change only

#### 4. Database Migrations (Flyway)
- Schema versioned in code (`src/main/resources/db/migration`)
- Works with local Postgres, RDS, Azure Database, Cloud SQL
- Migration path: dump → restore → run Flyway

#### 5. Infrastructure as Code (Future)
- Current: Manual provisioning + documented scripts
- Phase 2+: Terraform/Pulumi for reproducible deployments

---

## Technical Implementation

### Production Docker Compose

See `deployment/docker-compose.prod.yml` for the production configuration.

Key differences from development:
- No MinIO container (uses external R2)
- Production database credentials
- SSL termination via Caddy
- Health checks for all services
- Resource limits
- Restart policies

### Environment Variables (Production)

```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/studentaccommodation_prod
USER_DB=saas_app_user
PASSWORD_DB=<strong-password-here>

# JWT
JWT_SECRET=<generate-strong-secret-256bit>
JWT_EXPIRATION=3600000
JWT_ADMIN_EXPIRATION=14400000

# S3-Compatible Storage (Cloudflare R2)
MINIO_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com
MINIO_ACCESS_KEY=<r2-access-key>
MINIO_SECRET_KEY=<r2-secret-key>
MINIO_BUCKET=student-accommodation-prod

# Email (AWS SES)
MAIL_HOST=email-smtp.af-south-1.amazonaws.com
MAIL_PORT=587
MAIL_USERNAME=<ses-smtp-username>
MAIL_PASSWORD=<ses-smtp-password>

# Application
APP_BASE_URL=https://yourdomain.com
APP_SUPPORT_EMAIL=support@yourdomain.com
APP_UNIVERSITY_NAME=YourCompanyName

# File Upload Limits
MAX_FILE_SIZE=10MB
MAX_REQUEST_SIZE=20MB
```

### Deployment Checklist

#### Pre-Deployment
- [ ] Register domain
- [ ] Provision Hetzner VPS (Johannesburg)
- [ ] Create Cloudflare R2 bucket
- [ ] Verify AWS SES domain
- [ ] Generate strong JWT secret (`openssl rand -base64 64`)
- [ ] Create production environment file

#### Initial Deployment
- [ ] SSH into VPS
- [ ] Install Docker & Docker Compose
- [ ] Clone repository
- [ ] Set up environment variables
- [ ] Run `docker-compose -f docker-compose.prod.yml up -d`
- [ ] Verify services are running
- [ ] Point domain DNS to VPS IP

#### Post-Deployment
- [ ] Test SSL certificate (https://yourdomain.com)
- [ ] Create admin user
- [ ] Test file upload → R2
- [ ] Test email sending
- [ ] Set up monitoring (UptimeRobot)
- [ ] Configure backups (cron job for `pg_dump`)
- [ ] Document credentials in password manager

#### Security Hardening
- [ ] Disable password SSH (keys only)
- [ ] Configure UFW firewall
- [ ] Install Fail2ban
- [ ] Enable unattended-upgrades
- [ ] Set up log rotation
- [ ] Review database user permissions

---

## Scaling Roadmap

### Metrics to Monitor

| Metric | Phase 1 Limit | Action Threshold |
|--------|---------------|------------------|
| **Monthly Revenue** | R0–R25,000 | >R25,000 → Consider Phase 2 |
| **Active Clients** | 1–10 | >10 → Plan Phase 2 migration |
| **CPU Usage (avg)** | <70% | >70% → Upgrade VPS or migrate |
| **RAM Usage (avg)** | <75% | >75% → Upgrade VPS |
| **DB Size** | <50 GB | >50 GB → Managed Postgres |
| **Storage (R2)** | <100 GB | >100 GB → Review pricing |
| **Emails/mo** | <5,000 | >5,000 → Review SES pricing |

### Decision Tree

```
Revenue < R25,000/mo?
├─ YES → Stay on Phase 1 (Hetzner + R2)
└─ NO → Evaluate migration
    ├─ VPS maxed out? → Upgrade to CPX31 or Phase 2
    ├─ Need compliance? → Phase 3 (specific cloud)
    └─ Want managed DB? → Phase 2 (hybrid)

Revenue > R80,000/mo?
└─ YES → Plan Phase 3 migration (full cloud)
    ├─ Multi-region needed? → AWS/Azure/GCP
    ├─ Budget priority? → GCP or Hetzner + DigitalOcean Managed DB
    └─ SA-first? → AWS Cape Town or Azure South Africa
```

---

## Risk Mitigation

### Single VPS Risk (Phase 1)
**Risk:** VPS failure = downtime  
**Mitigation:**
- Daily offsite backups (R2)
- Hetzner weekly snapshots
- Recovery time: <2 hours (restore snapshot or rebuild)
- Acceptable for early stage (cost vs. risk trade-off)

### Data Loss Risk
**Risk:** Database corruption/deletion  
**Mitigation:**
- Daily automated backups (30-day retention)
- Point-in-time recovery (when on managed DB)
- Test restore monthly

### Security Risk
**Risk:** Breach, data theft  
**Mitigation:**
- SSL/TLS everywhere
- Fail2ban + firewall
- Regular security updates
- Environment variables (no hardcoded secrets)
- JWT with expiration
- Input validation (backend)

### Scaling Risk
**Risk:** Can't handle growth  
**Mitigation:**
- Phased migration path (proven tech stack)
- Containers enable easy scaling
- Monitoring alerts before hitting limits

---

## Support & Maintenance

### Client Support Time Estimate
- **Starter clients:** 5–10 hours/year
- **Professional clients:** 10–20 hours/year
- **Enterprise clients:** 20–40 hours/year

### Infrastructure Maintenance
- **Monthly tasks:** Review logs, check backups, security updates
- **Time required:** 2–4 hours/month
- **Automation opportunity:** CI/CD, monitoring alerts (Phase 2+)

### Feature Development
- **Client requests:** Track in backlog
- **Enterprise priority:** Included in pricing
- **Standard features:** Quarterly release cycle

---

## Financial Projections

### Conservative Scenario (Year 1)
- **Q1:** 1 Starter client → R7,100
- **Q2:** +2 clients (mixed) → R26,200
- **Q3:** +3 clients (mixed) → R43,700
- **Q4:** +3 clients (mixed) → R60,500
- **Year 1 Total:** R137,500
- **Infrastructure costs:** R4,200
- **Net profit (before tax/time):** R133,300

### Optimistic Scenario (Year 1)
- 12 clients by year-end (mix of tiers)
- Year 1 revenue: R180,000–R250,000
- Infrastructure costs: R5,000–R8,000
- Net profit: R172,000–R242,000

### Year 2+ (Recurring Revenue)
- Assume 85% retention
- Add 1–2 new clients/month
- Year 2 revenue: R300,000–R500,000
- Phase 2 infrastructure costs: R20,000–R30,000/year
- Net profit: R270,000–R470,000

---

## Next Steps

### Immediate Actions (Week 1)
1. [ ] Register business domain
2. [ ] Create Hetzner account + provision VPS
3. [ ] Create Cloudflare account + R2 bucket
4. [ ] Set up AWS account + verify SES domain
5. [ ] Generate production credentials
6. [ ] Deploy to production
7. [ ] Test end-to-end (signup → application → document upload)

### Short Term (Month 1)
1. [ ] Onboard first client
2. [ ] Set up monitoring and alerts
3. [ ] Document operational procedures
4. [ ] Create client onboarding checklist
5. [ ] Build simple marketing site (landing page)

### Medium Term (Quarter 1)
1. [ ] Acquire 2–3 clients
2. [ ] Gather feedback and iterate
3. [ ] Build feature backlog
4. [ ] Plan Phase 2 migration triggers
5. [ ] Consider partnerships (property managers, universities)

---

## Appendix

### Competitor Pricing (South Africa)
*(Research and update with actual competitor data)*

### Technology Stack
- **Frontend:** Angular 21, Angular Material, TypeScript
- **Backend:** Spring Boot 3.5, Java 21
- **Database:** PostgreSQL 16
- **Storage:** S3-compatible (MinIO SDK)
- **Email:** SMTP (AWS SES)
- **Containerization:** Docker, Docker Compose
- **Reverse Proxy:** Caddy or Nginx
- **Version Control:** Git

### Useful Commands

#### Deploy/Update
```bash
# Pull latest code
git pull origin main

# Rebuild and restart services
docker-compose -f docker-compose.prod.yml up -d --build

# View logs
docker-compose -f docker-compose.prod.yml logs -f
```

#### Backup Database
```bash
# Manual backup
docker exec studentaccommodation_postgres \
  pg_dump -U saas_app_user studentaccommodation_prod | \
  gzip > backup_$(date +%Y%m%d).sql.gz

# Upload to R2 (using AWS CLI with R2 endpoint)
aws s3 cp backup_$(date +%Y%m%d).sql.gz \
  s3://student-accommodation-prod/backups/
```

#### Monitor Resources
```bash
# CPU/RAM usage
docker stats

# Disk usage
df -h

# Database size
docker exec studentaccommodation_postgres \
  psql -U saas_app_user -d studentaccommodation_prod \
  -c "SELECT pg_size_pretty(pg_database_size('studentaccommodation_prod'));"
```

---

## Conclusion

This pricing and deployment strategy provides:
- ✅ **Profitable from day 1** (71–95% margins)
- ✅ **Low initial investment** (R300–400/mo infrastructure)
- ✅ **Scalable path** (clear migration phases)
- ✅ **Zero vendor lock-in** (containers + S3-compatible APIs)
- ✅ **Production-ready** (backups, monitoring, security)

**Key Insight:** Start lean, scale incrementally, and migrate to expensive cloud providers only when revenue justifies it. The containerized architecture ensures you can move to AWS/Azure/GCP with minimal code changes when the time is right.

**Recommended Action:** Deploy Phase 1 now, acquire 1–3 clients, validate pricing and demand, then reassess infrastructure needs quarterly.

---

**Document Owner:** Development Team  
**Review Cycle:** Quarterly (or when revenue doubles)  
**Last Review:** January 2026  
**Next Review:** April 2026
