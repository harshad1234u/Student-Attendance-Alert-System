import streamlit as st
import json
import requests
import firebase_admin
from firebase_admin import credentials, firestore

# ─────────────────────────────────────────
# 1. PAGE CONFIG  (must be first st call)
# ─────────────────────────────────────────
st.set_page_config(
    page_title="PhishCatch Kiosk",
    page_icon="⚠️",
    layout="centered"
)

# ─────────────────────────────────────────
# 2. LOAD SECRETS  (Streamlit Cloud safe)
# ─────────────────────────────────────────
try:
    NVIDIA_API_KEY = st.secrets["NVIDIA_API_KEY"]
except KeyError:
    NVIDIA_API_KEY = None
    st.warning("⚠️ NVIDIA_API_KEY is not set in Streamlit secrets. Phishing analysis is disabled.")

# ─────────────────────────────────────────
# 3. FIREBASE INIT  (in-memory, no file)
# ─────────────────────────────────────────
@st.cache_resource
def init_firebase():
    """
    Initialize Firebase using the JSON stored in Streamlit secrets.
    Returns (client, error_message). Never calls any st.* UI functions
    because @st.cache_resource forbids UI calls inside cached functions.
    """
    try:
        # Read Firebase config as a native TOML section — no JSON parsing needed,
        # so \n in the private key is never corrupted.
        firebase_dict = dict(st.secrets["firebase"])

        if not firebase_admin._apps:
            cred = credentials.Certificate(firebase_dict)
            firebase_admin.initialize_app(cred)

        return firestore.client(), None          # (client, no error)
    except KeyError:
        return None, None                        # Secret section not set — skip silently
    except Exception as e:
        return None, str(e)                      # Return error string, NOT st.toast()

db, _firebase_error = init_firebase()

# ─────────────────────────────────────────
# 4. FIREBASE HELPER
# ─────────────────────────────────────────
def save_to_firebase(text: str, threat_level: str, risk_score: int):
    """Persist a scan record to Firestore. Fails silently if DB is unavailable."""
    if not db:
        return
    try:
        db.collection("scans").document().set({
            "input_text": text,
            "threat_level": threat_level,
            "risk_score": risk_score,
            "timestamp": firestore.SERVER_TIMESTAMP,
        })
    except Exception as e:
        st.toast(f"Error saving to Firestore: {e}", icon="⚠️")

# ─────────────────────────────────────────
# 5. AI ANALYSIS
# ─────────────────────────────────────────
MODEL_PRIMARY  = "meta/llama-3.1-8b-instruct"   # Fast, low latency
MODEL_FALLBACK = "meta/llama-3.1-70b-instruct"  # More accurate, slower

SYSTEM_PROMPT = """You are a cybersecurity expert analyzing potential phishing emails or SMS.
You MUST output ONLY a valid JSON object matching this exact schema:
{
  "threat_level": "Safe" | "Medium" | "Critical",
  "risk_score": <integer between 0 and 100>,
  "red_flags": ["<string>", "<string>"],
  "advice": "<string of exactly two sentences>"
}
Do NOT wrap the JSON in markdown blocks (e.g. ```json). Just return the raw JSON."""

def analyze_phishing(text: str, retry: bool = False) -> dict:
    """
    Call NVIDIA NIM API and return a structured analysis dict.
    On first failure retries with the fallback model.
    """
    if not NVIDIA_API_KEY:
        return {"error": "🔑 NVIDIA API key is not configured. Add NVIDIA_API_KEY in Streamlit secrets."}

    model = MODEL_FALLBACK if retry else MODEL_PRIMARY
    url   = "https://integrate.api.nvidia.com/v1/chat/completions"
    headers = {
        "Authorization": f"Bearer {NVIDIA_API_KEY}",
        "Content-Type":  "application/json",
    }
    payload = {
        "model": model,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user",   "content": f"Analyze this text:\n\n{text}"},
        ],
        "temperature": 0.2,
        "max_tokens":  512,
    }

    try:
        resp = requests.post(url, headers=headers, json=payload, timeout=60)
        resp.raise_for_status()
        content = resp.json()["choices"][0]["message"]["content"].strip()

        # Strip markdown fences if model disobeyed
        if content.startswith("```json"):
            content = content[7:].rstrip("` \n")
        elif content.startswith("```"):
            content = content[3:].rstrip("` \n")

        result = json.loads(content)

        required_keys = {"threat_level", "risk_score", "red_flags", "advice"}
        if not required_keys.issubset(result):
            raise ValueError("JSON missing required fields.")

        return result

    except (json.JSONDecodeError, ValueError):
        if not retry:
            return analyze_phishing(text, retry=True)
        return {"error": "🧩 Failed to parse analysis results. Please try again."}

    except requests.exceptions.Timeout:
        if not retry:
            return analyze_phishing(text, retry=True)
        return {"error": "⏱️ Request timed out. The NVIDIA API is under heavy load — please try again in a moment."}

    except requests.exceptions.ConnectionError:
        return {"error": "🌐 Network error: Cannot reach NVIDIA API. Check your internet connection."}

    except requests.exceptions.HTTPError as e:
        status = e.response.status_code if e.response is not None else "unknown"
        if status == 401:
            return {"error": "🔑 Invalid API Key. Please verify NVIDIA_API_KEY in Streamlit secrets."}
        if status == 429:
            return {"error": "⚠️ Rate limit exceeded. Wait a moment and try again."}
        return {"error": f"API HTTP Error {status}: {e}"}

    except requests.exceptions.RequestException as e:
        return {"error": f"API Connection failed: {e}"}

    except Exception as e:
        return {"error": f"Unexpected error: {e}"}

# ─────────────────────────────────────────
# 6. UI
# ─────────────────────────────────────────
st.title("⚠️ PhishCatch Kiosk")
st.markdown("### Interactive Cybersecurity Analysis")
st.write("Paste a suspicious email, text message, or link below to instantly analyze its threat level.")

# Firebase status banner — UI calls live here, OUTSIDE the cached function
if _firebase_error:
    st.warning(f"⚠️ Firebase error: {_firebase_error}")
elif not db:
    st.info("ℹ️ Firebase is not connected — scans will not be logged. Add `FIREBASE_KEY` to Streamlit secrets to enable logging.")

input_text = st.text_area(
    "Suspicious Payload:",
    height=200,
    placeholder="Dear customer, your account has been compromised. Verify your identity here: http://fake-link.com...",
)

if st.button("🔍 Analyze Threat", type="primary", use_container_width=True):
    if not input_text.strip():
        st.error("Please enter some text to analyze.")
    else:
        with st.spinner("Analyzing payload vectors..."):
            result = analyze_phishing(input_text.strip())

        if "error" in result:
            st.error(result["error"])
        else:
            threat_level = result.get("threat_level", "Unknown")
            risk_score   = int(result.get("risk_score", 0))

            # Color-code by severity
            color_map = {"safe": "green", "medium": "#ffa500", "critical": "#ff4b4b"}
            color = color_map.get(threat_level.lower(), "gray")

            # ── Results card ──
            with st.container(border=True):
                col1, col2 = st.columns(2)
                with col1:
                    st.markdown(
                        f"### Threat Level: <span style='color:{color}'>{threat_level}</span>",
                        unsafe_allow_html=True,
                    )
                with col2:
                    st.progress(risk_score / 100, text=f"Risk Score: {risk_score}/100")

            # ── Red flags ──
            st.subheader("🚩 Detected Red Flags")
            flags = result.get("red_flags", [])
            if not flags:
                st.success("No red flags detected.")
            else:
                for flag in flags:
                    st.markdown(f"- {flag}")

            # ── Advice ──
            st.subheader("💡 Expert Advice")
            st.info(result.get("advice", "Be careful."))

            # ── Persist to Firestore ──
            save_to_firebase(input_text.strip(), threat_level, risk_score)
