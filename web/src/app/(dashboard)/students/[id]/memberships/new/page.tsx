"use client";

import { use, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { Button, Card, Select } from "@/components/ui";
import MembershipForm from "@/components/memberships/MembershipForm";
import { useMembershipActions } from "@/hooks/useMemberships";
import { useStudentDetail } from "@/hooks/useStudents";
import { useProgramPlansByProgram } from "@/hooks/usePrograms";
import { CreateMembershipRequest } from "@/lib/types/membership";

interface Props {
  params: Promise<{ id: string }>;
}

export default function NewMembershipPage({ params }: Props) {
  const { id: studentId } = use(params);
  const router = useRouter();
  const t = useTranslations("memberships");
  const tStudents = useTranslations("students");
  const tCommon = useTranslations("common");
  const { createMembership } = useMembershipActions();
  const { student, loading: studentLoading } = useStudentDetail(studentId);
  const studentName = student ? `${student.firstName} ${student.lastName}` : studentId;

  const activeEnrollments = student?.enrollments?.filter((e) => e.status === "ACTIVE") ?? [];
  const [selectedProgramId, setSelectedProgramId] = useState<string>("");

  // Scope plans to the selected program; exclude CLASSES_PER_WEEK (backend rejects them for memberships)
  const { plans: rawPlans, loading: plansLoading } = useProgramPlansByProgram(selectedProgramId || null);
  const plans = rawPlans.filter((p) => p.modality !== "CLASSES_PER_WEEK");

  async function handleSubmit(data: CreateMembershipRequest) {
    await createMembership(data);
    router.push(`/students/${studentId}/memberships`);
  }

  return (
    <div className="max-w-xl">
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/students/${studentId}/memberships`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/students" className="hover:text-k-subtle">{tStudents("pageTitle")}</Link>
          <span className="mx-2">/</span>
          <Link href={`/students/${studentId}`} className="hover:text-k-subtle">{studentName}</Link>
          <span className="mx-2">/</span>
          <Link href={`/students/${studentId}/memberships`} className="hover:text-k-subtle">{t("detailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{t("newBreadcrumb")}</span>
        </nav>
      </div>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-6">{t("newPageTitle")}</h1>

      {studentLoading ? (
        <div className="py-8 text-center text-sm text-k-muted">{t("newLoadingStudent")}</div>
      ) : activeEnrollments.length === 0 ? (
        <div className="rounded-k-sm bg-k-warn-bg border border-k-warn-text/30 p-4 text-sm text-k-warn-text">
          {t("newNoEnrollments")}
        </div>
      ) : (
        <Card padding="md">
          <div className="space-y-5">
            {/* Program selector — scopes plan list */}
            <div>
              <label className="block text-sm font-medium text-k-subtle mb-1">{t("formProgramLabel")}</label>
              <Select
                value={selectedProgramId}
                onChange={(e) => setSelectedProgramId(e.target.value)}
              >
                <option value="">{t("formSelectProgram")}</option>
                {activeEnrollments.map((e) => (
                  <option key={e.programId} value={e.programId}>
                    {e.programName} ({e.level})
                  </option>
                ))}
              </Select>
            </div>

            {selectedProgramId && (
              plansLoading ? (
                <div className="py-4 text-center text-sm text-k-muted">{t("newLoadingPlans")}</div>
              ) : plans.length === 0 ? (
                <div className="rounded-k-sm bg-k-warn-bg border border-k-warn-text/30 p-3 text-sm text-k-warn-text">
                  {t("newNoPlans")}
                </div>
              ) : (
                <MembershipForm
                  studentId={studentId}
                  plans={plans}
                  onSubmit={handleSubmit}
                  onCancel={() => router.push(`/students/${studentId}/memberships`)}
                />
              )
            )}
          </div>
        </Card>
      )}
    </div>
  );
}
