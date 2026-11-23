import { useEffect, useState } from "react";
import api from "../services/api";
import { WELLNESS_CATEGORIES } from "../services/categories";

export function SpecialistPage() {
  const [requests, setRequests] = useState([]);
  const [category, setCategory] = useState(WELLNESS_CATEGORIES[0].title);
  const [concern, setConcern] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const loadRequests = async () => {
    const res = await api.get("/api/specialist/requests");
    setRequests(res.data || []);
  };

  useEffect(() => {
    loadRequests().catch(() => {});
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError("");
    setMessage("");
    try {
      await api.post("/api/specialist/requests", { category, concern });
      setConcern("");
      setMessage("Specialist support request submitted. Our wellness team will review it.");
      await loadRequests();
    } catch (err) {
      setError(err.response?.data?.detail || "Failed to submit support request.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Specialist Support</h1>
        <p className="text-sm text-slate-600 mt-1">
          Submit a wellness support inquiry. Note: this creates a support ticket for educational review and does not book a clinical appointment.
        </p>
      </div>

      {message && <div className="p-3 rounded-xl bg-mintCare-50 text-mintCare-600 text-sm">{message}</div>}
      {error && <div className="p-3 rounded-xl bg-rose-50 text-rose-700 text-sm">{error}</div>}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <form onSubmit={handleSubmit} className="bg-white rounded-2xl border border-slate-200 p-5 space-y-4">
          <h2 className="font-semibold text-slate-900">New Support Inquiry</h2>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Support Category</label>
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm bg-white"
            >
              {WELLNESS_CATEGORIES.map((c) => (
                <option key={c.id} value={c.title}>
                  {c.title}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Describe Your Concern</label>
            <textarea
              required
              minLength={5}
              rows={4}
              value={concern}
              onChange={(e) => setConcern(e.target.value)}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
              placeholder="Describe what guidance or clarification you are looking for..."
            />
          </div>
          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-xl bg-tealCare-600 hover:bg-tealCare-700 text-white py-2.5 text-sm font-semibold"
          >
            {submitting ? "Submitting..." : "Submit Request"}
          </button>
        </form>

        <div className="bg-white rounded-2xl border border-slate-200 p-5 space-y-3">
          <h2 className="font-semibold text-slate-900">Your Support Requests</h2>
          {requests.length === 0 ? (
            <p className="text-sm text-slate-500">No support requests submitted yet.</p>
          ) : (
            requests.map((r) => (
              <div key={r.id} className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 space-y-1.5">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-semibold text-slate-800">{r.category}</span>
                  <span className="text-xs font-semibold uppercase px-2 py-0.5 rounded-full bg-tealCare-100 text-tealCare-700">
                    {r.status}
                  </span>
                </div>
                <p className="text-xs text-slate-600">{r.concern}</p>
                {r.admin_notes && (
                  <p className="text-xs text-tealCare-800 bg-tealCare-50 p-2 rounded-lg">
                    Specialist Note: {r.admin_notes}
                  </p>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
