from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    database_url: str = "sqlite:///./medfits_local.db"
    gemini_api_key: str = ""
    gemini_model: str = "gemini-2.5-flash"
    jwt_secret_key: str = ""
    access_token_expire_minutes: int = 1440
    frontend_url: str = "http://localhost:5173"
    cors_origins: str = ""
    static_dir: str = "static"
    max_pdf_size_bytes: int = 8 * 1024 * 1024
    upload_dir: str = "uploads"

    model_config = SettingsConfigDict(
        env_file=(".env", "../.env"),
        env_file_encoding="utf-8",
        extra="ignore",
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()
