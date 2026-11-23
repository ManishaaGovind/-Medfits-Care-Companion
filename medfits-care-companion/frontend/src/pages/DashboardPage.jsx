import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Activity, ArrowRight, CheckCircle2, Droplets, MessageSquare, Moon } from "lucide-react";
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import api from "../services/api";
import { DEFAULT_HABITS, WELLNESS_CATEGORIES } from "../services/categories";
import { useAuth } from "../context/AuthContext";

export function DashboardPage() {
  const { profile } = useAuth();
  const navigate = useNavigate();
  const [records, setRecords] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [goals, setGoals] = useState({
    water_target_ml: 2500,
    sleep_target_hours: 8,
    exercise_target_minutes: 30,
    daily_habits_target: 5,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      api.get("/api/wellness").catch(() => ({ data: [] })),
      api.get("/api/chat/sessions").catch(() => ({ data: [] })),
      api.get("/api/wellness/goals/current").catch(() => ({ data: null })),
    ])
      .then(([wRes, sRes, gRes]) => {
        setRecords(wRes.data || []);
        setSessions(sRes.data || []);
        if (gRes.data) setGoals(gRes.data);
      })
      .finally(() => setLoading(false));
  }, []);

  const latest = records[0];
  const chartData = [...records]
    .slice(0, 7)
    .reverse()
    .map((r) => ({
      date: r.record_date.slice(5),
      Water_L: Number((r.water_ml / 1000).toFixed(2)),
      Sleep_Hrs: r.sleep_hours,
      Active_Min: r.exercise_minutes,
    }));

  return (
    <div className="space-y-6">
      <div className="bg-gradient-to-r from-tealCare-600 to-mintCare-600 rounded-2xl p-6 text-white shadow-sm">
        <h1 className="text-2xl font-bold">Welcome back, {profile?.full_name || "Friend"}</h1>
        <p className="text-tealCare-50 text-sm mt-1">
          {profile?.wellness_goal
            ? `Current focus: ${profile.wellness_goal} • ${profile.dietary_preference} • ${profile.activity_level}`
            : "Complete your wellness profile to unlock tailored AI lifestyle guidance."}
        </p>
      </div>

      {/* Today's Wellness Overview */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <StatCard
          icon={Droplets}
          label="Daily Water Intake"
          value={`${latest?.water_ml ?? 0} ml`}
          target={`Goal: ${goals.water_target_ml} ml`}
        />
        <StatCard
          icon={Moon}
          label="Sleep Tracking"
          value={`${latest?.sleep_hours ?? 0} hrs`}
          target={`Goal: ${goals.sleep_target_hours} hrs`}
        />
        <StatCard
          icon={Activity}
          label="Physical Activity"
          value={`${latest?.exercise_minutes ?? 0} min`}
          target={latest?.physical_activity || `Goal: ${goals.exercise_target_minutes} min`}
        />
        <StatCard
          icon={CheckCircle2}
          label="Healthy Habits"
          value={`${latest?.habits_completed?.length ?? 0} / ${DEFAULT_HABITS.length}`}
          target={`Target: ${goals.daily_habits_target} habits`}
        />
      </div>

      {/* Weekly Chart + Recent Conversations */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-white rounded-2xl border border-slate-200 p-5 shadow-sm">
          <h2 className="text-lg font-semibold text-slate-900 mb-4">Weekly Wellness Progress</h2>
          {loading ? (
            <p className="text-sm text-slate-500">Loading chart data...</p>
          ) : chartData.length === 0 ? (
            <div className="py-12 text-center text-sm text-slate-500">
              No wellness records yet. Log your first daily entry in the Wellness Tracker.
            </div>
          ) : (
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                  <XAxis dataKey="date" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="Active_Min" name="Exercise (min)" fill="#0d9488" radius={[6, 6, 0, 0]} />
                  <Bar dataKey="Sleep_Hrs" name="Sleep (hrs)" fill="#10b981" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-slate-900">Recent AI Chats</h2>
            <button
              onClick={() => navigate("/chat")}
              className="text-xs font-semibold text-tealCare-700 hover:underline"
            >
              Open Chat
            </button>
          </div>
          {sessions.length === 0 ? (
            <p className="text-sm text-slate-500">No saved conversations yet. Select any category below to begin.</p>
          ) : (
            <div className="space-y-2.5">
              {sessions.slice(0, 5).map((s) => (
                <button
                  key={s.id}
                  onClick={() => navigate(`/chat?session=${s.id}`)}
                  className="w-full text-left p-3 rounded-xl bg-slate-50 hover:bg-tealCare-50 border border-slate-100 transition flex items-center justify-between gap-2"
                >
                  <div className="min-w-0">
                    <p className="text-xs font-semibold text-tealCare-700">{s.category}</p>
                    <p className="text-sm font-medium text-slate-800 truncate">{s.title}</p>
                  </div>
                  <MessageSquare className="h-4 w-4 text-tealCare-600 shrink-0" />
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Quick Access to all 11 Categories */}
      <div>
        <h2 className="text-lg font-semibold text-slate-900 mb-3">11 Core Wellness AI Categories</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {WELLNESS_CATEGORIES.map((cat) => (
            <button
              key={cat.id}
              onClick={() => navigate(`/chat?category=${encodeURIComponent(cat.title)}`)}
              className="text-left bg-white rounded-2xl border border-slate-200 p-4 hover:border-tealCare-500 hover:shadow-md transition flex flex-col justify-between"
            >
              <div>
                <h3 className="font-semibold text-slate-900 text-sm">{cat.title}</h3>
                <p className="text-xs text-slate-500 mt-1">{cat.description}</p>
              </div>
              <span className="mt-3 inline-flex items-center gap-1 text-xs font-semibold text-tealCare-700">
                Ask AI <ArrowRight className="h-3.5 w-3.5" />
              </span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

function StatCard({ icon: Icon, label, value, target }) {
  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-4 shadow-sm">
      <div className="flex items-center justify-between mb-2">
        <span className="text-xs font-medium text-slate-500">{label}</span>
        <div className="h-8 w-8 rounded-lg bg-tealCare-50 text-tealCare-600 flex items-center justify-center">
          <Icon className="h-4 w-4" />
        </div>
      </div>
      <p className="text-2xl font-bold text-slate-900">{value}</p>
      <p className="text-xs text-slate-500 mt-0.5">{target}</p>
    </div>
  );
}
