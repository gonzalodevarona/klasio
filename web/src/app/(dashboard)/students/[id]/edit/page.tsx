"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import StudentForm from "@/components/students/StudentForm";
import { useStudentDetail } from "@/hooks/useStudents";
import { Button } from "@/components/ui";

interface EditStudentPageProps {
  params: Promise<{ id: string }>;
}

export default function EditStudentPage({ params }: EditStudentPageProps) {
  const { id } = use(params);
  const tStudents = useTranslations("students");
  const tCommon = useTranslations("common");
  const { student, loading, error } = useStudentDetail(id);

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/students/${id}`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/students" className="hover:text-k-subtle">{tStudents("pageTitle")}</Link>
          <span className="mx-2">/</span>
          <Link href={`/students/${id}`} className="hover:text-k-subtle">
            {student ? `${student.firstName} ${student.lastName}` : id}
          </Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">Edit</span>
        </nav>
      </div>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-8">Edit Student</h1>

      {loading && (
        <div className="text-center py-8 text-sm text-k-muted">
          Loading student...
        </div>
      )}

      {error && (
        <div
          className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text"
          role="alert"
        >
          {error}
        </div>
      )}

      {student && <StudentForm student={student} />}
    </div>
  );
}
