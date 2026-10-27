import asyncio
import uuid

from app.memory import ConversationMemory


class FakeRedis:
    def __init__(self):
        self.values = {}

    async def rpush(self, key, value):
        self.values.setdefault(key, []).append(value)

    async def expire(self, key, seconds):
        return True

    async def lrange(self, key, start, end):
        return self.values.get(key, [])[start:]


def test_conversation_messages_are_encrypted_in_redis():
    async def run():
        redis = FakeRedis()
        memory = ConversationMemory(redis, b"0" * 32)
        conversation_id = uuid.uuid4()

        await memory.append("patient-id", conversation_id, "my health detail", "your answer")
        stored = next(iter(redis.values.values()))
        assert all("my health detail" not in item and "your answer" not in item for item in stored)
        history = await memory.history("patient-id", conversation_id)

        assert history == [
            {"role": "user", "content": "my health detail"},
            {"role": "assistant", "content": "your answer"},
        ]

    asyncio.run(run())
