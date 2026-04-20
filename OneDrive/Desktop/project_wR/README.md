# ⚠️ PhishCatch Kiosk

> An AI-powered interactive cybersecurity kiosk that instantly analyzes suspicious emails, SMS messages, and links for phishing threats.

[![Streamlit App](https://static.streamlit.io/badges/streamlit_badge_black_white.svg)](https://share.streamlit.io)
![Python](https://img.shields.io/badge/Python-3.11-blue)
![Streamlit](https://img.shields.io/badge/Streamlit-Cloud-red)
![Firebase](https://img.shields.io/badge/Firebase-Firestore-orange)
![NVIDIA](https://img.shields.io/badge/NVIDIA-NIM%20API-green)

---

## 🚀 Features

| Feature | Detail |
|---|---|
| ⚡ Dual-Model AI Analysis | Tries fast **Llama 3.1 8B** first, auto-falls back to **70B** on failure |
| 🎯 Structured Threat Report | Returns Threat Level, Risk Score (0–100), Red Flags, Expert Advice |
| 🔥 Firebase Firestore Logging | Every scan is auto-saved to Firestore in the background |
| 🛡️ Streamlit Secrets | Zero hardcoded keys — fully secure via `st.secrets` |
| 🔁 Smart Retry Logic | Auto-retries on timeout, JSON parse failure, or model error |
| 🌐 Graceful Error Handling | User-friendly messages for 401, 429, timeout, network errors |
| ☁️ Streamlit Cloud Ready | No `.env`, no file-based Firebase key — deploys with zero changes |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| UI Framework | [Streamlit](https://streamlit.io) |
| AI / LLM | [NVIDIA NIM API](https://build.nvidia.com) — Llama 3.1 8B & 70B Instruct |
| Database | [Firebase Firestore](https://firebase.google.com/products/firestore) |
| Language | Python 3.11 |
| Deployment | Streamlit Cloud |

---

## 🧠 How the AI Analysis Works

```
User Input
    │
    ▼
analyze_phishing(text)
    │
    ├─► 1st attempt: meta/llama-3.1-8b-instruct  (fast, ~5-10s)
    │       │
    │       ├─ Success → return structured JSON result
    │       │
    │       └─ Fail (timeout / bad JSON / error)
    │               │
    │               ▼
    └─► 2nd attempt: meta/llama-3.1-70b-instruct (accurate, ~30-60s)
                    │
                    ├─ Success → return structured JSON result
                    └─ Fail → return user-friendly error message
```

**AI Response Schema (strict JSON):**
```json
{
  "threat_level": "Safe" | "Medium" | "Critical",
  "risk_score": 0-100,
  "red_flags": ["string", "string"],
  "advice": "Two sentence expert advice string"
}
```

---

## 📂 Project Structure

```
phishcatch-kiosk/
├── app.py                    # Main Streamlit application
├── requirements.txt          # Python dependencies (3 packages)
├── README.md                 # This file
├── .gitignore                # Excludes secrets, cache, venv
└── .streamlit/
    └── secrets.toml          # LOCAL ONLY — never committed to Git
```

---

## ⚙️ Local Development Setup

### Prerequisites
- Python 3.11
- A [NVIDIA NIM API key](https://build.nvidia.com)
- A Firebase project with Firestore enabled

### 1. Clone the repository
```bash
git clone https://github.com/harshad1234u/Student-Attendance-Alert-System.git
cd Student-Attendance-Alert-System
```

### 2. Install dependencies
```bash
pip install -r requirements.txt
```

### 3. Create local secrets file
Create `.streamlit/secrets.toml` (this file is git-ignored):

```toml
NVIDIA_API_KEY = "nvapi-your-key-here"

[firebase]
type = "service_account"
project_id = "your-project-id"
private_key_id = "your-key-id"
private_key = "-----BEGIN PRIVATE KEY-----\nYOUR_KEY\n-----END PRIVATE KEY-----\n"
client_email = "firebase-adminsdk@your-project.iam.gserviceaccount.com"
client_id = "your-client-id"
auth_uri = "https://accounts.google.com/o/oauth2/auth"
token_uri = "https://oauth2.googleapis.com/token"
auth_provider_x509_cert_url = "https://www.googleapis.com/oauth2/v1/certs"
client_x509_cert_url = "https://www.googleapis.com/robot/v1/metadata/x509/your-service-account"
universe_domain = "googleapis.com"
```

> ⚠️ **Important:** Use the `[firebase]` TOML section format — NOT a raw JSON string. The JSON string approach corrupts `\n` characters in the private key.

### 4. Run locally
```bash
streamlit run app.py
```

App opens at **http://localhost:8501**

---

## ☁️ Streamlit Cloud Deployment

### Step 1 — Push to GitHub
```bash
git add app.py requirements.txt README.md .gitignore
git commit -m "deploy: PhishCatch Kiosk"
git push origin main
```
> Do NOT push `.streamlit/secrets.toml` — it is git-ignored.

### Step 2 — Deploy on Streamlit Cloud
1. Go to [share.streamlit.io](https://share.streamlit.io) → Sign in with GitHub
2. Click **"New app"** → Select your repo
3. Set **Main file**: `app.py` | **Python version**: `3.11`
4. Click **"Advanced settings"** → **Secrets** tab

### Step 3 — Add Secrets in Streamlit Cloud
Paste the following TOML format in the Secrets box:

```toml
NVIDIA_API_KEY = "nvapi-your-key-here"

[firebase]
type = "service_account"
project_id = "your-project-id"
private_key_id = "your-private-key-id"
private_key = "-----BEGIN PRIVATE KEY-----\nYOUR_FULL_KEY_HERE\n-----END PRIVATE KEY-----\n"
client_email = "firebase-adminsdk@your-project.iam.gserviceaccount.com"
client_id = "your-client-id"
auth_uri = "https://accounts.google.com/o/oauth2/auth"
token_uri = "https://oauth2.googleapis.com/token"
auth_provider_x509_cert_url = "https://www.googleapis.com/oauth2/v1/certs"
client_x509_cert_url = "https://www.googleapis.com/robot/v1/metadata/x509/your-service-account"
universe_domain = "googleapis.com"
```

5. Click **Save** → Click **Deploy** 🎉

---

## 🔐 Getting API Keys

### NVIDIA NIM API Key
1. Go to [build.nvidia.com](https://build.nvidia.com)
2. Sign in / Create account → **API Keys** → **Generate Key**
3. Key format: `nvapi-xxxxxxxxxxxxxxxxxxxx`

### Firebase Service Account Key
1. Go to [Firebase Console](https://console.firebase.google.com) → Your Project
2. **Project Settings** → **Service Accounts** → **Generate new private key**
3. Open the downloaded JSON — copy each field into the `[firebase]` TOML section

---

## 🐛 Troubleshooting

| Error | Cause | Fix |
|---|---|---|
| `⚠️ NVIDIA_API_KEY is not set` | Secret missing in Streamlit Cloud | App Settings → Secrets → Add `NVIDIA_API_KEY` |
| `🔑 Invalid API Key` | Wrong/expired NVIDIA key | Get fresh key from [build.nvidia.com](https://build.nvidia.com) |
| `⏱️ Request timed out` | NVIDIA API under heavy load | App auto-retries with 70B model; try again |
| `Firebase error: Invalid control character` | Used JSON string instead of TOML section | Use `[firebase]` TOML section format (not `FIREBASE_KEY = """..."""`) |
| `CacheReplayClosureError` | `st.*` UI call inside `@st.cache_resource` | Already fixed — UI calls are outside the cached function |
| `DefaultCredentialsError` | Firebase initialized twice | Handled by `if not firebase_admin._apps:` check |
| `ModuleNotFoundError: dotenv` | Old dependency | Removed — `python-dotenv` no longer needed |

---

## 📦 Dependencies

```
streamlit        # UI framework
requests         # HTTP client for NVIDIA API
firebase-admin   # Firestore database client
```
> Total: 3 packages — lightweight and fast to install on Streamlit Cloud.

---

## 🗄️ Firebase Firestore Schema

Each scan is stored in the `scans` collection:

```json
{
  "input_text": "The suspicious message analyzed",
  "threat_level": "Critical",
  "risk_score": 92,
  "timestamp": "2026-04-20T15:30:00Z"
}
```

---

## 📋 Changelog

| Version | Change |
|---|---|
| v1.0 | Initial release with `.env` + `firebase-key.json` |
| v1.1 | Migrated to `st.secrets` for Streamlit Cloud compatibility |
| v1.2 | Fixed `CacheReplayClosureError` — removed `st.toast()` from `@st.cache_resource` |
| v1.3 | Fixed Firebase `Invalid control character` — switched to `[firebase]` TOML section |
| v1.4 | Added dual-model strategy: 8B fast → 70B fallback with 60s timeout |
