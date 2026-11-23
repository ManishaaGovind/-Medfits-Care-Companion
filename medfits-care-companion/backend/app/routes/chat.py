from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.ai import generate_chat_reply
from app.database import get_db
from app.models import ChatMessage, ChatSession, User, UserProfile
from app.schemas import (
    ChatReplyResponse,
    ChatRequest,
    ChatSessionDetail,
    ChatSessionSummary,
)
from app.security import get_current_user

router = APIRouter(prefix="/api/chat", tags=["Chat"])


@router.post("", response_model=ChatReplyResponse)
def send_chat_message(
    payload: ChatRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if payload.session_id is not None:
        session = (
            db.query(ChatSession)
            .filter(ChatSession.id == payload.session_id, ChatSession.user_id == current_user.id)
            .first()
        )
        if not session:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Chat session not found.")
        history = list(session.messages)
    else:
        session = ChatSession(
            user_id=current_user.id,
            title=payload.message.strip()[:80] or payload.category,
            category=payload.category,
        )
        db.add(session)
        db.flush()
        history = []

    profile = db.query(UserProfile).filter(UserProfile.user_id == current_user.id).first()

    try:
        ai_text = generate_chat_reply(
            profile=profile,
            category=payload.category,
            history=history,
            user_message=payload.message.strip(),
        )
    except RuntimeError as exc:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail=str(exc))
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"Gemini AI generation failed: {exc}",
        )

    user_msg = ChatMessage(session_id=session.id, role="user", content=payload.message.strip())
    ai_msg = ChatMessage(session_id=session.id, role="assistant", content=ai_text)
    session.category = payload.category
    db.add(user_msg)
    db.add(ai_msg)
    db.commit()
    db.refresh(session)

    return ChatReplyResponse(
        session_id=session.id,
        category=session.category,
        reply=ai_text,
        messages=session.messages,
    )


@router.get("/sessions", response_model=list[ChatSessionSummary])
def list_sessions(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return (
        db.query(ChatSession)
        .filter(ChatSession.user_id == current_user.id)
        .order_by(ChatSession.updated_at.desc())
        .all()
    )


@router.get("/sessions/{session_id}", response_model=ChatSessionDetail)
def get_session(
    session_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    session = (
        db.query(ChatSession)
        .filter(ChatSession.id == session_id, ChatSession.user_id == current_user.id)
        .first()
    )
    if not session:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Chat session not found.")
    return session


@router.delete("/sessions/{session_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_session(
    session_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    session = (
        db.query(ChatSession)
        .filter(ChatSession.id == session_id, ChatSession.user_id == current_user.id)
        .first()
    )
    if not session:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Chat session not found.")
    db.delete(session)
    db.commit()
