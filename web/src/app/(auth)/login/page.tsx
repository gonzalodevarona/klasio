import { getTranslations } from "next-intl/server";
import LoginForm from "@/components/auth/LoginForm";

export default async function LoginPage() {
  const t = await getTranslations("loginPage");

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="max-w-md w-full space-y-8 p-8 bg-white rounded-lg shadow">
        <div>
          <h1 className="text-2xl font-bold text-center text-gray-900">
            {t("title")}
          </h1>
          <p className="mt-2 text-center text-sm text-gray-600">
            {t("subtitle")}
          </p>
        </div>
        <LoginForm />
      </div>
    </div>
  );
}
