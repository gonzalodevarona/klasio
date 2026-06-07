import { headers } from "next/headers";
import { getTranslations } from "next-intl/server";
import { tenantSlugFromHost } from "@/lib/tenant/tenantSlugFromHost";
import SelfRegistration from "@/components/auth/SelfRegistration";

export default async function RegisterPage() {
  const t = await getTranslations("registerPage");
  const host = (await headers()).get("host");
  const rootDomain = process.env.NEXT_PUBLIC_ROOT_DOMAIN ?? "localhost";
  const slug = tenantSlugFromHost(host, rootDomain);

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-k-dark px-4 py-12">
      <img src="/logo.svg" alt="Klasio" width={48} height={48} className="mb-8" />
      <div className="w-full max-w-2xl bg-k-surface rounded-k-xl shadow-k-modal p-10">
        {slug ? (
          <>
            <div className="text-center mb-6">
              <h1 className="text-[22px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
              <p className="mt-2 text-sm text-k-muted">{t("subtitle", { tenantSlug: slug })}</p>
            </div>
            <SelfRegistration tenantSlug={slug} />
          </>
        ) : (
          <div className="text-center">
            <h1 className="text-[22px] font-extrabold text-k-dark">{t("noTenantTitle")}</h1>
            <p className="mt-2 text-sm text-k-muted">{t("noTenantBody")}</p>
          </div>
        )}
      </div>
    </div>
  );
}
