import Link from "next/link";
import { getTranslations } from "next-intl/server";
import { Button } from "@/components/ui";
import StudentForm from "@/components/students/StudentForm";

export const metadata = {
  title: "Add Student - Klasio",
};

export default async function NewStudentPage() {
  const tStudents = await getTranslations("students");
  const tCommon = await getTranslations("common");

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href="/students">← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/students" className="hover:text-k-subtle">{tStudents("pageTitle")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{tStudents("addButton")}</span>
        </nav>
      </div>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-8">
        {tStudents("addButton")}
      </h1>

      <StudentForm />
    </div>
  );
}
