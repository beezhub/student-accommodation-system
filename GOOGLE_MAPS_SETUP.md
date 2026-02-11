# Google Maps API Setup Guide

This guide will help you safely configure Google Maps API keys for the Student Accommodation System.

## 🔐 Security First

**NEVER commit API keys to the repository!**

The `.gitignore` file is already configured to exclude:
- `.env`
- `.env.local`
- `*.env`

## 📋 Prerequisites

- Google Cloud Platform account
- Project created in Google Cloud Console

---

## Part 1: Create Google Cloud API Keys

### Step 1: Access Google Cloud Console

1. Go to https://console.cloud.google.com/
2. Create a new project or select your existing project
3. Navigate to **APIs & Services** → **Library**

### Step 2: Enable Required APIs

Search for and enable these APIs:
- ✅ **Geocoding API** (for backend geocoding)
- ✅ **Places API** (for frontend autocomplete)
- ✅ **Maps JavaScript API** (for frontend map display)

### Step 3: Create Backend API Key

1. Go to **APIs & Services** → **Credentials**
2. Click **+ CREATE CREDENTIALS** → **API key**
3. A key will be created - click **RESTRICT KEY**
4. Configure:
   - **Name**: `Student Accommodation - Backend Server`
   - **Application restrictions**:
     - Select "IP addresses"
     - For development: Add `127.0.0.1` (or leave unrestricted temporarily)
     - For production: Add your server's public IP addresses
   - **API restrictions**:
     - Select "Restrict key"
     - Select only: ✅ **Geocoding API**
5. Click **SAVE**
6. **Copy the API key** - you'll need it for backend configuration

### Step 4: Create Frontend API Key

1. Click **+ CREATE CREDENTIALS** → **API key** again
2. Click **RESTRICT KEY**
3. Configure:
   - **Name**: `Student Accommodation - Frontend Client`
   - **Application restrictions**:
     - Select "HTTP referrers (websites)"
     - Add these referrers:
       - `http://localhost:4200/*` (for local development)
       - `http://localhost:*` (backup for different ports)
       - `https://yourdomain.com/*` (add your production domain when ready)
   - **API restrictions**:
     - Select "Restrict key"
     - Select: ✅ **Places API** and ✅ **Maps JavaScript API**
4. Click **SAVE**
5. **Copy the API key** - you'll need it for frontend configuration

---

## Part 2: Local Development Setup

### Option A: Using `.env` File (Recommended) ⭐

#### 1. Create `.env` file

In the `backend/` directory, create a `.env` file:

```bash
cd backend
cp .env.example .env
```

#### 2. Edit `.env` file

Open `backend/.env` and add your API keys:

```bash
# Replace with your actual API keys from Google Cloud Console
GOOGLE_MAPS_API_KEY=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
GOOGLE_MAPS_FRONTEND_API_KEY=AIzaSyYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY
GEOCODING_ENABLED=true
```

#### 3. Configure Spring Boot to use `.env`

Add this dependency to `backend/build.gradle` if not already present:

```gradle
implementation 'me.paulschwarz:spring-dotenv:4.0.0'
```

Or use this alternative approach - **create a run configuration** in your IDE:

**IntelliJ IDEA:**
1. Run → Edit Configurations
2. Select your Spring Boot application
3. Environment Variables → Click 📝 edit icon
4. Add variables from your `.env` file

**VS Code:**
1. Create `.vscode/launch.json`
2. Add environment variables to configuration

#### 4. Verify `.env` is not tracked by Git

```bash
git status
# .env should NOT appear in the list
```

### Option B: Using IDE Environment Variables

**IntelliJ IDEA:**
1. Run → Edit Configurations
2. Select your Spring Boot application
3. Environment Variables → Add:
   - `GOOGLE_MAPS_API_KEY=AIzaSy...`
   - `GOOGLE_MAPS_FRONTEND_API_KEY=AIzaSy...`
   - `GEOCODING_ENABLED=true`

**Eclipse:**
1. Run → Run Configurations
2. Select your application
3. Environment tab → New
4. Add each variable

**VS Code:**
1. Create `.vscode/launch.json`
2. Add to your configuration:
```json
{
  "configurations": [
    {
      "type": "java",
      "name": "Spring Boot",
      "request": "launch",
      "mainClass": "com.beezhub.student_accommodation.StudentAccommodationApplication",
      "env": {
        "GOOGLE_MAPS_API_KEY": "AIzaSy...",
        "GOOGLE_MAPS_FRONTEND_API_KEY": "AIzaSy...",
        "GEOCODING_ENABLED": "true"
      }
    }
  ]
}
```

### Option C: Using System Environment Variables

**Windows:**
```powershell
# Temporary (current session only)
$env:GOOGLE_MAPS_API_KEY="AIzaSy..."
$env:GOOGLE_MAPS_FRONTEND_API_KEY="AIzaSy..."
$env:GEOCODING_ENABLED="true"

# Permanent (requires restart)
[System.Environment]::SetEnvironmentVariable("GOOGLE_MAPS_API_KEY", "AIzaSy...", "User")
```

**Linux/Mac:**
```bash
# Add to ~/.bashrc or ~/.zshrc
export GOOGLE_MAPS_API_KEY="AIzaSy..."
export GOOGLE_MAPS_FRONTEND_API_KEY="AIzaSy..."
export GEOCODING_ENABLED="true"

# Apply changes
source ~/.bashrc
```

---

## Part 3: Production Deployment

### For Cloud Platforms

**Heroku:**
```bash
heroku config:set GOOGLE_MAPS_API_KEY="AIzaSy..."
heroku config:set GOOGLE_MAPS_FRONTEND_API_KEY="AIzaSy..."
heroku config:set GEOCODING_ENABLED="true"
```

**AWS Elastic Beanstalk:**
1. Console → Configuration → Software
2. Add environment properties

**Google Cloud Run:**
```bash
gcloud run services update SERVICE_NAME \
  --set-env-vars GOOGLE_MAPS_API_KEY="AIzaSy...",GOOGLE_MAPS_FRONTEND_API_KEY="AIzaSy...",GEOCODING_ENABLED="true"
```

**Docker:**
```yaml
# docker-compose.yml
services:
  backend:
    environment:
      - GOOGLE_MAPS_API_KEY=${GOOGLE_MAPS_API_KEY}
      - GOOGLE_MAPS_FRONTEND_API_KEY=${GOOGLE_MAPS_FRONTEND_API_KEY}
      - GEOCODING_ENABLED=true
```

Then use a `.env` file (which is git-ignored) for docker-compose.

---

## Part 4: Verify Setup

### 1. Check Backend Configuration

Start your backend application and check the logs:

```bash
cd backend
./gradlew bootRun
```

Look for: `Geocoding enabled: true` in logs (or similar indication)

### 2. Test the API Endpoint

Open browser or use curl:
```bash
curl http://localhost:8080/api/building/maps-config
```

Expected response:
```json
{
  "enabled": true,
  "apiKey": "AIzaSyYYYYY..."
}
```

### 3. Test Frontend Integration

1. Start frontend: `cd frontend && npm start`
2. Login as admin
3. Navigate to Building Management
4. Click "Add Building"
5. Start typing in the address field
6. **You should see Google Places autocomplete suggestions** 🎉

---

## 🔒 Security Best Practices

### ✅ DO:
- ✅ Use separate API keys for backend and frontend
- ✅ Restrict backend key by IP addresses
- ✅ Restrict frontend key by HTTP referrers
- ✅ Limit API restrictions to only necessary APIs
- ✅ Store keys in environment variables
- ✅ Add `.env` to `.gitignore`
- ✅ Rotate keys periodically
- ✅ Monitor API usage in Google Cloud Console
- ✅ Set up billing alerts to avoid unexpected charges

### ❌ DON'T:
- ❌ Commit API keys to Git
- ❌ Share API keys in chat/email
- ❌ Use production keys in development
- ❌ Leave keys unrestricted
- ❌ Use the same key for backend and frontend
- ❌ Hardcode keys in source code

---

## 🆘 Troubleshooting

### "Geocoding is disabled" message
- Check `GEOCODING_ENABLED=true` in environment
- Verify `GOOGLE_MAPS_API_KEY` is set
- Restart application after changing environment variables

### "This API key is not authorized to use this service"
- Check API restrictions in Google Cloud Console
- Ensure correct APIs are enabled (Geocoding, Places, Maps JavaScript)
- Verify key restrictions match your setup (IP/HTTP referrer)

### Frontend autocomplete not working
- Open browser console (F12) for errors
- Check `/api/building/maps-config` returns valid key
- Verify frontend key has Places API enabled
- Check HTTP referrer restrictions include your domain

### "Request was denied" or 403 errors
- IP restriction: Add your current IP to the backend key
- HTTP referrer: Add `http://localhost:4200/*` to frontend key
- Check API quota hasn't been exceeded in Google Cloud Console

---

## 📊 Monitoring

### Check API Usage
1. Go to Google Cloud Console
2. **APIs & Services** → **Dashboard**
3. View requests per day, errors, latency

### Set Up Billing Alerts
1. **Billing** → **Budgets & Alerts**
2. Create budget alert
3. Set threshold (e.g., $10/month)

### Free Tier Limits
- **Geocoding API**: $200 free credit/month
- **Places API**: $200 free credit/month
- Monitor usage to stay within free tier

---

## 📝 Team Setup Instructions

When a new developer joins the team:

1. **Share this guide** (not the actual keys!)
2. They should:
   - Get access to the Google Cloud project, OR
   - Create their own API keys for development
3. Copy `.env.example` to `.env`
4. Add their own API keys to `.env`
5. Never commit `.env` to Git

---

## 🔄 Key Rotation

If a key is compromised:

1. **Immediately disable** the compromised key in Google Cloud Console
2. Create a new API key with same restrictions
3. Update environment variables everywhere (local, production, CI/CD)
4. Update documentation if needed
5. Monitor for unauthorized usage

---

## Support

For issues or questions:
- Check troubleshooting section above
- Review Google Maps API documentation: https://developers.google.com/maps/documentation
- Check application logs for detailed error messages
