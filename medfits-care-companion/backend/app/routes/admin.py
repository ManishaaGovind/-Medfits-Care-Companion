from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database import get_db
from app.models import (
    ChatSession,
    SpecialistRequest,
    User,
    WellnessRecord,
    WellnessResource,
)
from app.schemas import (
    SpecialistRequestResponse,
    SpecialistRequestUpdate,
    UserResponse,
    WellnessResourceCreate,
    WellnessResourceResponse,
)
from app.security import get_current_user, is_user_admin, require_admin

router = APIRouter(prefix="/api/admin", tags=["Admin"])


@router.get("/dashboard")
def get_admin_dashboard(
    admin_user: User = Depends(require_admin),
    db: Session = Depends(get_db),
):
    total_users = db.query(User).count()
    total_conversations = db.query(ChatSession).count()
    total_wellness_records = db.query(WellnessRecord).count()
    total_specialist_requests = db.query(SpecialistRequest).count()

    recent_users = db.query(User).order_by(User.created_at.desc()).limit(8).all()
    recent_activity = [
        {
            "id": u.id,
            "email": u.email,
            "full_name": u.profile.full_name if u.profile else "",
            "created_at": u.created_at,
        }
        for u in recent_users
    ]

    return {
        "total_users": total_users,
        "total_conversations": total_conversations,
        "total_wellness_records": total_wellness_records,
        "total_specialist_requests": total_specialist_requests,
        "recent_activity": recent_activity,
    }


@router.get("/users", response_model=list[UserResponse])
def list_users(
    admin_user: User = Depends(require_admin),
    db: Session = Depends(get_db),
):
    users = db.query(User).order_by(User.created_at.desc()).all()
    return [
        UserResponse(
            id=u.id,
            email=u.email,
            is_active=u.is_active,
            is_admin=is_user_admin(db, u.id),
            created_at=u.created_at,
        )
        for u in users
    ]


@router.get("/requests", response_model=list[SpecialistRequestResponse])
def list_all_requests(
    admin_user: User = Depends(require_admin),
    db: Session = Depends(get_db),
):
    return db.query(SpecialistRequest).order_by(SpecialistRequest.created_at.desc()).all()


@router.put("/requests/{request_id}", response_model=SpecialistRequestResponse)
def update_request(
    request_id: int,
    payload: SpecialistRequestUpdate,
    admin_user: User = Depends(require_admin),
    db: Session = Depends(get_db),
):
    req = db.query(SpecialistRequest).filter(SpecialistRequest.id == request_id).first()
    if not req:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Specialist request not found.")
    req.status = payload.status
    req.admin_notes = payload.admin_notes.strip()
    db.commit()
    db.refresh(req)
    return req


@router.get("/resources", response_model=list[WellnessResourceResponse])
def list_resources(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return db.query(WellnessResource).order_by(WellnessResource.created_at.desc()).all()


@router.post("/resources", response_model=WellnessResourceResponse, status_code=status.HTTP_201_CREATED)
def create_resource(
    payload: WellnessResourceCreate,
    admin_user: User = Depends(require_admin),
    db: Session = Depends(get_db),
):
    res = WellnessResource(
        title=payload.title.strip(),
        category=payload.category.strip(),
        summary=payload.summary.strip(),
    )
    db.add(res)
    db.commit()
    db.refresh(res)
    return res


@router.delete("/resources/{resource_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_resource(
    resource_id: int,
    admin_user: User = Depends(require_admin),
    db: Session = Depends(get_db),
):
    res = db.query(WellnessResource).filter(WellnessResource.id == resource_id).first()
    if not res:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Wellness resource not found.")
    db.delete(res)
    db.commit()
