"""
Optional Demo Account Creation Script using temporary generated credentials.
Usage:
    python -m app.seed_demo
"""
import secrets
from datetime import date, timedelta
from app.database import Base, SessionLocal, engine
from app.models import User, UserProfile, WellnessGoal, WellnessRecord
from app.security import hash_password


def main():
    Base.metadata.create_all(bind=engine)
    demo_email = f"demo+{secrets.token_hex(3)}@medfits.local"
    temp_password = secrets.token_urlsafe(12)

    db = SessionLocal()
    try:
        user = User(email=demo_email, hashed_password=hash_password(temp_password), is_active=True)
        db.add(user)
        db.flush()

        db.add(
            UserProfile(
                user_id=user.id,
                full_name="Alex Morgan",
                age_range="25-34",
                preferred_language="English",
                dietary_preference="Vegetarian",
                wellness_goal="Improve sleep consistency and daily hydration",
                activity_level="Moderately Active",
                preferred_daily_routine="7 AM walk, balanced lunch, 10 PM wind-down",
                general_preferences="Plant-forward meals, gentle yoga",
            )
        )
        db.add(WellnessGoal(user_id=user.id))

        today = date.today()
        for offset in range(5):
            d = today - timedelta(days=offset)
            db.add(
                WellnessRecord(
                    user_id=user.id,
                    record_date=d,
                    water_ml=1900 + offset * 150,
                    sleep_hours=7.0 + (offset % 2) * 0.5,
                    physical_activity="Brisk Walking",
                    exercise_minutes=30 + offset * 5,
                    habits_completed="Morning Hydration,20+ Min Movement",
                    notes="Sample seeded wellness entry",
                )
            )

        db.commit()
        print("Temporary demo account created:")
        print(f"  Email:    {demo_email}")
        print(f"  Password: {temp_password}")
        print("Change or delete this demo account before production use.")
    finally:
        db.close()


if __name__ == "__main__":
    main()
