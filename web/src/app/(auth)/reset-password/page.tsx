"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui";
import ResetPasswordForm from "@/components/auth/ResetPasswordForm";

function ResetPasswordContent() {
  const t = useTranslations("resetPasswordPage");
  const searchParams = useSearchParams();
  const token = searchParams.get("token");

  const shell = (children: React.ReactNode) => (
    <div className="min-h-screen flex flex-col items-center justify-center bg-k-dark px-4 py-12">
      <div className="mb-8 text-white font-extrabold text-2xl tracking-[-0.04em] text-center">
        klasio
      </div>
      <div className="w-full max-w-md bg-k-surface rounded-k-xl shadow-k-modal p-10">
        {children}
      </div>
    </div>
  );

  if (!token) {
    return shell(
      <div className="text-center">
        <h2 className="text-lg font-semibold text-k-danger-text mb-2">{t("invalidTitle")}</h2>
        <p className="text-sm text-k-subtle mb-4">{t("invalidBody")}</p>
        <Button variant="primary" asChild>
          <Link href="/forgot-password">{t("requestNew")}</Link>
        </Button>
      </div>
    );
  }

  return shell(
    <>
      <div className="text-center mb-6">
        <h1 className="text-[22px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
        <p className="mt-2 text-sm text-k-muted">{t("subtitle")}</p>
      </div>
      <ResetPasswordForm token={token} />
    </>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex flex-col items-center justify-center bg-k-dark px-4 py-12">
          <div className="mb-8 text-white font-extrabold text-2xl tracking-[-0.04em] text-center">
            klasio
          </div>
          <div className="w-full max-w-md bg-k-surface rounded-k-xl shadow-k-modal p-10">
            <p className="text-center text-k-muted text-sm">Loading…</p>
          </div>
        </div>
      }
    >
      <ResetPasswordContent />
    </Suspense>
  );
}
