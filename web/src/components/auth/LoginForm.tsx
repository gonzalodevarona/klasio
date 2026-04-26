"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import type { LoginResponse, AuthError } from "@/lib/types/auth";

const inputStyle: React.CSSProperties = {
  width: "100%",
  background: "#FAFAF8",
  border: "1.5px solid #DDDDD8",
  borderRadius: 8,
  padding: "8px 12px",
  fontSize: 13,
  color: "#0A0A0A",
  outline: "none",
  fontFamily: "var(--font-main)",
  boxSizing: "border-box",
};

const labelStyle: React.CSSProperties = {
  fontFamily: "var(--font-mono)",
  fontSize: 11,
  fontWeight: 600,
  textTransform: "uppercase",
  letterSpacing: "0.06em",
  color: "#4A4A48",
  display: "block",
  marginBottom: 6,
};

export default function LoginForm() {
  const t = useTranslations("auth.login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<{
    code: string;
    message: string;
    lockedUntil?: string;
  } | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });

      const data = await response.json();

      if (!response.ok) {
        const raw = (data as AuthError | { error: string }).error;
        if (typeof raw === "string") {
          setError({ code: raw, message: raw });
        } else {
          setError({
            code: raw.code,
            message: raw.message,
            lockedUntil: raw.lockedUntil,
          });
        }
        return;
      }

      const loginData = data as LoginResponse;
      window.location.href = loginData.dashboardUrl;
    } catch {
      setError({ code: "NETWORK_ERROR", message: t("errorNetwork") });
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: 16 }}>
      {error && (
        <div
          role="alert"
          style={{
            background: "#FFE8E8",
            border: "1px solid #FFCCCC",
            borderRadius: 8,
            padding: "12px 16px",
          }}
        >
          <p style={{ fontSize: 13, color: "#CC2200", margin: 0 }}>
            {error.code === "ACCOUNT_LOCKED" && error.lockedUntil
              ? t("errorAccountLocked", { date: new Date(error.lockedUntil).toLocaleString() })
              : error.code === "EMAIL_NOT_VERIFIED"
                ? t("errorEmailNotVerified")
                : error.code === "ACCOUNT_SETUP_PENDING"
                  ? t("errorAccountSetupPending")
                  : error.message}
          </p>
          {error.code === "ACCOUNT_SETUP_PENDING" && (
            <p style={{ marginTop: 8, fontSize: 13, color: "#CC2200", margin: "8px 0 0" }}>
              {t("accountSetupPendingResend")}{" "}
              <a href="/setup-account" style={{ color: "#0A0A0A", textDecoration: "underline" }}>
                {t("accountSetupPendingLink")}
              </a>
            </p>
          )}
        </div>
      )}

      <div>
        <label htmlFor="email" style={labelStyle}>
          {t("emailLabel")}
        </label>
        <input
          id="email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          placeholder={t("emailPlaceholder")}
          style={inputStyle}
          onFocus={(e) => { e.currentTarget.style.borderColor = "#CAFF4D"; }}
          onBlur={(e) => { e.currentTarget.style.borderColor = "#DDDDD8"; }}
        />
      </div>

      <div>
        <label htmlFor="password" style={labelStyle}>
          {t("passwordLabel")}
        </label>
        <div style={{ position: "relative" }}>
          <input
            id="password"
            type={showPassword ? "text" : "password"}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            style={{ ...inputStyle, paddingRight: 40 }}
            onFocus={(e) => { e.currentTarget.style.borderColor = "#CAFF4D"; }}
            onBlur={(e) => { e.currentTarget.style.borderColor = "#DDDDD8"; }}
          />
          <button
            type="button"
            onClick={() => setShowPassword((v) => !v)}
            style={{
              position: "absolute",
              top: 0,
              right: 0,
              bottom: 0,
              display: "flex",
              alignItems: "center",
              paddingRight: 12,
              background: "none",
              border: "none",
              cursor: "pointer",
              color: "#9A9A98",
            }}
            onMouseEnter={(e) => { e.currentTarget.style.color = "#4A4A48"; }}
            onMouseLeave={(e) => { e.currentTarget.style.color = "#9A9A98"; }}
            aria-label={showPassword ? t("hidePassword") : t("showPassword")}
          >
            {showPassword ? (
              <svg xmlns="http://www.w3.org/2000/svg" style={{ width: 20, height: 20 }} viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M3.707 2.293a1 1 0 00-1.414 1.414l14 14a1 1 0 001.414-1.414l-1.473-1.473A10.014 10.014 0 0019.542 10C18.268 5.943 14.478 3 10 3a9.958 9.958 0 00-4.512 1.074l-1.78-1.781zm4.261 4.26l1.514 1.515a2.003 2.003 0 012.45 2.45l1.514 1.514a4 4 0 00-5.478-5.478z" clipRule="evenodd" />
                <path d="M12.454 16.697L9.75 13.992a4 4 0 01-3.742-3.741L2.335 6.578A9.98 9.98 0 00.458 10c1.274 4.057 5.065 7 9.542 7 .847 0 1.669-.105 2.454-.303z" />
              </svg>
            ) : (
              <svg xmlns="http://www.w3.org/2000/svg" style={{ width: 20, height: 20 }} viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
                <path fillRule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clipRule="evenodd" />
              </svg>
            )}
          </button>
        </div>
      </div>

      <div style={{ textAlign: "right", marginTop: -8 }}>
        <a
          href="/forgot-password"
          style={{ fontSize: 12, color: "#9A9A98", textDecoration: "none" }}
          onMouseEnter={(e) => { e.currentTarget.style.color = "#4A4A48"; }}
          onMouseLeave={(e) => { e.currentTarget.style.color = "#9A9A98"; }}
        >
          {t("forgotPassword")}
        </a>
      </div>

      <button
        type="submit"
        disabled={loading}
        style={{
          width: "100%",
          background: "#0A0A0A",
          color: "#FAFAF8",
          borderRadius: 10,
          padding: "13px 0",
          fontSize: 14,
          fontWeight: 600,
          border: "none",
          cursor: loading ? "not-allowed" : "pointer",
          marginTop: 8,
          fontFamily: "var(--font-main)",
          opacity: loading ? 0.6 : 1,
        }}
      >
        {loading ? t("submitting") : t("submit")}
      </button>
    </form>
  );
}
