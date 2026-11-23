import { useEffect, useState } from "react";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { Line, LineChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import api from "../services/api";
import { DEFAULT_HABITS } from "../services/categories";

export function WellnessPage() {
  const [records, setRecords] = useState([]);
  const [goals, setGoals] = useState({
    water_target_ml: 2500,
    sleep_target_hours: 8,
    exercise_target_minutes: 30,
    daily_habits_target: 5,
  });
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState({
    record_date: new Date().toISOString().slice(0, 10),
    water_ml: 2000,
    sleep_hours: 7.5,
    physical_activity: "Brisk Walking",
    exercise_minutes: 30,
    habits_completed: ["Morning Hydration"],
    notes: "",
  });
  const [statusMsg, setStatusMsg] = useState("");
  const [error, setError] = useState("");

  const loadData = async () => {
    const [recRes, goalRes] = await Promise.all([
      api.get("/api/wellness"),
      api.get("/api/wellness/goals/current"),
    ]);
    setRecords(recRes.data || []);
    if (goalRes.data) setGoals(goalRes.data);
  };

  useEffect(() => {
    loadData().catch(() => setError("Unable to load wellness records."));
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setStatusMsg("");
    try {
      if (editingId) {
        await api.put(`/api/wellness/${editingId}`, form);
        setStatusMsg("Wellness entry updated.");
      } else {
        await api.post("/api/wellness", form);
        setStatusMsg("Wellness entry saved.");
      }
      setEditingId(null);
      await loadData();
    } catch (err) {
      setError(err.response?.data?.detail || "Failed to save wellness entry.");
    }
  };

  const handleSaveGoals = async (e) => {
    e.preventDefault();
    await api.put("/api/wellness/goals/current", goals);
    setStatusMsg("Personal wellness goals updated.");
  };

  const toggleHabit = (habit) => {
    setForm((prev) => {
      const exists = prev.habits_completed.includes(habit);
      return {
        ...prev,
        habits_completed: exists
          ? prev.habits_completed.filter((h) => h !== habit)
          : [...prev.habits_completed, habit],
      };
    });
  };

  const chartData = [...records]
    .slice(0, 7)
    .reverse()
    .map((r) => ({
      date: r.record_date.slice(5),
      Water_ml: r.water_ml,
      Sleep_hrs: r.sleep_hours,
      Exercise_min: r.exercise_minutes,
    }));

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-slate-900">Daily Wellness Tracker</h1>

      {statusMsg && <div className="p-3 rounded-xl bg-mintCare-50 text-mintCare-600 text-sm">{statusMsg}</div>}
      {error && <div className="p-3 rounded-xl bg-rose-50 text-rose-700 text-sm">{error}</div>}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <form onSubmit={handleSubmit} className="bg-white rounded-2xl border border-slate-200 p-5 space-y-4">
          <h2 className="font-semibold text-slate-900">{editingId ? "Edit Daily Entry" : "Add Daily Entry"}</h2>
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Date</label>
            <input
              type="date"
              required
              value={form.record_date}
              onChange={(e) => setForm({ ...form, record_date: e.target.value })}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Water (ml)</label>
              <input
                type="number"
                min={0}
                max={10000}
                value={form.water_ml}
                onChange={(e) => setForm({ ...form, water_ml: Number(e.target.value) })}
                className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Sleep (hrs)</label>
              <input
                type="number"
                step="0.5"
                min={0}
                max={24}
                value={form.sleep_hours}
                onChange={(e) => setForm({ ...form, sleep_hours: Number(e.target.value) })}
                className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Activity</label>
              <input
                type="text"
                value={form.physical_activity}
                onChange={(e) => setForm({ ...form, physical_activity: e.target.value })}
                className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Minutes</label>
              <input
                type="number"
                min={0}
                max={600}
                value={form.exercise_minutes}
                onChange={(e) => setForm({ ...form, exercise_minutes: Number(e.target.value) })}
                className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1.5">Daily Habits</label>
            <div className="flex flex-wrap gap-1.5">
              {DEFAULT_HABITS.map((h) => {
                const active = form.habits_completed.includes(h);
                return (
                  <button
                    key={h}
                    type="button"
                    onClick={() => toggleHabit(h)}
                    className={`px-2.5 py-1 rounded-full text-xs font-medium border ${
                      active
                        ? "bg-tealCare-600 text-white border-tealCare-600"
                        : "bg-slate-50 text-slate-600 border-slate-200"
                    }`}
                  >
                    {h}
                  </button>
                );
              })}
            </div>
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Notes</label>
            <textarea
              rows={2}
              value={form.notes}
              onChange={(e) => setForm({ ...form, notes: e.target.value })}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
            />
          </div>
          <button
            type="submit"
            className="w-full rounded-xl bg-tealCare-600 hover:bg-tealCare-700 text-white py-2.5 text-sm font-semibold flex items-center justify-center gap-1.5"
          >
            <Plus className="h-4 w-4" /> {editingId ? "Update Entry" : "Save Entry"}
          </button>
        </form>

        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white rounded-2xl border border-slate-200 p-5">
            <h2 className="font-semibold text-slate-900 mb-4">7-Day Wellness Trend</h2>
            {chartData.length === 0 ? (
              <p className="text-sm text-slate-500">Add entries to see your 7-day trends.</p>
            ) : (
              <div className="h-56">
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                    <XAxis dataKey="date" />
                    <YAxis />
                    <Tooltip />
                    <Legend />
                    <Line type="monotone" dataKey="Exercise_min" name="Exercise (min)" stroke="#0d9488" strokeWidth={2} />
                    <Line type="monotone" dataKey="Sleep_hrs" name="Sleep (hrs)" stroke="#10b981" strokeWidth={2} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            )}
          </div>

          <form onSubmit={handleSaveGoals} className="bg-white rounded-2xl border border-slate-200 p-5">
            <h2 className="font-semibold text-slate-900 mb-3">Personal Wellness Goals</h2>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div>
                <label className="text-xs text-slate-600">Water (ml)</label>
                <input
                  type="number"
                  value={goals.water_target_ml}
                  onChange={(e) => setGoals({ ...goals, water_target_ml: Number(e.target.value) })}
                  className="w-full rounded-xl border border-slate-300 px-2.5 py-1.5 text-sm mt-1"
                />
              </div>
              <div>
                <label className="text-xs text-slate-600">Sleep (hrs)</label>
                <input
                  type="number"
                  step="0.5"
                  value={goals.sleep_target_hours}
                  onChange={(e) => setGoals({ ...goals, sleep_target_hours: Number(e.target.value) })}
                  className="w-full rounded-xl border border-slate-300 px-2.5 py-1.5 text-sm mt-1"
                />
              </div>
              <div>
                <label className="text-xs text-slate-600">Exercise (min)</label>
                <input
                  type="number"
                  value={goals.exercise_target_minutes}
                  onChange={(e) => setGoals({ ...goals, exercise_target_minutes: Number(e.target.value) })}
                  className="w-full rounded-xl border border-slate-300 px-2.5 py-1.5 text-sm mt-1"
                />
              </div>
              <div className="flex items-end">
                <button
                  type="submit"
                  className="w-full rounded-xl bg-mintCare-600 hover:bg-mintCare-500 text-white py-2 text-xs font-semibold"
                >
                  Save Goals
                </button>
              </div>
            </div>
          </form>

          <div className="bg-white rounded-2xl border border-slate-200 p-5 space-y-3">
            <h2 className="font-semibold text-slate-900">Historical Records</h2>
            {records.length === 0 ? (
              <p className="text-sm text-slate-500">No entries recorded yet.</p>
            ) : (
              records.map((r) => (
                <div key={r.id} className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 flex items-center justify-between gap-3">
                  <div>
                    <p className="text-sm font-semibold text-tealCare-700">{r.record_date}</p>
                    <p className="text-xs text-slate-700">
                      Water: {r.water_ml} ml • Sleep: {r.sleep_hours} hrs • {r.physical_activity} ({r.exercise_minutes} min)
                    </p>
                    {r.habits_completed.length > 0 && (
                      <p className="text-xs text-mintCare-600 mt-0.5">Habits: {r.habits_completed.join(", ")}</p>
                    )}
                  </div>
                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => {
                        setEditingId(r.id);
                        setForm({
                          record_date: r.record_date,
                          water_ml: r.water_ml,
                          sleep_hours: r.sleep_hours,
                          physical_activity: r.physical_activity,
                          exercise_minutes: r.exercise_minutes,
                          habits_completed: r.habits_completed,
                          notes: r.notes,
                        });
                      }}
                      className="p-2 text-slate-500 hover:text-tealCare-700"
                      aria-label="Edit entry"
                    >
                      <Pencil className="h-4 w-4" />
                    </button>
                    <button
                      onClick={async () => {
                        await api.delete(`/api/wellness/${r.id}`);
                        await loadData();
                      }}
                      className="p-2 text-slate-500 hover:text-rose-600"
                      aria-label="Delete entry"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
