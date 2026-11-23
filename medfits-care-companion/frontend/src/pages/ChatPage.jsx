import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Plus, Send, ShieldAlert, Trash2 } from "lucide-react";
import api from "../services/api";
import { WELLNESS_CATEGORIES } from "../services/categories";

export function ChatPage() {
  const [searchParams] = useSearchParams();
  const initialCat = searchParams.get("category") || WELLNESS_CATEGORIES[0].title;
  const initialSession = searchParams.get("session");

  const [category, setCategory] = useState(initialCat);
  const [sessions, setSessions] = useState([]);
  const [activeSessionId, setActiveSessionId] = useState(initialSession ? Number(initialSession) : null);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const [error, setError] = useState("");

  const loadSessions = async () => {
    const res = await api.get("/api/chat/sessions");
    setSessions(res.data || []);
  };

  useEffect(() => {
    loadSessions().catch(() => {});
  }, []);

  useEffect(() => {
    if (!activeSessionId) {
      setMessages([]);
      return;
    }
    api
      .get(`/api/chat/sessions/${activeSessionId}`)
      .then((res) => {
        setMessages(res.data.messages || []);
        setCategory(res.data.category);
      })
      .catch(() => setError("Unable to load selected conversation."));
  }, [activeSessionId]);

  const handleSend = async (textOverride) => {
    const text = (textOverride ?? input).trim();
    if (!text || sending) return;
    setError("");
    setSending(true);
    if (!textOverride) setInput("");

    try {
      const res = await api.post("/api/chat", {
        session_id: activeSessionId,
        category,
        message: text,
      });
      setActiveSessionId(res.data.session_id);
      setMessages(res.data.messages || []);
      await loadSessions();
    } catch (err) {
      setError(err.response?.data?.detail || "Failed to get AI response.");
    } finally {
      setSending(false);
    }
  };

  const handleDeleteSession = async (id, e) => {
    e.stopPropagation();
    await api.delete(`/api/chat/sessions/${id}`);
    if (activeSessionId === id) {
      setActiveSessionId(null);
      setMessages([]);
    }
    await loadSessions();
  };

  const activeCategoryObj = WELLNESS_CATEGORIES.find((c) => c.title === category);

  return (
    <div className="grid grid-cols-1 lg:grid-cols-4 gap-5 min-h-[calc(100vh-8rem)] lg:h-[calc(100vh-8rem)]">
      {/* Sessions Sidebar */}
      <div className="bg-white rounded-2xl border border-slate-200 p-4 flex flex-col max-h-60 lg:max-h-none overflow-hidden">
        <button
          onClick={() => {
            setActiveSessionId(null);
            setMessages([]);
          }}
          className="w-full flex items-center justify-center gap-2 rounded-xl bg-tealCare-600 hover:bg-tealCare-700 text-white py-2.5 text-sm font-semibold mb-4"
        >
          <Plus className="h-4 w-4" /> New Conversation
        </button>

        <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">History</h3>
        <div className="flex-1 overflow-y-auto space-y-2">
          {sessions.length === 0 ? (
            <p className="text-xs text-slate-500">No saved conversations yet.</p>
          ) : (
            sessions.map((s) => (
              <div
                key={s.id}
                onClick={() => setActiveSessionId(s.id)}
                className={`p-3 rounded-xl cursor-pointer flex items-center justify-between gap-2 text-sm border ${
                  activeSessionId === s.id
                    ? "bg-tealCare-50 border-tealCare-200 text-tealCare-900"
                    : "bg-slate-50 border-transparent hover:bg-slate-100"
                }`}
              >
                <div className="min-w-0">
                  <p className="font-medium truncate">{s.title}</p>
                  <p className="text-xs text-slate-500 truncate">{s.category}</p>
                </div>
                <button
                  onClick={(e) => handleDeleteSession(s.id, e)}
                  className="text-slate-400 hover:text-rose-600 p-1"
                  aria-label="Delete conversation"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </button>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Chat Pane */}
      <div className="lg:col-span-3 bg-white rounded-2xl border border-slate-200 flex flex-col min-h-[65vh] lg:min-h-0 overflow-hidden">
        <div className="p-4 border-b border-slate-100 space-y-2">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h1 className="text-lg font-bold text-slate-900">MedFits AI Wellness Guide</h1>
              <p className="text-xs text-slate-500">Educational wellness support — never diagnoses or prescribes.</p>
            </div>
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              className="rounded-xl border border-slate-300 px-3 py-1.5 text-sm bg-white focus:border-tealCare-600 focus:outline-none"
            >
              <option value="General Wellness">Free-Form Wellness Question</option>
              {WELLNESS_CATEGORIES.map((c) => (
                <option key={c.id} value={c.title}>
                  {c.title}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-[#f8fcfb]">
          {messages.length === 0 && (
            <div className="bg-white rounded-2xl border border-tealCare-100 p-5 space-y-3">
              <p className="font-semibold text-slate-900">{category}</p>
              <p className="text-sm text-slate-600">
                {activeCategoryObj?.description || "Ask any everyday healthcare education or lifestyle question."}
              </p>
              {activeCategoryObj && (
                <button
                  onClick={() => handleSend(activeCategoryObj.prompt)}
                  className="text-xs font-semibold text-tealCare-700 bg-tealCare-50 hover:bg-tealCare-100 px-3 py-2 rounded-xl transition"
                >
                  Try starter prompt: "{activeCategoryObj.prompt}"
                </button>
              )}
            </div>
          )}

          {messages.map((m) => (
            <div key={m.id} className={`flex ${m.role === "user" ? "justify-end" : "justify-start"}`}>
              <div
                className={`max-w-[80%] rounded-2xl px-4 py-3 text-sm whitespace-pre-wrap ${
                  m.role === "user"
                    ? "bg-tealCare-600 text-white rounded-br-sm"
                    : "bg-white border border-slate-200 text-slate-800 rounded-bl-sm"
                }`}
              >
                {m.content}
              </div>
            </div>
          ))}

          {sending && (
            <div className="text-xs text-tealCare-700 font-medium">MedFits AI is preparing a personalized answer...</div>
          )}
        </div>

        {error && (
          <div className="px-4 py-2 bg-rose-50 border-t border-rose-200 text-rose-700 text-xs flex items-center gap-2">
            <ShieldAlert className="h-4 w-4" /> {error}
          </div>
        )}

        <form
          onSubmit={(e) => {
            e.preventDefault();
            handleSend();
          }}
          className="p-3 border-t border-slate-100 flex items-center gap-2"
        >
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Ask a personalized wellness or lifestyle question..."
            className="flex-1 rounded-xl border border-slate-300 px-4 py-2.5 text-sm focus:border-tealCare-600 focus:outline-none"
          />
          <button
            type="submit"
            disabled={sending || !input.trim()}
            className="rounded-xl bg-tealCare-600 hover:bg-tealCare-700 text-white px-4 py-2.5 text-sm font-semibold flex items-center gap-1.5 disabled:opacity-50"
          >
            <Send className="h-4 w-4" /> Send
          </button>
        </form>
      </div>
    </div>
  );
}
