# Quick Start: Google Maps API Keys Setup

⚡ **Fast setup for local development** - Full guide: [GOOGLE_MAPS_SETUP.md](./GOOGLE_MAPS_SETUP.md)

## 1️⃣ Get Your API Keys

1. Go to https://console.cloud.google.com/apis/credentials
2. Create **TWO** API keys (or use existing ones):
   - **Backend key**: Restrict to Geocoding API
   - **Frontend key**: Restrict to Places API + Maps JavaScript API

## 2️⃣ Set Up Locally (Choose ONE method)

### Method A: .env File (Easiest) ⭐

```bash
# In the backend directory
cd backend
cp .env.example .env
```

Edit `backend/.env` and replace the placeholder values:
```bash
GOOGLE_MAPS_API_KEY=AIzaSy_YOUR_BACKEND_KEY_HERE
GOOGLE_MAPS_FRONTEND_API_KEY=AIzaSy_YOUR_FRONTEND_KEY_HERE
GEOCODING_ENABLED=true
```

### Method B: IDE Environment Variables

**IntelliJ IDEA:**
- Run → Edit Configurations → Environment Variables
- Add:
  - `GOOGLE_MAPS_API_KEY=AIzaSy_YOUR_BACKEND_KEY_HERE`
  - `GOOGLE_MAPS_FRONTEND_API_KEY=AIzaSy_YOUR_FRONTEND_KEY_HERE`
  - `GEOCODING_ENABLED=true`

**VS Code:**
- Edit `.vscode/launch.json`
- Add variables to `env` section

## 3️⃣ Verify

```bash
# Start backend
cd backend
./gradlew bootRun

# In another terminal, test the endpoint
curl http://localhost:8080/api/building/maps-config

# Should return:
# {"enabled":true,"apiKey":"AIzaSy..."}
```

## 4️⃣ Test Frontend

```bash
# Start frontend
cd frontend
npm start
```

1. Login as admin
2. Go to Building Management
3. Click "Add Building"
4. Type in the address field → See Google autocomplete suggestions! 🎉

---

## ⚠️ Important Security Notes

- ✅ `.env` is already in `.gitignore` - your keys are safe
- ✅ NEVER commit API keys to Git
- ✅ Use separate keys for backend/frontend
- ✅ Restrict keys in Google Cloud Console

---

## 🆘 Troubleshooting

**"Geocoding is disabled"**
→ Check environment variables are set correctly

**Frontend autocomplete not working**
→ Check browser console (F12) for errors
→ Verify `/api/building/maps-config` returns valid data

**"API key not authorized"**
→ Add `http://localhost:4200/*` to frontend key's HTTP referrer restrictions in Google Cloud Console

---

📚 **Full Documentation**: [GOOGLE_MAPS_SETUP.md](./GOOGLE_MAPS_SETUP.md)
