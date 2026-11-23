import { useState } from "react";
import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import {
  Activity,
  FileText,
  HeartPulse,
  LayoutDashboard,
  LogOut,
  Menu,
  MessageSquareHeart,
  ShieldCheck,
  Stethoscope,
  UserCircle,
  X,
} from "lucide-react";
import { useAuth } from "../context/AuthContext";

export function DashboardLayout() {
  const { user, profile, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);

  const navItems = [
    { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
    { to: "/chat", label: "AI Assistant", icon: MessageSquareHeart },
    { to: "/wellness", label: "Wellness Tracker", icon: Activity },
    { to: "/reports", label: "PDF Reports", icon: FileText },
    { to: "/specialist", label: "Specialist Support", icon: Stethoscope },
    { to: "/profile", label: "My Profile", icon: UserCircle },
  ];

  if (user?.is_admin) {
    navItems.push({ to: "/admin", label: "Admin Panel", icon: ShieldCheck });
  }

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <div className="min-h-screen flex bg-[#f8fcfb] text-slate-900">
      {/* Desktop Sidebar */}
      <aside className="hidden lg:flex lg:flex-col lg:w-64 bg-white border-r border-slate-200 px-4 py-6">
        <Link to="/dashboard" className="flex items-center gap-3 px-2 mb-8">
          <div className="h-10 w-10 rounded-xl bg-tealCare-600 text-white flex items-center justify-center shadow-sm">
            <HeartPulse className="h-6 w-6" />
          </div>
          <div>
            <p className="font-bold text-slate-900 leading-tight">MedFits Care</p>
            <p className="text-xs text-tealCare-700 font-medium">Wellness Companion</p>
          </div>
        </Link>

        <nav className="flex-1 space-y-1.5">
          {navItems.map((item) => {
            const Icon = item.icon;
            const active = location.pathname.startsWith(item.to);
            return (
              <Link
                key={item.to}
                to={item.to}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition ${
                  active
                    ? "bg-tealCare-50 text-tealCare-700 border border-tealCare-100"
                    : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                }`}
              >
                <Icon className="h-4 w-4" />
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="border-t border-slate-100 pt-4 mt-4">
          <p className="text-xs font-semibold text-slate-800 truncate px-2">
            {profile?.full_name || user?.email}
          </p>
          <p className="text-xs text-slate-500 truncate px-2 mb-3">{user?.email}</p>
          <button
            onClick={handleLogout}
            className="w-full flex items-center gap-2 px-3 py-2 rounded-xl text-sm font-medium text-rose-600 hover:bg-rose-50 transition"
          >
            <LogOut className="h-4 w-4" />
            Sign Out
          </button>
        </div>
      </aside>

      {/* Mobile Drawer */}
      {mobileOpen && (
        <div className="fixed inset-0 z-40 lg:hidden flex">
          <div className="fixed inset-0 bg-slate-900/40" onClick={() => setMobileOpen(false)} />
          <div className="relative z-50 w-64 bg-white h-full p-5 flex flex-col">
            <div className="flex items-center justify-between mb-6">
              <span className="font-bold text-tealCare-700">MedFits Care</span>
              <button onClick={() => setMobileOpen(false)} aria-label="Close menu">
                <X className="h-5 w-5" />
              </button>
            </div>
            <nav className="flex-1 space-y-1">
              {navItems.map((item) => {
                const Icon = item.icon;
                const active = location.pathname.startsWith(item.to);
                return (
                  <Link
                    key={item.to}
                    to={item.to}
                    onClick={() => setMobileOpen(false)}
                    className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium ${
                      active ? "bg-tealCare-50 text-tealCare-700" : "text-slate-600"
                    }`}
                  >
                    <Icon className="h-4 w-4" />
                    {item.label}
                  </Link>
                );
              })}
            </nav>
            <button
              onClick={handleLogout}
              className="flex items-center gap-2 px-3 py-2 rounded-xl text-sm font-medium text-rose-600 hover:bg-rose-50"
            >
              <LogOut className="h-4 w-4" />
              Sign Out
            </button>
          </div>
        </div>
      )}

      {/* Main Content */}
      <div className="flex-1 flex flex-col min-w-0">
        <header className="lg:hidden bg-white border-b border-slate-200 px-4 py-3 flex items-center justify-between">
          <button
            onClick={() => setMobileOpen(true)}
            className="p-2 rounded-lg text-slate-600 hover:bg-slate-100"
            aria-label="Open navigation"
          >
            <Menu className="h-5 w-5" />
          </button>
          <span className="font-semibold text-tealCare-700">MedFits Care Companion</span>
          <div className="w-9" />
        </header>

        <main className="flex-1 p-4 sm:p-6 lg:p-8 max-w-7xl w-full mx-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
