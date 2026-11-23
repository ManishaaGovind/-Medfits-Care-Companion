from typing import Optional
from google import genai
from google.genai import types
from app.config import get_settings
from app.models import ChatMessage, UserProfile

CORE_CATEGORIES = [
    "Nutrition and Diet",
    "Healthy Weight Management",
    "Fitness and Physical Activity",
    "Sleep Management",
    "Mental Wellness and Stress Management",
    "Hydration",
    "Skin and Personal Care",
    "Daily Lifestyle and Routine Planning",
    "Women's Wellness Education",
    "Preventive Healthcare",
    "Healthy Habit Formation",
]

def _get_model_name() -> str:
    return get_settings().gemini_model or "gemini-2.5-flash"


def _get_client() -> genai.Client:
    settings = get_settings()
    if not settings.gemini_api_key:
        raise RuntimeError(
            "GEMINI_API_KEY is not configured in environment variables."
        )
    return genai.Client(api_key=settings.gemini_api_key)


def _build_system_instruction(profile: Optional[UserProfile], category: str) -> str:
    if profile:
        profile_block = (
            f"USER PROFILE:\n"
            f"- Name: {profile.full_name}\n"
            f"- Age / Range: {profile.age_range}\n"
            f"- Preferred Language: {profile.preferred_language}\n"
            f"- Dietary Preference: {profile.dietary_preference}\n"
            f"- Wellness Goal: {profile.wellness_goal or 'Not yet specified'}\n"
            f"- Activity Level: {profile.activity_level}\n"
            f"- Preferred Daily Routine: {profile.preferred_daily_routine or 'Not yet specified'}\n"
            f"- General Preferences: {profile.general_preferences or 'Not yet specified'}\n"
        )
    else:
        profile_block = (
            "USER PROFILE: Not completed yet. Never assume missing health or lifestyle details; "
            "ask polite clarifying follow-up questions when needed."
        )

    return f"""You are MedFits Care Companion, an educational healthcare and lifestyle wellness assistant.
Active Category: {category}
Supported Core Categories: {", ".join(CORE_CATEGORIES)} (plus free-form wellness education).

{profile_block}

MANDATORY SAFETY AND QUALITY RULES:
1. Respond in the user's preferred language ({profile.preferred_language if profile else 'English'}).
2. You are an educational assistant, NOT a physician. NEVER diagnose diseases, NEVER prescribe medications, and NEVER suggest changing or stopping prescribed treatments.
3. Never claim a PDF health report or lab metric confirms a disease. Explain uncertainty clearly and recommend discussing results with a qualified clinician.
4. If the user mentions potentially urgent or emergency symptoms (e.g., chest pain, difficulty breathing, sudden weakness/numbness, severe bleeding, suicidal thoughts), immediately advise seeking urgent medical evaluation or contacting local emergency services. Do not give false reassurance.
5. Use clear headings and bullet points where helpful, personalize based on profile attributes, avoid repetitive generic answers, and ask relevant clarifying questions when information is missing.
"""


def generate_chat_reply(
    profile: Optional[UserProfile],
    category: str,
    history: list[ChatMessage],
    user_message: str,
) -> str:
    client = _get_client()
    system_instruction = _build_system_instruction(profile, category)

    contents: list[types.Content] = []
    for msg in history[-16:]:
        role = "user" if msg.role == "user" else "model"
        contents.append(types.Content(role=role, parts=[types.Part.from_text(text=msg.content)]))
    contents.append(types.Content(role="user", parts=[types.Part.from_text(text=user_message)]))

    response = client.models.generate_content(
        model=_get_model_name(),
        contents=contents,
        config=types.GenerateContentConfig(
            system_instruction=system_instruction,
            temperature=0.6,
        ),
    )
    return (response.text or "").strip() or "I could not generate a response right now. Please try again."


def explain_pdf_report(
    profile: Optional[UserProfile],
    file_name: str,
    extracted_text: str,
) -> str:
    client = _get_client()
    system_instruction = _build_system_instruction(profile, "PDF Health Report Explanation")

    prompt = f"""Analyze the following extracted text from PDF health report "{file_name}".

Format your response into two distinct sections:
## Document Findings Listed
Summarize strictly what is written in the extracted text without fabricating missing numbers.

## Plain-Language Educational Explanation
Explain any medical terms, abbreviations, and general concepts simply. Emphasize that this explanation is educational only, does not diagnose any condition, and should be reviewed with a qualified healthcare professional.

Extracted PDF Text:
{extracted_text[:30000]}
"""
    response = client.models.generate_content(
        model=_get_model_name(),
        contents=prompt,
        config=types.GenerateContentConfig(
            system_instruction=system_instruction,
            temperature=0.4,
        ),
    )
    return (response.text or "").strip() or "Unable to summarize this PDF document."


def answer_pdf_question(
    profile: Optional[UserProfile],
    file_name: str,
    extracted_text: str,
    ai_summary: str,
    question: str,
) -> str:
    client = _get_client()
    system_instruction = _build_system_instruction(profile, "PDF Health Report Q&A")

    prompt = f"""Document Name: {file_name}

Extracted Document Text:
{extracted_text[:20000]}

Previous Educational Summary:
{ai_summary[:8000]}

User Question: {question}

Clearly distinguish exact facts present in the document from your plain-language educational explanation. Never diagnose or prescribe.
"""
    response = client.models.generate_content(
        model=_get_model_name(),
        contents=prompt,
        config=types.GenerateContentConfig(
            system_instruction=system_instruction,
            temperature=0.4,
        ),
    )
    return (response.text or "").strip() or "Unable to answer that question right now."
