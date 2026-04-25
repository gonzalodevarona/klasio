"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import StudentDetail from "@/components/students/StudentDetail";
import { useStudentDetail } from "@/hooks/useStudents";
import { Button } from "@/components/ui";

interface StudentDetailPageProps {
  params: Promise<{ id: string }>;
}

export default function StudentDetailPage({ params }: StudentDetailPageProps) {
  const tCommon = useTranslations("common");
  const tStudents = useTranslations("students");
  const { id } = use(params);
  const { student, loading, error, refetch } = useStudentDetail(id);

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href="/students">← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/students" className="hover:text-k-subtle">{tStudents("pageTitle")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">
            {student ? `${student.firstName} ${student.lastName}` : id}
          </span>
        </nav>
      </div>

      {loading && (
        <div className="text-center py-8 text-sm text-k-muted">
          Loading student details...
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

      {student && <StudentDetail student={student} onStatusChanged={refetch} />}
    </div>
  );
}
