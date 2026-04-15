"use client";

import { Suspense, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";

type VerifyState = "loading" | "success" | "expired" | "already_used" | "error";

function VerifyEmailContent() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token");
  const [state, setState] = useState<VerifyState>("loading");
  const [resendEmail, setResendEmail] = useState("");
  const [resendSent, setResendSent] = useState(false);
  const [resendLoading, setResendLoading] = useState(false);

  useEffect(() => {
    if (!token) {
      setState("error");
      return;
    }

    async function verify() {
      try {
        const response = await fetch(
          `${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1"}/auth/verify-email?token=${token}`,
          { credentials: "include" }
        );

        if (response.ok) {
          setState("success");
          return;
        }

        const data = await response.json();
        const code = data.error?.code;

        if (code === "VERIFICATION_TOKEN_EXPIRED") {
          setState("expired");
        } else if (code === "VERIFICATION_TOKEN_ALREADY_USED") {
          setState("already_used");
        } else {
          setState("error");
        }
      } catch {
        setState("error");
      }
    }

    verify();
  }, [token]);

  async function handleResend() {
    if (!resendEmail) return;
    setResendLoading(true);

    try {
      await fetch(
        `${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1"}/auth/resend-verification`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email: resendEmail, tenantSlug: "default" }),
          credentials: "include",
        }
      );
      setResendSent(true);
    } catch {
      // no-op — same message regardless
      setResendSent(true);
    } finally {
      setResendLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4">
      <div className="max-w-md w-full space-y-6">
        {state === "loading" && (
          <div className="text-center">
            <p className="text-gray-600">Verifying your email...</p>
          </div>
        )}

        {state === "success" && (
          <div className="bg-green-50 border border-green-200 rounded-md p-6 text-center">
            <h2 className="text-lg font-semibold text-green-800 mb-2">Email Verified!</h2>
            <p className="text-sm text-green-700 mb-4">
              Your account has been activated. You can now log in.
            </p>
            <a
              href="/login"
              className="inline-block px-4 py-2 bg-indigo-600 text-white text-sm rounded-md hover:bg-indigo-700"
            >
              Go to Login
            </a>
          </div>
        )}

        {state === "already_used" && (
          <div className="bg-blue-50 border border-blue-200 rounded-md p-6 text-center">
            <h2 className="text-lg font-semibold text-blue-800 mb-2">Already Verified</h2>
            <p className="text-sm text-blue-700 mb-4">
              This verification link has already been used. Your account is active.
            </p>
            <a
              href="/login"
              className="inline-block px-4 py-2 bg-indigo-600 text-white text-sm rounded-md hover:bg-indigo-700"
            >
              Go to Login
            </a>
          </div>
        )}

        {(state === "expired" || state === "error") && (
          <div className="bg-red-50 border border-red-200 rounded-md p-6 text-center">
            <h2 className="text-lg font-semibold text-red-800 mb-2">
              {state === "expired" ? "Link Expired" : "Verification Failed"}
            </h2>
            <p className="text-sm text-red-700 mb-4">
              {state === "expired"
                ? "This verification link has expired. Please request a new one."
                : "Something went wrong. Please try again or request a new verification link."}
            </p>

            {!resendSent ? (
              <div className="mt-4 space-y-2">
                <input
                  type="email"
                  value={resendEmail}
                  onChange={(e) => setResendEmail(e.target.value)}
                  placeholder="Enter your email"
                  className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm text-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                />
                <button
                  onClick={handleResend}
                  disabled={resendLoading || !resendEmail}
                  className="w-full px-4 py-2 bg-indigo-600 text-white text-sm rounded-md hover:bg-indigo-700 disabled:opacity-50"
                >
                  {resendLoading ? "Sending..." : "Resend Verification Email"}
                </button>
              </div>
            ) : (
              <p className="text-sm text-green-700">
                If an unverified account exists with that email, a new verification email has been sent.
              </p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

export default function VerifyEmailPage() {
  return (
    <Suspense fallback={<div className="min-h-screen flex items-center justify-center bg-gray-50"><p className="text-gray-600">Loading...</p></div>}>
      <VerifyEmailContent />
    </Suspense>
  );
}
