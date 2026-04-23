"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import PasswordPolicyChecker, { validatePassword } from "./PasswordPolicyChecker";

interface SetupAccountFormProps {
  token: string | null;
}

type Phase =
  | "form"        // initial state — enter new password
  | "success"     // account set up OK
  | "expired"     // 410 from backend
  | "resend"      // user clicked "Request a new link"
  | "resend-done"; // resend confirmed

export default function SetupAccountForm({ token }: SetupAccountFormProps) {
  const t = useTranslations("auth.setupAccount");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [phase, setPhase] = useState<Phase>(token ? "form" : "resend");
  const [formError, setFormError] = useState<string | null>(null);

  const [resendEmail, setResendEmail] = useState("");
  const [resendLoading, setResendLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setFormError(null);

    if (password !== confirmPassword) {
      setFormError(t("errorPasswordMismatch"));
      return;
    }

    if (!validatePassword(password)) {
      setFormError(t("errorPasswordInvalid"));
      return;
    }

    setLoading(true);

    try {
      const apiBase =
        process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

      const response = await fetch(`${apiBase}/auth/setup-account`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token, newPassword: password }),
        credentials: "include",
      });

      if (response.ok) {
        setPhase("success");
        return;
      }

      if (response.status === 410) {
        setPhase("expired");
        return;
      }

      setFormError(t("errorGeneric"));
    } catch {
      setFormError(t("errorGeneric"));
    } finally {
      setLoading(false);
    }
  }

  async function handleResendSubmit(e: React.FormEvent) {
    e.preventDefault();
    setResendLoading(true);

    try {
      const apiBase =
        process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

      await fetch(`${apiBase}/auth/resend-setup`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: resendEmail }),
        credentials: "include",
      });
    } catch {
      // always show success — backend is silent even when email not found
    } finally {
      setResendLoading(false);
      setPhase("resend-done");
    }
  }

  if (phase === "success") {
    return (
      <div role="status" className="bg-green-50 border border-green-200 rounded-md p-6 text-center">
        <h2 className="text-lg font-semibold text-green-800 mb-2">{t("successTitle")}</h2>
        <p className="text-sm text-green-700 mb-4">{t("successMessage")}</p>
        <a
          href="/login"
          className="inline-block px-4 py-2 bg-indigo-600 text-white text-sm rounded-md hover:bg-indigo-700"
        >
          {t("successLogIn")}
        </a>
      </div>
    );
  }

  if (phase === "expired") {
    return (
      <div className="space-y-6">
        <div role="alert" className="bg-red-50 border border-red-200 rounded-md p-6 text-center">
          <h2 className="text-lg font-semibold text-red-800 mb-2">{t("expiredTitle")}</h2>
          <p className="text-sm text-red-700 mb-4">{t("expiredMessage")}</p>
          <button
            type="button"
            onClick={() => setPhase("resend")}
            className="inline-block px-4 py-2 bg-indigo-600 text-white text-sm rounded-md hover:bg-indigo-700"
          >
            {t("expiredRequestNew")}
          </button>
        </div>
      </div>
    );
  }

  if (phase === "resend") {
    return (
      <form onSubmit={handleResendSubmit} className="space-y-6">
        <div className="bg-amber-50 border border-amber-200 rounded-md p-4">
          <p className="text-sm text-amber-800">{t("resendHint")}</p>
        </div>

        <div>
          <label htmlFor="resend-email" className="block text-sm font-medium text-gray-700">
            {t("resendEmailLabel")}
          </label>
          <input
            id="resend-email"
            type="email"
            value={resendEmail}
            onChange={(e) => setResendEmail(e.target.value)}
            required
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
            placeholder={t("resendEmailPlaceholder")}
          />
        </div>

        <button
          type="submit"
          disabled={resendLoading}
          className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
        >
          {resendLoading ? t("resendSubmitting") : t("resendSubmit")}
        </button>
      </form>
    );
  }

  if (phase === "resend-done") {
    return (
      <div role="status" className="bg-green-50 border border-green-200 rounded-md p-6 text-center">
        <h2 className="text-lg font-semibold text-green-800 mb-2">{t("resendDoneTitle")}</h2>
        <p className="text-sm text-green-700">{t("resendDoneMessage")}</p>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {formError && (
        <div role="alert" className="bg-red-50 border border-red-200 rounded-md p-4">
          <p className="text-sm text-red-800">{formError}</p>
        </div>
      )}

      <div>
        <label htmlFor="setup-password" className="block text-sm font-medium text-gray-700">
          {t("labelNewPassword")}
        </label>
        <input
          id="setup-password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
        />
        <PasswordPolicyChecker password={password} />
      </div>

      <div>
        <label htmlFor="setup-confirm-password" className="block text-sm font-medium text-gray-700">
          {t("labelConfirmPassword")}
        </label>
        <input
          id="setup-confirm-password"
          type="password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          required
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
        />
      </div>

      <button
        type="submit"
        disabled={loading}
        className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
      >
        {loading ? t("submitting") : t("submit")}
      </button>
    </form>
  );
}
