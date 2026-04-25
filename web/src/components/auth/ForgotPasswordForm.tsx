"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Input, Button } from "@/components/ui";

export default function ForgotPasswordForm() {
  const t = useTranslations("auth.forgotPassword");
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);

    try {
      await fetch(
        `${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1"}/auth/forgot-password`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email }),
          credentials: "include",
        }
      );
    } catch {
      // no-op — always show same message
    } finally {
      setLoading(false);
      setSubmitted(true);
    }
  }

  if (submitted) {
    return (
      <div className="bg-green-50 border border-green-200 rounded-md p-6 text-center">
        <h2 className="text-lg font-semibold text-green-800 mb-2">{t("successTitle")}</h2>
        <p className="text-sm text-green-700">
          {t("successMessage", { email })}
        </p>
        <a
          href="/login"
          className="mt-4 inline-block text-sm text-indigo-600 hover:text-indigo-500"
        >
          {t("backToLogin")}
        </a>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <Input
        label={t("emailLabel")}
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        required
        placeholder={t("emailPlaceholder")}
      />

      <Button variant="volt" type="submit" disabled={loading} className="w-full">
        {loading ? t("submitting") : t("submit")}
      </Button>

      <p className="text-center text-sm text-gray-600">
        <a href="/login" className="text-indigo-600 hover:text-indigo-500">
          {t("backToLogin")}
        </a>
      </p>
    </form>
  );
}
