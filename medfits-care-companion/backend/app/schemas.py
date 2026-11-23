from datetime import date, datetime
from typing import Literal, Optional
from pydantic import BaseModel, EmailStr, Field


class RegisterRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)
    full_name: str = Field(min_length=1, max_length=120)


class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=1, max_length=128)


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"


class UserResponse(BaseModel):
    id: int
    email: EmailStr
    is_active: bool
    is_admin: bool
    created_at: datetime

    model_config = {"from_attributes": True}


class ProfileUpdateRequest(BaseModel):
    full_name: str = Field(min_length=1, max_length=120)
    age_range: str = Field(min_length=1, max_length=40)
    preferred_language: str = Field(min_length=1, max_length=40)
    dietary_preference: Literal["Vegetarian", "Non-vegetarian", "Vegan"]
    wellness_goal: str = Field(default="", max_length=255)
    activity_level: Literal["Sedentary", "Lightly Active", "Moderately Active", "Very Active"]
    preferred_daily_routine: str = Field(default="", max_length=1000)
    general_preferences: str = Field(default="", max_length=1000)


class ProfileResponse(BaseModel):
    full_name: str
    age_range: str
    preferred_language: str
    dietary_preference: str
    wellness_goal: str
    activity_level: str
    preferred_daily_routine: str
    general_preferences: str
    updated_at: datetime

    model_config = {"from_attributes": True}


class ChatRequest(BaseModel):
    session_id: Optional[int] = None
    category: str = Field(default="General Wellness", min_length=1, max_length=80)
    message: str = Field(min_length=1, max_length=4000)


class ChatMessageResponse(BaseModel):
    id: int
    role: str
    content: str
    created_at: datetime

    model_config = {"from_attributes": True}


class ChatSessionSummary(BaseModel):
    id: int
    title: str
    category: str
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class ChatSessionDetail(ChatSessionSummary):
    messages: list[ChatMessageResponse] = []


class ChatReplyResponse(BaseModel):
    session_id: int
    category: str
    reply: str
    messages: list[ChatMessageResponse]


class WellnessRecordCreate(BaseModel):
    record_date: date
    water_ml: int = Field(ge=0, le=10000)
    sleep_hours: float = Field(ge=0.0, le=24.0)
    physical_activity: str = Field(min_length=1, max_length=120)
    exercise_minutes: int = Field(ge=0, le=600)
    habits_completed: list[str] = Field(default_factory=list, max_length=20)
    notes: str = Field(default="", max_length=1000)


class WellnessGoalUpdate(BaseModel):
    water_target_ml: int = Field(ge=500, le=6000)
    sleep_target_hours: float = Field(ge=4.0, le=12.0)
    exercise_target_minutes: int = Field(ge=10, le=300)
    daily_habits_target: int = Field(ge=1, le=15)


class WellnessRecordResponse(BaseModel):
    id: int
    record_date: date
    water_ml: int
    sleep_hours: float
    physical_activity: str
    exercise_minutes: int
    habits_completed: list[str]
    notes: str
    created_at: datetime
    updated_at: datetime


class HealthReportResponse(BaseModel):
    id: int
    file_name: str
    file_size_bytes: int
    page_count: int
    extracted_text: str
    ai_summary: str
    created_at: datetime

    model_config = {"from_attributes": True}


class ReportAskRequest(BaseModel):
    question: str = Field(min_length=2, max_length=2000)


class ReportAskResponse(BaseModel):
    report_id: int
    question: str
    answer: str


class SpecialistRequestCreate(BaseModel):
    category: str = Field(min_length=1, max_length=80)
    concern: str = Field(min_length=5, max_length=2000)


class SpecialistRequestUpdate(BaseModel):
    status: Literal["pending", "in_review", "resolved"]
    admin_notes: str = Field(default="", max_length=1000)


class SpecialistRequestResponse(BaseModel):
    id: int
    user_id: int
    category: str
    concern: str
    status: str
    admin_notes: str
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class WellnessResourceCreate(BaseModel):
    title: str = Field(min_length=2, max_length=160)
    category: str = Field(min_length=2, max_length=80)
    summary: str = Field(min_length=5, max_length=2000)


class WellnessResourceResponse(BaseModel):
    id: int
    title: str
    category: str
    summary: str
    created_at: datetime

    model_config = {"from_attributes": True}
