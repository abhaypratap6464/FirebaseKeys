# Firebase Keys Security Demo

An Android demo app showcasing **why hardcoding API keys in `strings.xml` is a critical security vulnerability**.

## What This Demo Shows

When API keys are stored in `res/values/strings.xml`, anyone can extract them from your released APK in under a minute:

```bash
# Step 1: Decompile the APK
apktool d your_app.apk

# Step 2: Find the keys instantly
cat your_app/res/values/strings.xml
```

With the extracted keys, an attacker can:
- Load Google Maps using your quota
- Query Firebase Realtime Database
- Call Routes, Air Quality, Pollen, Solar, and Weather APIs
- Rack up charges on your Google Cloud billing account

## Features

| Screen | API Used |
|--------|----------|
| Map with search | Maps SDK + Places API |
| Firebase Info | Firebase App options (apiKey, projectId, appId, etc.) |
| Database | Firebase Realtime Database REST API |
| Routes | Google Routes API |
| Air Quality | Google Air Quality API |
| Pollen | Google Pollen API |
| Solar | Google Solar API |
| Weather | Google Weather API |

## Setup

1. Clone the repo
2. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
3. Download `google-services.json` and place it in `app/`
4. Replace placeholders in `app/src/main/res/values/strings.xml` with your real keys
5. Enable the required APIs in [Google Cloud Console](https://console.cloud.google.com)
6. Build and run

## The Right Way to Handle API Keys

| Bad (demonstrated here) | Good |
|------------------------|------|
| `strings.xml` | `local.properties` + `.gitignore` |
| Hardcoded in code | Environment variables / CI secrets |
| No key restrictions | Restrict keys to your app's package name + SHA-1 |
| Public Firebase rules | `auth != null` rules on all paths |

## Key Restrictions (Google Cloud Console)

Always restrict your API keys:
- **Android apps** → restrict by package name + SHA-1 fingerprint
- **Maps/Places** → restrict to specific APIs only
- **Firebase** → enable App Check

## License

MIT — for educational and interview demonstration purposes only.
