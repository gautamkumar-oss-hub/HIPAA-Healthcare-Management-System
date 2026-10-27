import json
import os
import urllib.error
import urllib.request


api_url = os.environ.get("API_URL", "http://localhost:8080/api").rstrip("/")
admin_email = os.environ["BOOTSTRAP_ADMIN_EMAIL"]
admin_password = os.environ["BOOTSTRAP_ADMIN_PASSWORD"]
admin_code = os.environ["ADMIN_MFA_CODE"]
doctor_email = os.environ["SEED_DOCTOR_EMAIL"]
doctor_password = os.environ["SEED_DOCTOR_PASSWORD"]
patient_email = os.environ["SEED_PATIENT_EMAIL"]
patient_password = os.environ["SEED_PATIENT_PASSWORD"]


def request(method, path, payload=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(payload).encode() if payload is not None else None
    request_object = urllib.request.Request(
        f"{api_url}{path}", data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request_object, timeout=15) as response:
            body = response.read()
            return json.loads(body) if body else None
    except urllib.error.HTTPError as error:
        detail = error.read().decode(errors="replace")
        raise RuntimeError(f"API request failed with status {error.code}: {detail}") from error


admin = request("POST", "/auth/login", {
    "email": admin_email, "password": admin_password, "mfaCode": admin_code,
})
admin_token = admin["accessToken"]
users = request("GET", "/auth/users", token=admin_token)
if not any(user["email"].casefold() == doctor_email.casefold()
           and user["role"] == "DOCTOR" for user in users):
    request("POST", "/auth/users", {
        "email": doctor_email, "password": doctor_password, "role": "DOCTOR",
    }, token=admin_token)

try:
    patient = request("POST", "/auth/register", {
        "email": patient_email, "password": patient_password,
    })
except RuntimeError as error:
    if "status 409" not in str(error):
        raise
    patient = request("POST", "/auth/login", {
        "email": patient_email, "password": patient_password,
    })

request("PUT", "/patients/me", {
    "firstName": "Taylor",
    "lastName": "Example",
    "dateOfBirth": "1990-01-02",
    "phone": "+15555550199",
}, token=patient["accessToken"])
print("Synthetic admin, doctor, and patient accounts are ready.")
