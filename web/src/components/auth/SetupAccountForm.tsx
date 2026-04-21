"use client";

import { useState } from "react";
import PasswordPolicyChecker, { validatePassword } from "./PasswordPolicyChecker";

interface SetupAccountFormProps {
  token: string | null;
}

type Phase =
  | "form"          // initial state — enter new password
  | "success"       // account set up OK
  | "expired"       // 410 from backend
  | "resend"        // user clicked "Request a new link"
  | "resend-done";  // resend confirmed

export default function SetupAccountForm({ token }: SetupAccountFormProps) {
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  // When no token is present, skip straight to the resend form
  const [phase, setPhase] = useState<Phase>(token ? "form" : "resend");
  const [formError, setFormError] = useState<string | null>(null);

  // Resend sub-form
  const [resendEmail, setResendEmail] = useState("");
  const [resendLoading, setResendLoading] = useState(false);

  // ---------------------------------------------------------------------------
  // Setup form submission
  // ---------------------------------------------------------------------------

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setFormError(null);

    if (password !== confirmPassword) {
      setFormError("Passwords do not match.");
      return;
    }

    if (!validatePassword(password)) {
      setFormError("Password does not meet the requirements.");
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

      setFormError("Something went wrong. Please try again.");
    } catch {
      setFormError("Something went wrong. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  // ---------------------------------------------------------------------------
  // Resend submission
  // ---------------------------------------------------------------------------

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

  // ---------------------------------------------------------------------------
  // Render: success
  // ---------------------------------------------------------------------------

  if (phase === "success") {
    return (
      <div role="status" className="bg-green-50 border border-green-200 rounded-md p-6 text-center">
        <h2 className="text-lg font-semibold text-green-800 mb-2">Account Setup Complete</h2>
        <p className="text-sm text-green-700 mb-4">
          Your account is ready! You can now log in.
        </p>
        <a
          href="/login"
          className="inline-block px-4 py-2 bg-indigo-600 text-white text-sm rounded-md hover:bg-indigo-700"
        >
          Log In
        </a>
      </div>
    );
  }

  // ---------------------------------------------------------------------------
  // Render: expired / invalid link
  // ---------------------------------------------------------------------------

  if (phase === "expired") {
    return (
      <div className="space-y-6">
        <div role="alert" className="bg-red-50 border border-red-200 rounded-md p-6 text-center">
          <h2 className="text-lg font-semibold text-red-800 mb-2">Setup Link Expired</h2>
          <p className="text-sm text-red-700 mb-4">
            This setup link has expired or is no longer valid.
          </p>
          <button
            type="button"
            onClick={() => setPhase("resend")}
            className="inline-block px-4 py-2 bg-indigo-600 text-white text-sm rounded-md hover:bg-indigo-700"
          >
            Request a New Link
          </button>
        </div>
      </div>
    );
  }

  // ---------------------------------------------------------------------------
  // Render: resend form
  // ---------------------------------------------------------------------------

  if (phase === "resend") {
    return (
      <form onSubmit={handleResendSubmit} className="space-y-6">
        <div className="bg-amber-50 border border-amber-200 rounded-md p-4">
          <p className="text-sm text-amber-800">
            Enter your email address and we&apos;ll send you a new setup link.
          </p>
        </div>

        <div>
          <label htmlFor="resend-email" className="block text-sm font-medium text-gray-700">
            Email address
          </label>
          <input
            id="resend-email"
            type="email"
            value={resendEmail}
            onChange={(e) => setResendEmail(e.target.value)}
            required
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
            placeholder="you@example.com"
          />
        </div>

        <button
          type="submit"
          disabled={resendLoading}
          className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
        >
          {resendLoading ? "Sending..." : "Send New Link"}
        </button>
      </form>
    );
  }

  // ---------------------------------------------------------------------------
  // Render: resend confirmation
  // ---------------------------------------------------------------------------

  if (phase === "resend-done") {
    return (
      <div role="status" className="bg-green-50 border border-green-200 rounded-md p-6 text-center">
        <h2 className="text-lg font-semibold text-green-800 mb-2">Email Sent</h2>
        <p className="text-sm text-green-700">
          A new setup link has been sent to your email.
        </p>
      </div>
    );
  }

  // ---------------------------------------------------------------------------
  // Render: main password form
  // ---------------------------------------------------------------------------

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {formError && (
        <div role="alert" className="bg-red-50 border border-red-200 rounded-md p-4">
          <p className="text-sm text-red-800">{formError}</p>
        </div>
      )}

      <div>
        <label htmlFor="setup-password" className="block text-sm font-medium text-gray-700">
          New Password
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
          Confirm New Password
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
        {loading ? "Setting up..." : "Set Password"}
      </button>
    </form>
  );
}
