import { useEffect, useState } from "react";
import { Trash2 } from "lucide-react";
import api from "../services/api";

export function AdminPage() {
  const [dashboard, setDashboard] = useState(null);
  const [users, setUsers] = useState([]);
  const [requests, setRequests] = useState([]);
  const [resources, setResources] = useState([]);
  const [newResource, setNewResource] = useState({ title: "", category: "Preventive Healthcare", summary: "" });
  const [error, setError] = useState("");

  const loadAll = async () => {
    try {
      const [dRes, uRes, rRes, resRes] = await Promise.all([
        api.get("/api/admin/dashboard"),
        api.get("/api/admin/users"),
        api.get("/api/admin/requests"),
        api.get("/api/admin/resources"),
      ]);
      setDashboard(dRes.data);
      setUsers(uRes.data || []);
      setRequests(rRes.data || []);
      setResources(resRes.data || []);
    } catch (err) {
      setError(err.response?.data?.detail || "Admin dashboard access error.");
    }
  };

  useEffect(() => {
    loadAll();
  }, []);

  const updateRequest = async (id, status, admin_notes) => {
    await api.put(`/api/admin/requests/${id}`, { status, admin_notes });
    await loadAll();
  };

  const handleCreateResource = async (e) => {
    e.preventDefault();
    await api.post("/api/admin/resources", newResource);
    setNewResource({ title: "", category: "Preventive Healthcare", summary: "" });
    await loadAll();
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-slate-900">Admin Dashboard</h1>
      {error && <div className="p-3 rounded-xl bg-rose-50 text-rose-700 text-sm">{error}</div>}

      {dashboard && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <MetricBox label="Total Registered Users" value={dashboard.total_users} />
          <MetricBox label="Total Conversations" value={dashboard.total_conversations} />
          <MetricBox label="Total Wellness Records" value={dashboard.total_wellness_records} />
          <MetricBox label="Specialist Requests" value={dashboard.total_specialist_requests} />
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-2xl border border-slate-200 p-5 space-y-3">
          <h2 className="font-semibold text-slate-900">Specialist Support Queue</h2>
          {requests.map((req) => (
            <AdminRequestCard key={req.id} req={req} onUpdate={updateRequest} />
          ))}
        </div>

        <div className="space-y-6">
          <div className="bg-white rounded-2xl border border-slate-200 p-5 space-y-3">
            <h2 className="font-semibold text-slate-900">Wellness Resource Management</h2>
            <form onSubmit={handleCreateResource} className="space-y-2.5">
              <input
                type="text"
                required
                placeholder="Resource Title"
                value={newResource.title}
                onChange={(e) => setNewResource({ ...newResource, title: e.target.value })}
                className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
              />
              <input
                type="text"
                required
                placeholder="Category"
                value={newResource.category}
                onChange={(e) => setNewResource({ ...newResource, category: e.target.value })}
                className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
              />
              <textarea
                required
                rows={2}
                placeholder="Educational Summary"
                value={newResource.summary}
                onChange={(e) => setNewResource({ ...newResource, summary: e.target.value })}
                className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
              />
              <button className="rounded-xl bg-tealCare-600 text-white px-4 py-2 text-sm font-semibold">
                Publish Resource
              </button>
            </form>
            <div className="space-y-2 pt-2">
              {resources.map((res) => (
                <div key={res.id} className="p-3 rounded-xl bg-slate-50 flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium">{res.title}</p>
                    <p className="text-xs text-tealCare-700">{res.category}</p>
                  </div>
                  <button
                    onClick={async () => {
                      await api.delete(`/api/admin/resources/${res.id}`);
                      await loadAll();
                    }}
                    className="text-rose-600 p-1"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              ))}
            </div>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200 p-5 space-y-2">
            <h2 className="font-semibold text-slate-900">Recent Registered Users</h2>
            {users.slice(0, 8).map((u) => (
              <div key={u.id} className="text-xs text-slate-700 flex justify-between py-1.5 border-b border-slate-100">
                <span>{u.email}</span>
                <span className="font-medium text-tealCare-700">{u.is_admin ? "Admin" : "Member"}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

function MetricBox({ label, value }) {
  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-4">
      <p className="text-xs text-slate-500">{label}</p>
      <p className="text-2xl font-bold text-tealCare-700 mt-1">{value}</p>
    </div>
  );
}

function AdminRequestCard({ req, onUpdate }) {
  const [notes, setNotes] = useState(req.admin_notes || "");
  return (
    <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 space-y-2">
      <div className="flex justify-between items-center">
        <span className="text-sm font-semibold">{req.category}</span>
        <span className="text-xs font-semibold uppercase text-tealCare-700">{req.status}</span>
      </div>
      <p className="text-xs text-slate-600">{req.concern}</p>
      <input
        type="text"
        value={notes}
        onChange={(e) => setNotes(e.target.value)}
        placeholder="Add admin response note"
        className="w-full rounded-lg border border-slate-300 px-2.5 py-1.5 text-xs"
      />
      <div className="flex gap-2">
        <button
          onClick={() => onUpdate(req.id, "in_review", notes)}
          className="px-3 py-1 rounded-lg bg-amber-100 text-amber-800 text-xs font-semibold"
        >
          In Review
        </button>
        <button
          onClick={() => onUpdate(req.id, "resolved", notes)}
          className="px-3 py-1 rounded-lg bg-mintCare-100 text-mintCare-600 text-xs font-semibold"
        >
          Resolve
        </button>
      </div>
    </div>
  );
}
