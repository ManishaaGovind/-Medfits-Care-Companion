import { createContext, useContext, useEffect, useState } from "react";
import api from "../services/api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchSession = async () => {
    const token = localStorage.getItem("medfits_token");
    if (!token) {
      setUser(null);
      setProfile(null);
      setLoading(false);
      return;
    }
    try {
      const [meRes, profRes] = await Promise.all([
        api.get("/api/auth/me"),
        api.get("/api/profile").catch(() => ({ data: null })),
      ]);
      setUser(meRes.data);
      setProfile(profRes.data);
    } catch {
      localStorage.removeItem("medfits_token");
      setUser(null);
      setProfile(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSession();
  }, []);

  const login = async (email, password) => {
    const res = await api.post("/api/auth/login", { email, password });
    localStorage.setItem("medfits_token", res.data.access_token);
    await fetchSession();
  };

  const register = async (email, password, full_name) => {
    const res = await api.post("/api/auth/register", { email, password, full_name });
    localStorage.setItem("medfits_token", res.data.access_token);
    await fetchSession();
  };

  const logout = () => {
    localStorage.removeItem("medfits_token");
    setUser(null);
    setProfile(null);
  };

  return (
    <AuthContext.Provider
      value={{ user, profile, setProfile, loading, login, register, logout, refreshSession: fetchSession }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}
