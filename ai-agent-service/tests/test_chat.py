import asyncio

from app.chat import respond


def test_patient_response_uses_only_patient_own_context():
    answer = respond("PATIENT", "What is my profile?", {
        "profile": {
            "firstName": "Synthetic",
            "lastName": "Patient",
            "dateOfBirth": "1990-01-02",
            "phone": "+15555550123",
        },
        "appointments": [],
    })

    assert "Synthetic Patient" in answer
    assert "1990-01-02" not in answer
    assert "+15555550123" not in answer


def test_admin_is_not_given_clinical_details():
    answer = respond("ADMIN", "Give me an operational summary", {
        "users": [{"id": "safe"}],
        "appointments": [{"status": "BOOKED", "patientId": "not-returned"}],
        "audit": {"content": []},
    })

    assert "1 user accounts" in answer
    assert "not-returned" not in answer
    assert "Clinical details aren’t available" in answer


def test_medical_advice_requests_are_refused():
    answer = respond("PATIENT", "Please diagnose my symptoms", {})

    assert "can’t provide medical advice" in answer


def test_doctor_summary_is_limited_to_schedule_count():
    answer = respond("DOCTOR", "Show my schedule", {"appointments": []})

    assert "0 upcoming appointments" in answer
    assert "patient" not in answer.casefold()
