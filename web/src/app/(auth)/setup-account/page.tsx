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
    <div className="min-h-screen flex flex-col items-center justify-center bg-k-dark px-4 py-12">
      <img src="/logo.svg" alt="Klasio" width={48} height={48} className="mb-8" />
      <div className="w-full max-w-md bg-k-surface rounded-k-xl shadow-k-modal p-10">
        <div className="text-center mb-6">
          <h1 className="text-[22px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
          <p className="mt-2 text-sm text-k-muted">{t("pageSubtitle")}</p>
        </div>
        <SetupAccountForm token={token} />
      </div>
    </div>
  );
}

export default function SetupAccountPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex flex-col items-center justify-center bg-k-dark px-4 py-12">
          <img src="/logo.svg" alt="Klasio" width={48} height={48} className="mb-8" />
          <div className="w-full max-w-md bg-k-surface rounded-k-xl shadow-k-modal p-10">
            <p className="text-center text-k-muted text-sm">Loading…</p>
          </div>
        </div>
      }
    >
      <SetupAccountContent />
    </Suspense>
  );
}
