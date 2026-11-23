from contextlib import asynccontextmanager
from pathlib import Path
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from app.config import get_settings
from app.database import Base, engine
from app.routes import admin, auth, chat, profile, reports, specialist, wellness


@asynccontextmanager
async def lifespan(app: FastAPI):
    Base.metadata.create_all(bind=engine)
    yield


settings = get_settings()

app = FastAPI(
    title="MedFits Care Companion API",
    description="AI-Powered Healthcare and Lifestyle Assistant Backend",
    version="1.0.0",
    lifespan=lifespan,
)

allowed_origins = {
    settings.frontend_url,
    "http://localhost:5173",
    "http://127.0.0.1:5173",
}
if settings.cors_origins:
    for origin in settings.cors_origins.split(","):
        cleaned = origin.strip()
        if cleaned:
            allowed_origins.add(cleaned)

app.add_middleware(
    CORSMiddleware,
    allow_origins=sorted(allowed_origins),
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(profile.router)
app.include_router(chat.router)
app.include_router(wellness.router)
app.include_router(reports.router)
app.include_router(specialist.router)
app.include_router(admin.router)


@app.get("/api/health")
def health_check():
    db_engine = "sqlite" if settings.database_url.startswith("sqlite") else "postgresql"
    spa_present = (Path(settings.static_dir).resolve() / "index.html").exists()
    return {
        "status": "ok",
        "service": "medfits-care-companion",
        "database_engine": db_engine,
        "jwt_configured": True,
        "jwt_mode": "env" if settings.jwt_secret_key else "runtime_generated",
        "gemini_configured": bool(settings.gemini_api_key),
        "spa_bundle_present": spa_present,
    }


static_path = Path(settings.static_dir).resolve()
if static_path.exists() and (static_path / "index.html").exists():
    assets_dir = static_path / "assets"
    if assets_dir.exists():
        app.mount("/assets", StaticFiles(directory=str(assets_dir)), name="assets")

    @app.get("/{full_path:path}", include_in_schema=False)
    def serve_spa(full_path: str):
        if full_path.startswith("api/") or full_path == "api":
            raise HTTPException(status_code=404, detail="API endpoint not found.")
        candidate = (static_path / full_path).resolve()
        if (
            full_path
            and candidate.is_file()
            and static_path in candidate.parents
        ):
            return FileResponse(str(candidate))
        return FileResponse(str(static_path / "index.html"))
