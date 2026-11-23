import fitz  # PyMuPDF
from fastapi import APIRouter, Depends, File, HTTPException, UploadFile, status
from sqlalchemy.orm import Session
from app.ai import answer_pdf_question, explain_pdf_report
from app.config import get_settings
from app.database import get_db
from app.models import HealthReport, User, UserProfile
from app.schemas import HealthReportResponse, ReportAskRequest, ReportAskResponse
from app.security import get_current_user

router = APIRouter(prefix="/api/reports", tags=["Reports"])


@router.post("/upload", response_model=HealthReportResponse, status_code=status.HTTP_201_CREATED)
async def upload_health_report(
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    settings = get_settings()
    filename = file.filename or "report.pdf"
    if not filename.lower().endswith(".pdf") or (
        file.content_type and file.content_type not in ("application/pdf", "application/octet-stream")
    ):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Only PDF files (.pdf) are accepted.",
        )

    raw_bytes = await file.read()
    if len(raw_bytes) == 0:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Uploaded PDF file is empty.")
    if len(raw_bytes) > settings.max_pdf_size_bytes:
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail="PDF file exceeds the 8 MB maximum size limit.",
        )
    if not raw_bytes.startswith(b"%PDF-"):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid PDF header signature.",
        )

    try:
        doc = fitz.open(stream=raw_bytes, filetype="pdf")
        page_count = doc.page_count
        extracted_pages = [page.get_text("text") for page in doc]
        doc.close()
        extracted_text = "\n\n".join(p.strip() for p in extracted_pages if p.strip()).strip()
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=f"Failed to extract text from PDF: {exc}",
        )

    if not extracted_text:
        extracted_text = "No selectable text found in PDF (document may contain scanned images)."

    profile = db.query(UserProfile).filter(UserProfile.user_id == current_user.id).first()
    try:
        ai_summary = explain_pdf_report(profile=profile, file_name=filename, extracted_text=extracted_text)
    except RuntimeError as exc:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail=str(exc))
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"Gemini report analysis failed: {exc}",
        )

    report = HealthReport(
        user_id=current_user.id,
        file_name=filename[:255],
        file_size_bytes=len(raw_bytes),
        page_count=max(page_count, 1),
        extracted_text=extracted_text,
        ai_summary=ai_summary,
    )
    db.add(report)
    db.commit()
    db.refresh(report)
    return report


@router.get("", response_model=list[HealthReportResponse])
def list_reports(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return (
        db.query(HealthReport)
        .filter(HealthReport.user_id == current_user.id)
        .order_by(HealthReport.created_at.desc())
        .all()
    )


@router.get("/{report_id}", response_model=HealthReportResponse)
def get_report(
    report_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    report = (
        db.query(HealthReport)
        .filter(HealthReport.id == report_id, HealthReport.user_id == current_user.id)
        .first()
    )
    if not report:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Health report not found.")
    return report


@router.post("/{report_id}/ask", response_model=ReportAskResponse)
def ask_report(
    report_id: int,
    payload: ReportAskRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    report = (
        db.query(HealthReport)
        .filter(HealthReport.id == report_id, HealthReport.user_id == current_user.id)
        .first()
    )
    if not report:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Health report not found.")

    profile = db.query(UserProfile).filter(UserProfile.user_id == current_user.id).first()
    try:
        answer = answer_pdf_question(
            profile=profile,
            file_name=report.file_name,
            extracted_text=report.extracted_text,
            ai_summary=report.ai_summary,
            question=payload.question.strip(),
        )
    except RuntimeError as exc:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail=str(exc))
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"Gemini Q&A failed: {exc}",
        )

    return ReportAskResponse(report_id=report.id, question=payload.question.strip(), answer=answer)
