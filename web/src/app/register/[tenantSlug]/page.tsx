import { getTranslations } from "next-intl/server";
import RegistrationForm from "@/components/auth/RegistrationForm";

interface RegisterPageProps {
  params: Promise<{ tenantSlug: string }>;
}

export default async function RegisterPage({ params }: RegisterPageProps) {
  const { tenantSlug } = await params;
  const t = await getTranslations("registerPage");

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-lg w-full space-y-8">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-gray-900">{t("title")}</h1>
          <p className="mt-2 text-sm text-gray-600">
            {t("subtitle", { tenantSlug })}
          </p>
        </div>
        <RegistrationForm tenantSlug={tenantSlug} />
      </div>
    </div>
  );
}
