"use client";

import Image from "next/image";
import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui";
import ResetPasswordForm from "@/components/auth/ResetPasswordForm";

const AuthShell = ({ children }: { children: React.ReactNode }) => (
  <div className="min-h-screen flex flex-col items-center justify-center bg-k-dark px-4 py-12">
    <Image src="/logo.svg" alt="Klasio" width={48} height={48} className="mb-8" priority />
    <div className="w-full max-w-md bg-k-surface rounded-k-xl shadow-k-modal p-10">
      {children}
    </div>
  </div>
);

function ResetPasswordContent() {
  const t = useTranslations("resetPasswordPage");
  const searchParams = useSearchParams();
  const token = searchParams.get("token");

  if (!token) {
    return (
      <AuthShell>
        <div className="text-center">
          <h2 className="text-lg font-semibold text-k-danger-text mb-2">{t("invalidTitle")}</h2>
          <p className="text-sm text-k-subtle mb-4">{t("invalidBody")}</p>
          <Button variant="primary" asChild>
            <Link href="/forgot-password">{t("requestNew")}</Link>
          </Button>
        </div>
      </AuthShell>
    );
  }

  return (
    <AuthShell>
      <div className="text-center mb-6">
        <h1 className="text-[22px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
        <p className="mt-2 text-sm text-k-muted">{t("subtitle")}</p>
      </div>
      <ResetPasswordForm token={token} />
    </AuthShell>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex flex-col items-center justify-center bg-k-dark px-4 py-12">
          <Image src="/logo.svg" alt="Klasio" width={48} height={48} className="mb-8" />
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
