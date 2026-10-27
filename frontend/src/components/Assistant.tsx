import { useState, type FormEvent } from "react";
import { useAuth } from "../auth/AuthContext";

interface Message {
  role: "user" | "assistant";
  content: string;
}

interface ChatResponse {
  answer: string;
  conversation_id: string;
  role: string;
}

export function Assistant() {
  const { api, session } = useAuth();
  const [open, setOpen] = useState(false);
  const [text, setText] = useState("");
  const [conversationId, setConversationId] = useState<string>();
  const [messages, setMessages] = useState<Message[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function send(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const message = text.trim();
    if (!message || busy) return;
    setText("");
    setError("");
    setMessages((current) => [...current, { role: "user", content: message }]);
    setBusy(true);
    try {
      const response = await api<ChatResponse>("/ai/chat", {
        method: "POST",
        body: JSON.stringify({ message, conversation_id: conversationId ?? null }),
      });
      setConversationId(response.conversation_id);
      setMessages((current) => [...current, { role: "assistant", content: response.answer }]);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "The assistant is unavailable.");
    } finally {
      setBusy(false);
    }
  }

  if (!session) return null;
  return (
    <div className="assistant-widget">
      {open && <section className="assistant-panel" aria-label="Care assistant">
        <header className="assistant-header">
          <div className="assistant-avatar">✳</div>
          <div><strong>Care assistant</strong><span>Secure · {session.role.toLowerCase()} view</span></div>
          <button className="icon-button" aria-label="Close assistant" onClick={() => setOpen(false)}>×</button>
        </header>
        <div className="assistant-messages" aria-live="polite">
          {messages.length === 0 && <div className="assistant-welcome">
            <span className="assistant-avatar large">✳</span>
            <strong>How can I help?</strong>
            <p>I can help with information available to your account.</p>
          </div>}
          {messages.map((message, index) => (
            <div className={`chat-message ${message.role}`} key={`${index}-${message.role}`}>
              {message.content}
            </div>
          ))}
          {busy && <div className="chat-message assistant">Checking your secure information...</div>}
          {error && <p className="form-message" role="alert">{error}</p>}
        </div>
        <form className="assistant-input" onSubmit={(event) => void send(event)}>
          <input value={text} onChange={(event) => setText(event.target.value)}
            placeholder="Ask about your care..." aria-label="Message the assistant" maxLength={2000} />
          <button className="send-button" disabled={!text.trim() || busy} aria-label="Send message">↑</button>
        </form>
        <p className="assistant-disclaimer">Not for medical advice or emergencies.</p>
      </section>}
      <button className="assistant-launcher" onClick={() => setOpen(!open)}
        aria-label={open ? "Close care assistant" : "Open care assistant"}>
        {open ? "×" : "✳"}
      </button>
    </div>
  );
}
