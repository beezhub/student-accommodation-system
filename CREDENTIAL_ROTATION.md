# Credential Rotation Notice

**Date:** 2026-02-12
**Task:** TASK-1.01 - .env Credential Cleanup
**Priority:** CRITICAL

## Summary

This document tracks credentials that were exposed in the repository and require rotation.

## Exposed Credentials

The following credentials were found hardcoded in `docker-compose.yml` and potentially in git history:

### 1. JWT Secret
- **Location:** `docker-compose.yml` (line 34)
- **Exposed Value:** `jvc+QC/0myyJ9+//Rzo+qYxfMmHDiQdlysvcT9XhUF7/6mvHD9evZBB/ngNAMwTD6sDDQgf4vS0QDZ8JYLoBOA==`
- **Risk Level:** CRITICAL
- **Action Required:** Generate new 512-bit base64 JWT secret and update in all environments
- **Status:** ⏳ Pending rotation

### 2. Database Credentials
- **Location:** `docker-compose.yml` (lines 8-10, 30-32)
- **Exposed Values:**
  - Username: `localadmin`
  - Password: `admin`
- **Risk Level:** HIGH
- **Action Required:** Change database password in all non-local environments
- **Status:** ⏳ Pending rotation (production/staging only)
- **Note:** These are acceptable for local development but must be different in production

### 3. MinIO Credentials
- **Location:** `docker-compose.yml` (lines 37-38, 55-56)
- **Exposed Values:**
  - Access Key: `minioadmin`
  - Secret Key: `minioadmin`
- **Risk Level:** HIGH
- **Action Required:** Generate new MinIO access and secret keys for all environments
- **Status:** ⏳ Pending rotation
- **Note:** Default credentials must never be used in production

### 4. SMTP Credentials
- **Location:** `docker-compose.yml` (lines 42-43)
- **Exposed Values:**
  - Username: `student@gmail.com`
  - Password: `test`
- **Risk Level:** LOW (MailHog test credentials)
- **Action Required:** Ensure production SMTP credentials are not hardcoded
- **Status:** ✅ Not applicable (test credentials only)

### 5. Google Maps API Keys
- **Location:** `backend/.env.example`, potentially in git history if `.env` was previously committed
- **Risk Level:** HIGH (if real keys were committed)
- **Action Required:**
  - Check git history for exposed API keys
  - Rotate any exposed Google Maps API keys via Google Cloud Console
  - Add HTTP referrer and IP restrictions to new keys
- **Status:** ⏳ Requires investigation

## Rotation Actions Required

### Immediate Actions (Before Production Deployment)

1. **Generate New JWT Secret**
   ```bash
   # Linux/Mac
   openssl rand -base64 64

   # Windows PowerShell
   [Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Min 0 -Max 256 }))
   ```

2. **Rotate MinIO Credentials**
   - Generate new 20+ character access key
   - Generate new 40+ character secret key
   - Update in MinIO console and application configuration

3. **Set Strong Database Passwords**
   - Production: Minimum 16 characters, alphanumeric + special chars
   - Staging: Minimum 12 characters, different from production
   - Local dev: Keep simple (current values acceptable)

4. **Verify Google Maps API Keys**
   ```bash
   # Check git history for exposed API keys
   git log --all --full-history --source -- "*.env"
   git log -p --all --full-history --source -- "*GOOGLE*"
   ```
   - If keys found in history: Rotate via Google Cloud Console
   - Add restrictions: HTTP referrer for frontend key, IP restriction for backend key

5. **Configure Production SMTP**
   - Use production SMTP provider (e.g., SendGrid, AWS SES, Mailgun)
   - Never use MailHog in production
   - Store credentials in environment variables only

### Environment-Specific Credential Management

| Environment | Credential Storage Method |
|------------|---------------------------|
| **Local Dev** | `.env` file (gitignored) |
| **CI/CD** | GitHub Secrets / CI environment variables |
| **Staging** | Cloud secret manager (AWS Secrets Manager, Azure Key Vault, etc.) |
| **Production** | Cloud secret manager with encryption at rest |

## Verification Checklist

- [ ] `.env` added to `.gitignore` (✅ Complete)
- [ ] `.env.example` created with placeholders only (✅ Complete)
- [ ] README.md updated with setup instructions (✅ Complete)
- [ ] Git history checked for exposed secrets
- [ ] JWT secret rotated in production
- [ ] MinIO credentials rotated in staging/production
- [ ] Database passwords updated in staging/production
- [ ] Google Maps API keys verified/rotated if exposed
- [ ] Production SMTP credentials configured securely
- [ ] All team members notified of credential rotation
- [ ] Old credentials deactivated/revoked

## Prevention Measures

The following measures have been implemented to prevent future credential exposure:

1. **`.gitignore` Configuration**
   - `.env`, `.env.local`, `.env.*.local`, `*.env`, `**/.env`, `**/env` patterns added
   - Prevents accidental commits of environment files

2. **`.env.example` Template**
   - Provides structure without exposing real credentials
   - Includes instructions for secure key generation
   - Documents all required environment variables

3. **Documentation**
   - README.md includes clear security warnings
   - Setup instructions emphasize credential security
   - Links to credential generation tools provided

## Next Steps

1. Complete this task (TASK-1.01) and merge to develop
2. Schedule credential rotation before next production deployment
3. Implement TASK-1.04 (Redis) and TASK-1.05 (JWT Token Blacklist) for additional security
4. Consider implementing secrets scanning in CI/CD pipeline (Phase 3 enhancement)

## References

- **Task:** TASK-1.01 in `docs/task-list.md`
- **Architecture:** [ARCH 3.2.8] .env Credential Cleanup
- **Risk:** RISK-003 (Credential exposure from committed .env file)
- **Requirements:** [REQ AUTH-003] (JWT secret configuration), [REQ BR-030] (token expiry config)

---

**Last Updated:** 2026-02-12
**Owner:** Development Team
**Status:** In Progress - TASK-1.01
