import Link from "next/link";
import { getTranslations } from "next-intl/server";
import StudentList from "@/components/students/StudentList";

export const metadata = {
  title: "Students - Klasio",
};

export default async function StudentsPage() {
  const t = await getTranslations("students");

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">{t("pageTitle")}</h1>
        <Link
          href="/students/new"
          className="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
        >
          {t("addButton")}
        </Link>
      </div>

      <StudentList />
    </div>
  );
}
