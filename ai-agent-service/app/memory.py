import base64
import json
import os
import uuid
import hashlib

from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from redis.asyncio import Redis


class ConversationMemory:
    def __init__(self, redis: Redis, key: bytes) -> None:
        self.redis = redis
        self.cipher = AESGCM(key)

    async def history(self, user_id: str, conversation_id: uuid.UUID) -> list[dict[str, str]]:
        user_key = hashlib.sha256(user_id.encode()).hexdigest()
        stored = await self.redis.lrange(f"chat:{user_key}:{conversation_id}", -6, -1)
        history = []
        for value in stored:
            encrypted = base64.b64decode(value)
            plaintext = self.cipher.decrypt(encrypted[:12], encrypted[12:], None)
            history.append(json.loads(plaintext))
        return history

    async def append(self, user_id: str, conversation_id: uuid.UUID,
                     user_message: str, assistant_message: str) -> None:
        user_key = hashlib.sha256(user_id.encode()).hexdigest()
        key = f"chat:{user_key}:{conversation_id}"
        for item in ({"role": "user", "content": user_message},
                     {"role": "assistant", "content": assistant_message}):
            nonce = os.urandom(12)
            plaintext = json.dumps(item, separators=(",", ":")).encode()
            ciphertext = self.cipher.encrypt(nonce, plaintext, None)
            await self.redis.rpush(key, base64.b64encode(nonce + ciphertext).decode())
        await self.redis.expire(key, 1800)
