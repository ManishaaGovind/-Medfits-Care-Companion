"""
Secure Administrator Seed Script.
Prompts for admin email and password via CLI or environment variables (ADMIN_EMAIL / ADMIN_PASSWORD)
without hardcoding credentials in source code.
Usage:
    python -m app.seed_admin
"""
import getpass
import os
from app.database import Base, SessionLocal, engine
from app.models import Admin, User, UserProfile, WellnessGoal
from app.security import hash_password


def main():
    Base.metadata.create_all(bind=engine)
    email = os.getenv("ADMIN_EMAIL") or input("Enter administrator email: ").strip().lower()
    password = os.getenv("ADMIN_PASSWORD") or getpass.getpass("Enter administrator password (min 10 chars): ")
    full_name = os.getenv("ADMIN_NAME") or "MedFits Administrator"

    if not email or len(password) < 10:
        raise SystemExit("Valid email and password (>= 10 characters) are required.")

    db = SessionLocal()
    try:
        user = db.query(User).filter(User.email == email).first()
        if not user:
            user = User(email=email, hashed_password=hash_password(password), is_active=True)
            db.add(user)
            db.flush()
            db.add(UserProfile(user_id=user.id, full_name=full_name))
            db.add(WellnessGoal(user_id=user.id))
        else:
            user.hashed_password = hash_password(password)

        existing_admin = db.query(Admin).filter(Admin.user_id == user.id).first()
        if not existing_admin:
            db.add(Admin(user_id=user.id, role_level="super_admin"))

        db.commit()
        print(f"Administrator account configured for: {email}")
    finally:
        db.close()


if __name__ == "__main__":
    main()
