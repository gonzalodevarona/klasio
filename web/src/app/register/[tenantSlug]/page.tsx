
import { getTranslations } from "next-intl/server";
import RegistrationForm from "@/components/auth/RegistrationForm";

interface RegisterPageProps {
  params: Promise<{ tenantSlug: string }>;
}

export default async function RegisterPage({ params }: RegisterPageProps) {
  const { tenantSlug } = await params;
  const t = await getTranslations("registerPage");

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-k-dark px-4 py-12">
      <img src="/logo.svg" alt="Klasio" width={48} height={48} className="mb-8" />
      <div className="w-full max-w-md bg-k-surface rounded-k-xl shadow-k-modal p-10">
        <div className="text-center mb-6">
          <h1 className="text-[22px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
          <p className="mt-2 text-sm text-k-muted">{t("subtitle", { tenantSlug })}</p>
        </div>
        <RegistrationForm tenantSlug={tenantSlug} />
      </div>
    </div>
  );
}
