import base64
import os
import uuid
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

import httpx
from fastapi import Depends, FastAPI, HTTPException
from fastapi import Request
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel, Field
from redis.asyncio import Redis

from app.backend import BackendClient
from app.chat import conversation_id, respond
from app.memory import ConversationMemory

bearer = HTTPBearer(auto_error=False)


def create_memory() -> ConversationMemory:
    encoded_key = os.environ.get("FIELD_ENCRYPTION_KEY", "")
    try:
        key = base64.b64decode(encoded_key, validate=True)
    except ValueError as error:
        raise RuntimeError("FIELD_ENCRYPTION_KEY must be base64 encoded") from error
    if len(key) != 32:
        raise RuntimeError("FIELD_ENCRYPTION_KEY must decode to exactly 32 bytes")
    redis = Redis.from_url(os.environ["REDIS_URL"], decode_responses=True)
    return ConversationMemory(redis, key)


async def get_backend_client() -> AsyncIterator[BackendClient]:
    if os.environ.get("AI_PROVIDER", "mock") != "mock":
        raise RuntimeError("Only the local mock AI provider is enabled without an approved BAA")
    base_url = os.environ.get("API_GATEWAY_URL", "http://api-gateway:8080")
    async with httpx.AsyncClient(timeout=8) as client:
        yield BackendClient(base_url, client)


def memory_from_request(request: Request) -> ConversationMemory:
    return request.app.state.conversation_memory


class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=2000)
    conversation_id: uuid.UUID | None = None


class ChatResponse(BaseModel):
    answer: str
    conversation_id: uuid.UUID
    role: str


@asynccontextmanager
async def lifespan(application: FastAPI):
    memory = create_memory()
    application.state.conversation_memory = memory
    try:
        yield
    finally:
        await memory.redis.aclose()


app = FastAPI(title="Role-aware Healthcare Assistant", version="1.0.0", lifespan=lifespan)


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/chat", response_model=ChatResponse)
async def chat(
    request: ChatRequest,
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer),
    backend: BackendClient = Depends(get_backend_client),
    memory: ConversationMemory = Depends(memory_from_request),
) -> ChatResponse:
    if credentials is None or credentials.scheme.lower() != "bearer":
        raise HTTPException(status_code=401, detail="A bearer access token is required")
    token = credentials.credentials
    identity = await backend.current_user(token)
    role = identity.get("role")
    if role not in {"PATIENT", "DOCTOR", "ADMIN"}:
        raise HTTPException(status_code=403, detail="Unsupported account role")

    current_conversation = conversation_id(request.conversation_id)
    history = await memory.history(identity["id"], current_conversation)
    question = request.message
    if history and any(word in question.casefold() for word in ("when", "what about it", "which one")):
        previous_user_message = next(
            (message["content"] for message in reversed(history) if message["role"] == "user"),
            "",
        )
        question = f"{previous_user_message} {question}"
    context = await backend.context_for(role, token, question)
    answer = respond(role, question, context)
    await memory.append(identity["id"], current_conversation, request.message, answer)
    return ChatResponse(answer=answer, conversation_id=current_conversation, role=role)
