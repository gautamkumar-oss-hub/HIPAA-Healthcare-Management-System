import re
import uuid
from datetime import datetime, timezone

from fastapi import HTTPException

MEDICAL_ADVICE = re.compile(
    r"\b(diagnos(?:e|is|ing)|treat(?:ment)?|medication|prescription|symptom|"
    r"what disease|should i take|dosage|emergency)\b",
    re.IGNORECASE,
)


def respond(role: str, message: str, context: dict) -> str:
    if MEDICAL_ADVICE.search(message):
        return (
            "I can’t provide medical advice or interpret symptoms. Please contact your "
            "doctor or clinic. If this may be an emergency, contact local emergency services."
        )

    question = message.casefold()
    if role == "PATIENT":
        if "profile" in question:
            profile = context.get("profile")
            if not profile:
                return "You haven’t added a profile yet. You can create one in your patient dashboard."
            details = [f"Your profile name is {profile['firstName']} {profile['lastName']}."]
            if "birth" in question or "dob" in question:
                details.append(f"Your date of birth is {profile['dateOfBirth']}.")
            if "phone" in question:
                details.append(f"Your phone number is {profile['phone']}.")
            return " ".join(details)
        if any(word in question for word in ("appointment", "schedule", "booking")):
            appointments = [
                appointment for appointment in context.get("appointments", [])
                if appointment["status"] == "BOOKED"
                and datetime.fromisoformat(appointment["startAt"].replace("Z", "+00:00"))
                > datetime.now(timezone.utc)
            ]
            if not appointments:
                return "You don’t have any upcoming appointments. You can browse available times in your dashboard."
            appointments.sort(key=lambda appointment: appointment["startAt"])
            return "Your next appointment is " + appointments[0]["startAt"] + "."
        return "I can help with your own profile, appointments, or general clinic information."

    if role == "DOCTOR":
        if "patient" in question:
            patients = context.get("patients", [])
            if not patients:
                return "There are no patient records available from your assigned appointments."
            names = ", ".join(f"{patient['firstName']} {patient['lastName']}" for patient in patients[:5])
            return f"You have {len(patients)} assigned patient records. Current assigned patients include: {names}."
        if any(word in question for word in ("schedule", "appointment", "patient")):
            appointments = [
                appointment for appointment in context.get("appointments", [])
                if appointment["status"] == "BOOKED"
                and datetime.fromisoformat(appointment["startAt"].replace("Z", "+00:00"))
                > datetime.now(timezone.utc)
            ]
            return (
                f"You have {len(appointments)} upcoming appointments on your schedule. "
                "Open your schedule in the dashboard to review assigned appointments."
            )
        return "I can help with your schedule and appointments assigned to you."

    if role == "ADMIN":
        if any(word in question for word in ("clinical", "diagnosis", "medical record", "treatment")):
            return "I can’t access clinical details in the admin assistant. Please use an authorized clinical workflow."
        if any(word in question for word in ("operation", "schedule", "user", "appointment", "audit")):
            appointments = context.get("appointments", [])
            users = context.get("users", [])
            audit = context.get("audit", {}).get("content", [])
            return (
                f"Operational overview: {len(users)} user accounts, {len(appointments)} appointment slots, "
                f"and {len(audit)} recent audit records. Clinical details aren’t available in the admin assistant."
            )
        return "I can help with operational account, schedule, and audit summaries without clinical details."

    raise HTTPException(status_code=403, detail="Unsupported account role")


def conversation_id(value: uuid.UUID | None) -> uuid.UUID:
    return value or uuid.uuid4()
