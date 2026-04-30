import Link from "next/link";
import { getTranslations } from "next-intl/server";
import { Button } from "@/components/ui";
import StudentList from "@/components/students/StudentList";

export const metadata = {
  title: "Students - Klasio",
};

export default async function StudentsPage() {
  const t = await getTranslations("students");

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
        <Button variant="volt" asChild>
          <Link href="/students/new">+ {t("addButton")}</Link>
        </Button>
      </div>

      <StudentList />
    </div>
  );
}
