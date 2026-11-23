from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session
from app.database import get_db
from app.models import SpecialistRequest, User
from app.schemas import SpecialistRequestCreate, SpecialistRequestResponse
from app.security import get_current_user

router = APIRouter(prefix="/api/specialist", tags=["Specialist Support"])


@router.post("/requests", response_model=SpecialistRequestResponse, status_code=status.HTTP_201_CREATED)
def create_request(
    payload: SpecialistRequestCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    req = SpecialistRequest(
        user_id=current_user.id,
        category=payload.category.strip(),
        concern=payload.concern.strip(),
        status="pending",
    )
    db.add(req)
    db.commit()
    db.refresh(req)
    return req


@router.get("/requests", response_model=list[SpecialistRequestResponse])
def list_user_requests(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return (
        db.query(SpecialistRequest)
        .filter(SpecialistRequest.user_id == current_user.id)
        .order_by(SpecialistRequest.created_at.desc())
        .all()
    )
