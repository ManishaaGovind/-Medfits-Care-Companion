import os
import secrets
from datetime import date

os.environ["DATABASE_URL"] = "sqlite:///./test_medfits.db"
os.environ["JWT_SECRET_KEY"] = secrets.token_hex(32)

from fastapi.testclient import TestClient  # noqa: E402
from app.database import Base, SessionLocal, engine  # noqa: E402
from app.main import app  # noqa: E402
from app.models import Admin, User  # noqa: E402

client = TestClient(app)


def setup_module(module):
    Base.metadata.drop_all(bind=engine)
    Base.metadata.create_all(bind=engine)


def teardown_module(module):
    Base.metadata.drop_all(bind=engine)
    if os.path.exists("test_medfits.db"):
        os.remove("test_medfits.db")


def _register_and_login(email: str, password: str, full_name: str) -> str:
    res = client.post(
        "/api/auth/register",
        json={"email": email, "password": password, "full_name": full_name},
    )
    assert res.status_code == 201
    return res.json()["access_token"]


def test_auth_register_login_me_and_protected_routes():
    unauth = client.get("/api/profile")
    assert unauth.status_code == 401

    token = _register_and_login("user1@example.com", "StrongPass!123", "Jane Doe")
    headers = {"Authorization": f"Bearer {token}"}

    me = client.get("/api/auth/me", headers=headers)
    assert me.status_code == 200
    assert me.json()["email"] == "user1@example.com"
    assert me.json()["is_admin"] is False

    login = client.post(
        "/api/auth/login",
        json={"email": "user1@example.com", "password": "StrongPass!123"},
    )
    assert login.status_code == 200
    assert "access_token" in login.json()

    bad_login = client.post(
        "/api/auth/login",
        json={"email": "user1@example.com", "password": "WrongPassword"},
    )
    assert bad_login.status_code == 401


def test_profile_and_wellness_crud():
    token = _register_and_login("user2@example.com", "StrongPass!456", "Sam Rivera")
    headers = {"Authorization": f"Bearer {token}"}

    put_profile = client.put(
        "/api/profile",
        headers=headers,
        json={
            "full_name": "Sam Rivera",
            "age_range": "25-34",
            "preferred_language": "English",
            "dietary_preference": "Vegan",
            "wellness_goal": "Run 5k and drink 2.5L water",
            "activity_level": "Moderately Active",
            "preferred_daily_routine": "Morning jog, evening reading",
            "general_preferences": "Plant-based high fiber",
        },
    )
    assert put_profile.status_code == 200
    assert put_profile.json()["dietary_preference"] == "Vegan"

    create_rec = client.post(
        "/api/wellness",
        headers=headers,
        json={
            "record_date": str(date.today()),
            "water_ml": 2200,
            "sleep_hours": 7.5,
            "physical_activity": "Jogging",
            "exercise_minutes": 35,
            "habits_completed": ["Morning Hydration"],
            "notes": "Great energy",
        },
    )
    assert create_rec.status_code == 201
    rec_id = create_rec.json()["id"]

    update_rec = client.put(
        f"/api/wellness/{rec_id}",
        headers=headers,
        json={
            "record_date": str(date.today()),
            "water_ml": 2500,
            "sleep_hours": 8.0,
            "physical_activity": "Jogging",
            "exercise_minutes": 40,
            "habits_completed": ["Morning Hydration", "Screen-Free Wind Down"],
            "notes": "Updated",
        },
    )
    assert update_rec.status_code == 200
    assert update_rec.json()["water_ml"] == 2500

    delete_rec = client.delete(f"/api/wellness/{rec_id}", headers=headers)
    assert delete_rec.status_code == 204


def test_specialist_and_admin_authorization():
    user_token = _register_and_login("member@example.com", "StrongPass!789", "Member User")
    user_headers = {"Authorization": f"Bearer {user_token}"}

    req_res = client.post(
        "/api/specialist/requests",
        headers=user_headers,
        json={"category": "Sleep Management", "concern": "Need tips for consistent bedtime."},
    )
    assert req_res.status_code == 201
    req_id = req_res.json()["id"]

    # Non-admin must receive 403
    forbidden = client.get("/api/admin/dashboard", headers=user_headers)
    assert forbidden.status_code == 403

    # Create admin user and elevate in DB
    admin_token = _register_and_login("admin@example.com", "AdminPass!1234", "Admin User")
    db = SessionLocal()
    try:
        admin_user = db.query(User).filter(User.email == "admin@example.com").first()
        db.add(Admin(user_id=admin_user.id, role_level="super_admin"))
        db.commit()
    finally:
        db.close()

    admin_headers = {"Authorization": f"Bearer {admin_token}"}
    dash = client.get("/api/admin/dashboard", headers=admin_headers)
    assert dash.status_code == 200
    assert dash.json()["total_specialist_requests"] >= 1

    upd = client.put(
        f"/api/admin/requests/{req_id}",
        headers=admin_headers,
        json={"status": "in_review", "admin_notes": "Reviewing sleep habits"},
    )
    assert upd.status_code == 200
    assert upd.json()["status"] == "in_review"


def test_pdf_upload_validation():
    token = _register_and_login("pdfuser@example.com", "StrongPass!999", "PDF Tester")
    headers = {"Authorization": f"Bearer {token}"}

    # Reject non-PDF
    bad_file = client.post(
        "/api/reports/upload",
        headers=headers,
        files={"file": ("notes.txt", b"hello world", "text/plain")},
    )
    assert bad_file.status_code == 400
