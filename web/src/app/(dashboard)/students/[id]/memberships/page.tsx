"use client";

import { use, useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useMemberships, useMembershipActions } from "@/hooks/useMemberships";
import { useStudentDetail } from "@/hooks/useStudents";
import MembershipList from "@/components/memberships/MembershipList";
import { Button } from "@/components/ui";

interface Props {
  params: Promise<{ id: string }>;
}

export default function StudentMembershipsPage({ params }: Props) {
  const { id: studentId } = use(params);
  const t = useTranslations("memberships");
  const tStudents = useTranslations("students");
  const tCommon = useTranslations("common");
  const { memberships, loading, error, refetch } = useMemberships(studentId);
  const { student } = useStudentDetail(studentId);
  const studentName = student ? `${student.firstName} ${student.lastName}` : studentId;
  const { activateMembership, validatePayment, loading: actionLoading, error: actionError } =
    useMembershipActions();

  const [statusFilter, setStatusFilter] = useState<string>("");

  const filtered = statusFilter
    ? memberships.filter((m) => m.status === statusFilter)
    : memberships;

  async function handleActivate(id: string) {
    await activateMembership(id);
    refetch();
  }

  async function handleValidatePayment(id: string) {
    await validatePayment(id, { activateDirectly: true });
    refetch();
  }

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/students/${studentId}`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/students" className="hover:text-k-subtle">{tStudents("pageTitle")}</Link>
          <span className="mx-2">/</span>
          <Link href={`/students/${studentId}`} className="hover:text-k-subtle">{studentName}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{t("detailBreadcrumb")}</span>
        </nav>
      </div>

      <div className="flex items-center justify-between mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("adminPageTitle")}</h1>
        <Button variant="volt" asChild>
          <Link href={`/students/${studentId}/memberships/new`}>+ {t("adminNewButton")}</Link>
        </Button>
      </div>

      <div className="mb-4 flex gap-2">
        {["", "ACTIVE", "INACTIVE", "EXPIRED", "PENDING_PAYMENT", "PENDING_PAYMENT_VALIDATION", "PENDING_MANAGER_ACTIVATION"].map(
          (s) => (
            <button
              key={s}
              onClick={() => setStatusFilter(s)}
              className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
                statusFilter === s
                  ? "bg-k-dark text-white"
                  : "bg-k-bg text-k-subtle hover:bg-k-border"
              }`}
            >
              {s === "" ? t("adminFilterAll") : s.replace(/_/g, " ")}
            </button>
          )
        )}
      </div>

      {(actionError || error) && (
        <div className="mb-4 rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text">
          {actionError ?? error}
        </div>
      )}

      {loading ? (
        <div className="py-8 text-center text-sm text-k-muted">{t("adminLoading")}</div>
      ) : (
        <MembershipList
          memberships={filtered}
          studentId={studentId}
          onActivate={actionLoading ? undefined : handleActivate}
          onValidatePayment={actionLoading ? undefined : handleValidatePayment}
        />
      )}
    </div>
  );
}
