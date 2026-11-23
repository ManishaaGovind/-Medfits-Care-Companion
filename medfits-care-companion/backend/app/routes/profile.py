from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database import get_db
from app.models import User, UserProfile
from app.schemas import ProfileResponse, ProfileUpdateRequest
from app.security import get_current_user

router = APIRouter(prefix="/api/profile", tags=["Profile"])


@router.get("", response_model=ProfileResponse)
def get_profile(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    profile = db.query(UserProfile).filter(UserProfile.user_id == current_user.id).first()
    if not profile:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Profile not found.")
    return profile


@router.put("", response_model=ProfileResponse)
def update_profile(
    payload: ProfileUpdateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    profile = db.query(UserProfile).filter(UserProfile.user_id == current_user.id).first()
    if not profile:
        profile = UserProfile(user_id=current_user.id, full_name=payload.full_name.strip())
        db.add(profile)

    profile.full_name = payload.full_name.strip()
    profile.age_range = payload.age_range.strip()
    profile.preferred_language = payload.preferred_language.strip()
    profile.dietary_preference = payload.dietary_preference
    profile.wellness_goal = payload.wellness_goal.strip()
    profile.activity_level = payload.activity_level
    profile.preferred_daily_routine = payload.preferred_daily_routine.strip()
    profile.general_preferences = payload.general_preferences.strip()

    db.commit()
    db.refresh(profile)
    return profile
