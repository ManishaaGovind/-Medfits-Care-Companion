import { useEffect, useState } from "react";
import { FileText, Send, Upload } from "lucide-react";
import api from "../services/api";

export function ReportsPage() {
  const [reports, setReports] = useState([]);
  const [selected, setSelected] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [question, setQuestion] = useState("");
  const [qaList, setQaList] = useState([]);
  const [asking, setAsking] = useState(false);
  const [error, setError] = useState("");

  const loadReports = async () => {
    const res = await api.get("/api/reports");
    setReports(res.data || []);
  };

  useEffect(() => {
    loadReports().catch(() => {});
  }, []);

  const handleUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setError("");
    if (!file.name.toLowerCase().endsWith(".pdf")) {
      setError("Only PDF files (.pdf) are allowed.");
      return;
    }
    if (file.size > 8 * 1024 * 1024) {
      setError("Maximum allowed PDF file size is 8 MB.");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);
    setUploading(true);
    try {
      const res = await api.post("/api/reports/upload", formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      setSelected(res.data);
      setQaList([]);
      await loadReports();
    } catch (err) {
      setError(err.response?.data?.detail || "Failed to process PDF report.");
    } finally {
      setUploading(false);
    }
  };

  const handleAsk = async (e) => {
    e.preventDefault();
    if (!selected || !question.trim() || asking) return;
    setAsking(true);
    setError("");
    try {
      const res = await api.post(`/api/reports/${selected.id}/ask`, { question: question.trim() });
      setQaList((prev) => [...prev, res.data]);
      setQuestion("");
    } catch (err) {
      setError(err.response?.data?.detail || "Unable to answer question.");
    } finally {
      setAsking(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-2xl border border-slate-200 p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">PDF Health Report Analysis</h1>
          <p className="text-sm text-slate-600 mt-1">
            Upload a PDF health report (max 8 MB). Document text is separated from plain-language Gemini explanations.
          </p>
        </div>
        <label className="inline-flex items-center gap-2 rounded-xl bg-tealCare-600 hover:bg-tealCare-700 text-white px-4 py-2.5 text-sm font-semibold cursor-pointer shrink-0">
          <Upload className="h-4 w-4" />
          {uploading ? "Analyzing PDF..." : "Upload PDF (Max 8 MB)"}
          <input type="file" accept="application/pdf,.pdf" className="hidden" onChange={handleUpload} disabled={uploading} />
        </label>
      </div>

      {error && <div className="p-3 rounded-xl bg-rose-50 text-rose-700 text-sm">{error}</div>}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="bg-white rounded-2xl border border-slate-200 p-4 space-y-2">
          <h2 className="font-semibold text-slate-900 mb-2">Uploaded Reports</h2>
          {reports.length === 0 ? (
            <p className="text-sm text-slate-500">No reports uploaded yet.</p>
          ) : (
            reports.map((rep) => (
              <button
                key={rep.id}
                onClick={() => {
                  setSelected(rep);
                  setQaList([]);
                }}
                className={`w-full text-left p-3 rounded-xl border flex items-center gap-3 ${
                  selected?.id === rep.id
                    ? "bg-tealCare-50 border-tealCare-200"
                    : "bg-slate-50 border-transparent hover:bg-slate-100"
                }`}
              >
                <FileText className="h-5 w-5 text-tealCare-600 shrink-0" />
                <div className="min-w-0">
                  <p className="text-sm font-medium truncate">{rep.file_name}</p>
                  <p className="text-xs text-slate-500">
                    {rep.page_count} page(s) • {(rep.file_size_bytes / 1024).toFixed(0)} KB
                  </p>
                </div>
              </button>
            ))
          )}
        </div>

        <div className="lg:col-span-2 space-y-4">
          {!selected ? (
            <div className="bg-white rounded-2xl border border-slate-200 p-8 text-center text-sm text-slate-500">
              Select an uploaded PDF report or upload a new one to view extracted text and educational AI explanations.
            </div>
          ) : (
            <>
              <div className="bg-tealCare-50 border border-tealCare-100 rounded-2xl p-5">
                <h3 className="font-semibold text-tealCare-900 mb-2">Gemini Educational Explanation</h3>
                <p className="text-sm text-slate-800 whitespace-pre-wrap">{selected.ai_summary}</p>
              </div>

              <div className="bg-white border border-slate-200 rounded-2xl p-5">
                <h3 className="font-semibold text-slate-900 mb-2">Extracted Document Text (PyMuPDF)</h3>
                <pre className="text-xs text-slate-600 whitespace-pre-wrap max-h-60 overflow-y-auto bg-slate-50 p-3 rounded-xl">
                  {selected.extracted_text}
                </pre>
              </div>

              <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3">
                <h3 className="font-semibold text-slate-900">Ask Follow-Up Questions</h3>
                <form onSubmit={handleAsk} className="flex flex-col sm:flex-row gap-2">
                  <input
                    type="text"
                    value={question}
                    onChange={(e) => setQuestion(e.target.value)}
                    placeholder="Ask about any medical term or section in this report..."
                    className="flex-1 rounded-xl border border-slate-300 px-3 py-2 text-sm"
                  />
                  <button
                    type="submit"
                    disabled={asking || !question.trim()}
                    className="rounded-xl bg-tealCare-600 text-white px-4 py-2 text-sm font-semibold flex items-center justify-center gap-1.5 disabled:opacity-50"
                  >
                    <Send className="h-4 w-4" /> Ask
                  </button>
                </form>
                {qaList.map((qa, idx) => (
                  <div key={idx} className="p-3 rounded-xl bg-slate-50 space-y-1">
                    <p className="text-xs font-semibold text-tealCare-700">Q: {qa.question}</p>
                    <p className="text-sm text-slate-800 whitespace-pre-wrap">{qa.answer}</p>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
