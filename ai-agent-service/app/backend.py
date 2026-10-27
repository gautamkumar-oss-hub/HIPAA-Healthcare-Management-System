import httpx
from fastapi import HTTPException


class BackendClient:
    def __init__(self, base_url: str, client: httpx.AsyncClient) -> None:
        self.base_url = base_url.rstrip("/")
        self.client = client

    async def current_user(self, token: str) -> dict:
        return await self._get("/api/auth/me", token)

    async def context_for(self, role: str, token: str, message: str) -> dict:
        question = message.casefold()
        if role == "PATIENT":
            profile = None
            appointments = []
            if "profile" in question:
                profile = await self._get("/api/patients/me", token, allow_not_found=True)
            if any(word in question for word in ("appointment", "schedule", "booking")):
                appointments = await self._get("/api/appointments/me", token)
            return {"profile": profile, "appointments": appointments}
        if role == "DOCTOR":
            if any(word in question for word in ("schedule", "appointment", "patient")):
                appointments = await self._get("/api/appointments/me", token)
                patients = []
                if "patient" in question:
                    assigned_ids = {
                        item["patientId"] for item in appointments
                        if item.get("patientId") and item["status"] == "BOOKED"
                    }
                    for patient_id in list(assigned_ids)[:50]:
                        patients.append(await self._get(f"/api/patients/{patient_id}", token))
                return {"appointments": appointments, "patients": patients}
            return {"appointments": [], "patients": []}
        if role == "ADMIN":
            if not any(word in question for word in ("operation", "schedule", "user", "appointment", "audit")):
                return {"users": [], "appointments": [], "audit": {"content": []}}
            users = await self._get("/api/auth/users", token)
            appointments = await self._get("/api/appointments/me", token)
            audit = await self._get("/api/audit?page=0&size=100", token)
            return {"users": users, "appointments": appointments, "audit": audit}
        raise HTTPException(status_code=403, detail="Unsupported account role")

    async def _get(self, path: str, token: str, allow_not_found: bool = False):
        try:
            response = await self.client.get(
                f"{self.base_url}{path}",
                headers={"Authorization": f"Bearer {token}"},
            )
        except httpx.HTTPError as error:
            raise HTTPException(status_code=503, detail="Healthcare services are unavailable") from error
        if allow_not_found and response.status_code == 404:
            return None
        if response.status_code in (401, 403):
            raise HTTPException(status_code=response.status_code, detail="Request is not authorized")
        if response.is_error:
            raise HTTPException(status_code=503, detail="Healthcare services could not complete the request")
        return response.json()
