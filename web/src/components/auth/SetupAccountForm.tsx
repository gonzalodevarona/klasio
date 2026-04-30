"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import PasswordPolicyChecker, { validatePassword } from "./PasswordPolicyChecker";
import { Input, Button } from "@/components/ui";

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
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
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

        <Input
          label={t("resendEmailLabel")}
          type="email"
          value={resendEmail}
          onChange={(e) => setResendEmail(e.target.value)}
          required
          placeholder={t("resendEmailPlaceholder")}
        />

        <Button variant="volt" type="submit" disabled={resendLoading} className="w-full">
          {resendLoading ? t("resendSubmitting") : t("resendSubmit")}
        </Button>
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
        <label htmlFor="setup-password" className="block text-sm font-medium text-k-subtle">
          {t("labelNewPassword")}
        </label>
        {/* TODO: migrate to <Input> when primitive supports a trailing-icon slot. */}
        <div className="relative mt-1">
          <input
            id="setup-password"
            type={showPassword ? "text" : "password"}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            className="bg-k-surface border border-k-border rounded-k-sm px-3 py-2 pr-10 text-sm focus:border-k-volt focus:outline-none w-full"
          />
          <button
            type="button"
            onClick={() => setShowPassword((v) => !v)}
            className="absolute inset-y-0 right-0 flex items-center pr-3 text-k-muted hover:text-k-subtle"
            aria-label={showPassword ? "Hide password" : "Show password"}
          >
            {showPassword ? (
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M3.707 2.293a1 1 0 00-1.414 1.414l14 14a1 1 0 001.414-1.414l-1.473-1.473A10.014 10.014 0 0019.542 10C18.268 5.943 14.478 3 10 3a9.958 9.958 0 00-4.512 1.074l-1.78-1.781zm4.261 4.26l1.514 1.515a2.003 2.003 0 012.45 2.45l1.514 1.514a4 4 0 00-5.478-5.478z" clipRule="evenodd" />
                <path d="M12.454 16.697L9.75 13.992a4 4 0 01-3.742-3.741L2.335 6.578A9.98 9.98 0 00.458 10c1.274 4.057 5.065 7 9.542 7 .847 0 1.669-.105 2.454-.303z" />
              </svg>
            ) : (
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
                <path fillRule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clipRule="evenodd" />
              </svg>
            )}
          </button>
        </div>
        <PasswordPolicyChecker password={password} />
      </div>

      <div>
        <label htmlFor="setup-confirm-password" className="block text-sm font-medium text-k-subtle">
          {t("labelConfirmPassword")}
        </label>
        {/* TODO: migrate to <Input> when primitive supports a trailing-icon slot. */}
        <div className="relative mt-1">
          <input
            id="setup-confirm-password"
            type={showConfirmPassword ? "text" : "password"}
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
            className="bg-k-surface border border-k-border rounded-k-sm px-3 py-2 pr-10 text-sm focus:border-k-volt focus:outline-none w-full"
          />
          <button
            type="button"
            onClick={() => setShowConfirmPassword((v) => !v)}
            className="absolute inset-y-0 right-0 flex items-center pr-3 text-k-muted hover:text-k-subtle"
            aria-label={showConfirmPassword ? "Hide password" : "Show password"}
          >
            {showConfirmPassword ? (
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M3.707 2.293a1 1 0 00-1.414 1.414l14 14a1 1 0 001.414-1.414l-1.473-1.473A10.014 10.014 0 0019.542 10C18.268 5.943 14.478 3 10 3a9.958 9.958 0 00-4.512 1.074l-1.78-1.781zm4.261 4.26l1.514 1.515a2.003 2.003 0 012.45 2.45l1.514 1.514a4 4 0 00-5.478-5.478z" clipRule="evenodd" />
                <path d="M12.454 16.697L9.75 13.992a4 4 0 01-3.742-3.741L2.335 6.578A9.98 9.98 0 00.458 10c1.274 4.057 5.065 7 9.542 7 .847 0 1.669-.105 2.454-.303z" />
              </svg>
            ) : (
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
                <path fillRule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clipRule="evenodd" />
              </svg>
            )}
          </button>
        </div>
      </div>

      <Button variant="volt" type="submit" disabled={loading} className="w-full">
        {loading ? t("submitting") : t("submit")}
      </Button>
    </form>
  );
}
