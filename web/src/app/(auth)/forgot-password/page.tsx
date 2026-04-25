import { getTranslations } from "next-intl/server";
import ForgotPasswordForm from "@/components/auth/ForgotPasswordForm";

export default async function ForgotPasswordPage() {
  const t = await getTranslations("forgotPasswordPage");

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-k-dark px-4 py-12">
      <div className="mb-8 text-white font-extrabold text-2xl tracking-[-0.04em] text-center">
        klasio
      </div>
      <div className="w-full max-w-md bg-k-surface rounded-k-xl shadow-k-modal p-10">
        <div className="text-center mb-6">
          <h1 className="text-[22px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
          <p className="mt-2 text-sm text-k-muted">{t("subtitle")}</p>
        </div>
        <ForgotPasswordForm />
      </div>
    </div>
  );
}
