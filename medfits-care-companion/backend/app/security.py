from datetime import datetime, timedelta, timezone
import logging
import secrets
from typing import Optional
import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from passlib.context import CryptContext
from sqlalchemy.orm import Session
from app.config import get_settings
from app.database import get_db
from app.models import Admin, User

logger = logging.getLogger("medfits.security")
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/auth/login")

ALGORITHM = "HS256"
_RUNTIME_FALLBACK_SECRET: Optional[str] = None


def _get_jwt_secret() -> str:
    global _RUNTIME_FALLBACK_SECRET
    settings = get_settings()
    if settings.jwt_secret_key:
        return settings.jwt_secret_key
    if _RUNTIME_FALLBACK_SECRET is None:
        _RUNTIME_FALLBACK_SECRET = secrets.token_hex(32)
        logger.warning(
            "JWT_SECRET_KEY not set; generated ephemeral runtime secret. "
            "Set JWT_SECRET_KEY in environment variables to persist sessions across restarts."
        )
    return _RUNTIME_FALLBACK_SECRET


def hash_password(password: str) -> str:
    return pwd_context.hash(password)


def verify_password(plain_password: str, hashed_password: str) -> bool:
    return pwd_context.verify(plain_password, hashed_password)


def create_access_token(user_id: int, expires_minutes: Optional[int] = None) -> str:
    settings = get_settings()
    expire_delta = timedelta(minutes=expires_minutes or settings.access_token_expire_minutes)
    payload = {
        "sub": str(user_id),
        "exp": datetime.now(timezone.utc) + expire_delta,
        "iat": datetime.now(timezone.utc),
    }
    return jwt.encode(payload, _get_jwt_secret(), algorithm=ALGORITHM)


def get_current_user(
    token: str = Depends(oauth2_scheme),
    db: Session = Depends(get_db),
) -> User:
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate authentication credentials.",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = jwt.decode(token, _get_jwt_secret(), algorithms=[ALGORITHM])
        subject = payload.get("sub")
        if subject is None:
            raise credentials_exception
        user_id = int(subject)
    except (jwt.PyJWTError, ValueError):
        raise credentials_exception

    user = db.query(User).filter(User.id == user_id, User.is_active.is_(True)).first()
    if user is None:
        raise credentials_exception
    return user


def is_user_admin(db: Session, user_id: int) -> bool:
    return db.query(Admin).filter(Admin.user_id == user_id).first() is not None


def require_admin(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> User:
    if not is_user_admin(db, current_user.id):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Administrator privileges required.",
        )
    return current_user
