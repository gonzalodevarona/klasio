"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import SetupAccountForm from "@/components/auth/SetupAccountForm";

function SetupAccountContent() {
  const t = useTranslations("auth.setupAccount");
  const searchParams = useSearchParams();
  const token = searchParams.get("token");

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-gray-900">{t("pageTitle")}</h1>
          <p className="mt-2 text-sm text-gray-600">{t("pageSubtitle")}</p>
        </div>
        <SetupAccountForm token={token} />
      </div>
    </div>
  );
}

export default function SetupAccountPage() {
  const t = useTranslations("auth.setupAccount");

  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex items-center justify-center bg-gray-50">
          <p className="text-gray-600">{t("pageLoading")}</p>
        </div>
      }
    >
      <SetupAccountContent />
    </Suspense>
  );
}
