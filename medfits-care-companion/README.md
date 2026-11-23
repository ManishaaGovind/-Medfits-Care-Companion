# MedFits Care Companion – AI-Powered Healthcare and Lifestyle Assistant

## 1. Project Overview
MedFits Care Companion is an integrated healthcare education and everyday lifestyle wellness application. It pairs a personalized AI wellness guide with daily habit tracking, PDF health report explanations, specialist support inquiries, and an administrator operations dashboard.

> **Workspace Note:** This repository contains **both**:
> - **Android App (`/app/`)**: Native Kotlin + Jetpack Compose Material 3 app powered by Firebase Auth (Google Sign-In), Cloud Firestore, and Firebase AI Logic (`gemini-3.8-flash`).
> - **Full-Stack Web App (`/medfits-care-companion/`)**: React.js + Vite + Tailwind CSS + Recharts frontend backed by Python 3.11 + FastAPI + SQLAlchemy + PostgreSQL + PyMuPDF + Google GenAI SDK (`gemini-2.5-flash`).

---

## 2. Features
- **JWT Authentication & Protected Routes** (Registration, Login, Logout, bcrypt password hashing)
- **Personalized User Profile** (Name, age range, preferred language, dietary preference, wellness goal, activity level, routine, preferences)
- **AI Wellness Chatbot across 11 Core Categories + Free-Form Questions**:
  1. Nutrition and Diet
  2. Healthy Weight Management
  3. Fitness and Physical Activity
  4. Sleep Management
  5. Mental Wellness and Stress Management
  6. Hydration
  7. Skin and Personal Care
  8. Daily Lifestyle and Routine Planning
  9. Women's Wellness Education
  10. Preventive Healthcare
  11. Healthy Habit Formation
- **Healthcare Safety Guardrails**: Educational assistant only — never diagnoses, prescribes, or alters prescribed medications; urges professional or emergency care for urgent symptoms.
- **Daily Wellness Tracker**: Water intake, sleep hours, physical activity, exercise minutes, habit checklist, personal goals, and Recharts progress visualizations.
- **PDF Health Report Analysis**: Validates PDF-only files up to 8 MB, extracts text via PyMuPDF (`fitz`), separates raw extracted facts from plain-language Gemini explanations, and supports follow-up Q&A.
- **Specialist Support**: Submit inquiries, track statuses (`pending`, `in_review`, `resolved`), and view specialist notes.
- **Admin Dashboard**: Protected analytics (users, conversations, wellness records, requests), support queue management, and wellness resource publishing.

---

## 3. Technologies Used
- **Frontend**: React 18, Vite, JavaScript, Tailwind CSS, Lucide React, Axios, React Router, Recharts
- **Backend**: Python 3.11, FastAPI, Pydantic v2, SQLAlchemy 2.0, PyJWT, Passlib/bcrypt, PyMuPDF (`fitz`), Google GenAI Python SDK (`google-genai`)
- **Database**: PostgreSQL 16
- **Containerization**: Docker & Docker Compose

---

## 4. Prerequisites
- Python **3.11+**
- Node.js **20+** and npm
- PostgreSQL **16+** (or Docker Desktop)
- Google Gemini API Key from Google AI Studio
- Windows PowerShell (or VS Code integrated terminal)

---

## 5. How to Install Python Dependencies (Windows PowerShell)
```powershell
cd medfits-care-companion\backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install --upgrade pip
pip install -r requirements.txt
```

---

## 6. How to Install Frontend Dependencies (Windows PowerShell)
```powershell
cd medfits-care-companion\frontend
npm install
```

---

## 7. How to Configure PostgreSQL
Option A — **Using Docker Compose** (recommended):
```powershell
cd medfits-care-companion
docker compose up -d db
```

Option B — **Local PostgreSQL Installation**:
```sql
CREATE USER medfits WITH PASSWORD 'your_strong_db_password';
CREATE DATABASE medfits_db OWNER medfits;
```

---

## 8. How to Create a Gemini API Key
1. Open Google AI Studio and sign in.
2. Navigate to **Get API key** -> **Create API key**.
3. Copy the generated key and place it ONLY in your backend `.env` file (`GEMINI_API_KEY=...`). Never expose it in `frontend/` code.

---

## 9. How to Configure the `.env` File
```powershell
cd medfits-care-companion
Copy-Item .env.example .env
```
Generate a cryptographically strong `JWT_SECRET_KEY` using Python:
```powershell
python -c "import secrets; print(secrets.token_hex(32))"
```
Edit `.env` and populate:
```env
DATABASE_URL=postgresql+psycopg2://medfits:your_strong_db_password@localhost:5432/medfits_db
GEMINI_API_KEY=your_gemini_api_key_here
JWT_SECRET_KEY=your_generated_64_char_hex_secret
ACCESS_TOKEN_EXPIRE_MINUTES=1440
FRONTEND_URL=http://localhost:5173
```

---

## 10. How to Start the FastAPI Backend (Windows PowerShell)
```powershell
cd medfits-care-companion\backend
.\.venv\Scripts\Activate.ps1
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```
Interactive API docs will be available at `http://localhost:8000/docs`.

---

## 11. How to Start the React Frontend (Windows PowerShell)
```powershell
cd medfits-care-companion\frontend
npm run dev
```
Open `http://localhost:5173` in your browser.

---

## 12. How to Create an Admin Account Securely
Never hardcode admin passwords in source files. Run the interactive seed script:
```powershell
cd medfits-care-companion\backend
.\.venv\Scripts\Activate.ps1
python -m app.seed_admin
```
Optional temporary demo member account (prints a randomly generated temporary password):
```powershell
python -m app.seed_demo
```

---

## 13. How to Test the Application (Windows PowerShell)
Run the FastAPI automated test suite (authentication, authorization, profile, wellness CRUD, specialist, admin RBAC, PDF upload validation):
```powershell
cd medfits-care-companion\backend
.\.venv\Scripts\Activate.ps1
pytest -v
```
Build the React production bundle to verify frontend syntax and bundling:
```powershell
cd medfits-care-companion\frontend
npm run build
```

---

## 14. Publish as a Public HTTPS Website (Google Cloud Run Starter Tier — No Credit Card / No Play Store)

You do **not** need Google Play Console, a developer account, or a credit card to publish **MedFits Care Companion** as a public HTTPS website.

### Important Note on Cloud Run Starter Tier & Database Persistence
- **Google Cloud Run Starter Tier** provides free container hosting with an automatic `https://...run.app` public URL without requiring a credit card.
- **Cloud SQL is NOT included in the no-credit-card Starter Tier** (Cloud SQL requires billing/payment setup).
- To stay **100% free with zero credit card**:
  - **Recommended (Persistent Data):** Create a free PostgreSQL database on **Neon** (`neon.tech`) or **Supabase** (no credit card required) and copy its `postgresql://...` connection string into `DATABASE_URL`.
  - **Quick Demo Mode:** Omit `DATABASE_URL` and the backend uses local SQLite (`sqlite:///./medfits_local.db`). *(Note: SQLite inside Cloud Run resets when containers restart or scale to zero.)*

### Exact Steps to Deploy to Google Cloud Run (Public HTTPS Website)
1. **Export the project** from AI Studio (**Download ZIP** or **Push to GitHub**) and open a terminal inside `medfits-care-companion/`.
2. **Sign in to Google Cloud CLI (`gcloud`)** and select/create your project:
   ```bash
   gcloud auth login
   gcloud config set project YOUR_PROJECT_ID
   ```
3. **Generate a strong JWT secret:**
   ```bash
   python -c "import secrets; print(secrets.token_hex(32))"
   ```
4. **Deploy the unified React + FastAPI website directly from source:**
   *(Uses `medfits-care-companion/Dockerfile` + `.gcloudignore`; listens on Cloud Run `$PORT` `8080` and serves both the React website and `/api/*` endpoints from one HTTPS URL.)*
   ```bash
   gcloud run deploy medfits-care-companion \
     --source . \
     --region asia-northeast1 \
     --allow-unauthenticated \
     --port 8080 \
     --set-env-vars "GEMINI_API_KEY=YOUR_GEMINI_API_KEY,JWT_SECRET_KEY=YOUR_GENERATED_HEX_SECRET,DATABASE_URL=YOUR_FREE_POSTGRES_URL"
   ```
5. **Verify your live website:**
   - Cloud Run prints a public HTTPS URL like `https://medfits-care-companion-xxxx.a.run.app`.
   - Open `https://YOUR_CLOUD_RUN_URL/api/health` to confirm `"status": "ok"`, `"jwt_configured": true`, `"gemini_configured": true`, and `"spa_bundle_present": true`.
   - Open `https://YOUR_CLOUD_RUN_URL` in desktop, laptop, or mobile browsers.

### Alternative Free Hosting (One-Click Render Blueprint)
If you prefer GitHub-based auto-deploys without CLI commands:
1. Push to GitHub and open **Render** (free GitHub login) -> **New -> Blueprint** -> select `medfits-care-companion/render.yaml`.
2. Enter your `GEMINI_API_KEY` when prompted; Render provisions PostgreSQL + the unified Docker web service automatically.
