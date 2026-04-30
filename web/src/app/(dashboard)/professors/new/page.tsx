import Link from "next/link";
import { getTranslations } from "next-intl/server";
import { Button } from "@/components/ui";
import ProfessorForm from "@/components/professors/ProfessorForm";

export const metadata = {
  title: "Add Professor - Klasio",
};

export default async function NewProfessorPage() {
  const tProfessors = await getTranslations("professors");
  const tCommon = await getTranslations("common");

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href="/professors">← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/professors" className="hover:text-k-subtle">{tProfessors("pageTitle")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">Add Professor</span>
        </nav>
      </div>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-8">
        Add Professor
      </h1>

      <ProfessorForm />
    </div>
  );
}
