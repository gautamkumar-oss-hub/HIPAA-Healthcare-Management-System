import uuid

from fastapi.testclient import TestClient

from app.main import app, get_backend_client, memory_from_request


class FakeBackend:
    def __init__(self, role: str):
        self.role = role
        self.requested_context = None

    async def current_user(self, token: str):
        assert token == "caller-token"
        return {"id": str(uuid.uuid4()), "role": self.role}

    async def context_for(self, role: str, token: str, message: str):
        self.requested_context = role, token, message
        if role == "PATIENT":
            return {
                "profile": {"firstName": "Own", "lastName": "Profile", "dateOfBirth": "1990-01-01", "phone": "555"},
                "appointments": [],
            }
        return {"appointments": [], "patients": [], "users": [], "audit": {"content": []}}


class FakeMemory:
    def __init__(self):
        self.entries = []

    async def history(self, user_id, conversation_id):
        return []

    async def append(self, user_id, conversation_id, user_message, assistant_message):
        self.entries.append((user_id, conversation_id, user_message, assistant_message))


def test_chat_requires_a_caller_token_and_keeps_context_role_scoped():
    backend = FakeBackend("PATIENT")
    memory = FakeMemory()
    app.dependency_overrides[get_backend_client] = lambda: backend
    app.dependency_overrides[memory_from_request] = lambda: memory
    try:
        import os
        os.environ.setdefault("FIELD_ENCRYPTION_KEY", "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
        os.environ.setdefault("REDIS_URL", "redis://localhost:6379/0")
        with TestClient(app) as client:
            unauthorized = client.post("/chat", json={"message": "Show my profile"})
            assert unauthorized.status_code == 401

            response = client.post(
                "/chat",
                headers={"Authorization": "Bearer caller-token"},
                json={"message": "Show my profile"},
            )
    finally:
        app.dependency_overrides.clear()

    assert response.status_code == 200
    assert "Own Profile" in response.json()["answer"]
    assert response.json()["role"] == "PATIENT"
    assert backend.requested_context == ("PATIENT", "caller-token", "Show my profile")
    assert memory.entries[0][2] == "Show my profile"


def test_ai_provider_is_not_called_and_admin_is_denied_clinical_details():
    from app.chat import respond

    answer = respond("ADMIN", "Give me patient clinical details", {
        "users": [],
        "appointments": [],
        "audit": {"content": []},
    })

    assert "can’t access clinical details" in answer
