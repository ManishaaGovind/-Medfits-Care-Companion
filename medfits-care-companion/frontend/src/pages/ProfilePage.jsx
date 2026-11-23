import { useEffect, useState } from "react";
import api from "../services/api";
import { useAuth } from "../context/AuthContext";

export function ProfilePage() {
  const { profile, setProfile } = useAuth();
  const [form, setForm] = useState({
    full_name: "",
    age_range: "25-34",
    preferred_language: "English",
    dietary_preference: "Vegetarian",
    wellness_goal: "",
    activity_level: "Moderately Active",
    preferred_daily_routine: "",
    general_preferences: "",
  });
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    if (profile) {
      setForm({
        full_name: profile.full_name || "",
        age_range: profile.age_range || "25-34",
        preferred_language: profile.preferred_language || "English",
        dietary_preference: profile.dietary_preference || "Vegetarian",
        wellness_goal: profile.wellness_goal || "",
        activity_level: profile.activity_level || "Moderately Active",
        preferred_daily_routine: profile.preferred_daily_routine || "",
        general_preferences: profile.general_preferences || "",
      });
    }
  }, [profile]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setMessage("");
    setError("");
    try {
      const res = await api.put("/api/profile", form);
      setProfile(res.data);
      setMessage("Profile saved. MedFits AI will personalize your guidance using these details.");
    } catch (err) {
      setError(err.response?.data?.detail || "Unable to save profile.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Personalized Wellness Profile</h1>
        <p className="text-sm text-slate-600 mt-1">
          Your preferences personalize AI chatbot responses and daily wellness recommendations.
        </p>
      </div>

      {message && <div className="p-3 rounded-xl bg-mintCare-50 text-mintCare-600 text-sm">{message}</div>}
      {error && <div className="p-3 rounded-xl bg-rose-50 text-rose-700 text-sm">{error}</div>}

      <form onSubmit={handleSubmit} className="bg-white rounded-2xl border border-slate-200 p-6 space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Full Name</label>
            <input
              type="text"
              required
              value={form.full_name}
              onChange={(e) => setForm({ ...form, full_name: e.target.value })}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Age / Age Range</label>
            <input
              type="text"
              required
              value={form.age_range}
              onChange={(e) => setForm({ ...form, age_range: e.target.value })}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
              placeholder="25-34"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Preferred Language</label>
            <input
              type="text"
              required
              value={form.preferred_language}
              onChange={(e) => setForm({ ...form, preferred_language: e.target.value })}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Dietary Preference</label>
            <select
              value={form.dietary_preference}
              onChange={(e) => setForm({ ...form, dietary_preference: e.target.value })}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm bg-white"
            >
              <option value="Vegetarian">Vegetarian</option>
              <option value="Non-vegetarian">Non-vegetarian</option>
              <option value="Vegan">Vegan</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Activity Level</label>
            <select
              value={form.activity_level}
              onChange={(e) => setForm({ ...form, activity_level: e.target.value })}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm bg-white"
            >
              <option value="Sedentary">Sedentary</option>
              <option value="Lightly Active">Lightly Active</option>
              <option value="Moderately Active">Moderately Active</option>
              <option value="Very Active">Very Active</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Wellness Goal</label>
            <input
              type="text"
              value={form.wellness_goal}
              onChange={(e) => setForm({ ...form, wellness_goal: e.target.value })}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
              placeholder="Improve sleep and daily hydration"
            />
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Preferred Daily Routine</label>
          <textarea
            rows={2}
            value={form.preferred_daily_routine}
            onChange={(e) => setForm({ ...form, preferred_daily_routine: e.target.value })}
            className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">General Wellness Preferences</label>
          <textarea
            rows={3}
            value={form.general_preferences}
            onChange={(e) => setForm({ ...form, general_preferences: e.target.value })}
            className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
          />
        </div>

        <button
          type="submit"
          disabled={saving}
          className="rounded-xl bg-tealCare-600 hover:bg-tealCare-700 text-white px-5 py-2.5 text-sm font-semibold"
        >
          {saving ? "Saving..." : "Save Profile"}
        </button>
      </form>
    </div>
  );
}
