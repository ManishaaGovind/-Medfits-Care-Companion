from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database import get_db
from app.models import User, WellnessGoal, WellnessRecord
from app.schemas import WellnessGoalUpdate, WellnessRecordCreate, WellnessRecordResponse
from app.security import get_current_user

router = APIRouter(prefix="/api/wellness", tags=["Wellness"])


def _to_response(record: WellnessRecord) -> WellnessRecordResponse:
    habits = [h.strip() for h in record.habits_completed.split(",") if h.strip()]
    return WellnessRecordResponse(
        id=record.id,
        record_date=record.record_date,
        water_ml=record.water_ml,
        sleep_hours=record.sleep_hours,
        physical_activity=record.physical_activity,
        exercise_minutes=record.exercise_minutes,
        habits_completed=habits,
        notes=record.notes,
        created_at=record.created_at,
        updated_at=record.updated_at,
    )


@router.get("", response_model=list[WellnessRecordResponse])
def list_wellness_records(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    rows = (
        db.query(WellnessRecord)
        .filter(WellnessRecord.user_id == current_user.id)
        .order_by(WellnessRecord.record_date.desc())
        .all()
    )
    return [_to_response(r) for r in rows]


@router.post("", response_model=WellnessRecordResponse, status_code=status.HTTP_201_CREATED)
def create_wellness_record(
    payload: WellnessRecordCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    record = WellnessRecord(
        user_id=current_user.id,
        record_date=payload.record_date,
        water_ml=payload.water_ml,
        sleep_hours=payload.sleep_hours,
        physical_activity=payload.physical_activity.strip(),
        exercise_minutes=payload.exercise_minutes,
        habits_completed=",".join([h.strip() for h in payload.habits_completed if h.strip()]),
        notes=payload.notes.strip(),
    )
    db.add(record)
    db.commit()
    db.refresh(record)
    return _to_response(record)


@router.put("/{record_id}", response_model=WellnessRecordResponse)
def update_wellness_record(
    record_id: int,
    payload: WellnessRecordCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    record = (
        db.query(WellnessRecord)
        .filter(WellnessRecord.id == record_id, WellnessRecord.user_id == current_user.id)
        .first()
    )
    if not record:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Wellness record not found.")

    record.record_date = payload.record_date
    record.water_ml = payload.water_ml
    record.sleep_hours = payload.sleep_hours
    record.physical_activity = payload.physical_activity.strip()
    record.exercise_minutes = payload.exercise_minutes
    record.habits_completed = ",".join([h.strip() for h in payload.habits_completed if h.strip()])
    record.notes = payload.notes.strip()

    db.commit()
    db.refresh(record)
    return _to_response(record)


@router.delete("/{record_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_wellness_record(
    record_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    record = (
        db.query(WellnessRecord)
        .filter(WellnessRecord.id == record_id, WellnessRecord.user_id == current_user.id)
        .first()
    )
    if not record:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Wellness record not found.")
    db.delete(record)
    db.commit()


@router.get("/goals/current", response_model=WellnessGoalUpdate)
def get_wellness_goals(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    goal = db.query(WellnessGoal).filter(WellnessGoal.user_id == current_user.id).first()
    if not goal:
        goal = WellnessGoal(user_id=current_user.id)
        db.add(goal)
        db.commit()
        db.refresh(goal)
    return WellnessGoalUpdate(
        water_target_ml=goal.water_target_ml,
        sleep_target_hours=goal.sleep_target_hours,
        exercise_target_minutes=goal.exercise_target_minutes,
        daily_habits_target=goal.daily_habits_target,
    )


@router.put("/goals/current", response_model=WellnessGoalUpdate)
def update_wellness_goals(
    payload: WellnessGoalUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    goal = db.query(WellnessGoal).filter(WellnessGoal.user_id == current_user.id).first()
    if not goal:
        goal = WellnessGoal(user_id=current_user.id)
        db.add(goal)
    goal.water_target_ml = payload.water_target_ml
    goal.sleep_target_hours = payload.sleep_target_hours
    goal.exercise_target_minutes = payload.exercise_target_minutes
    goal.daily_habits_target = payload.daily_habits_target
    db.commit()
    return payload
