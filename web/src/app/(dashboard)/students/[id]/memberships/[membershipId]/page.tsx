"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useMembershipDetail } from "@/hooks/useMemberships";
import { useStudentDetail } from "@/hooks/useStudents";
import { useAuth } from "@/hooks/useAuth";
import MembershipDetail from "@/components/memberships/MembershipDetail";
import { PaymentProofTimeline } from "@/components/payment-proofs/PaymentProofTimeline";
import { Button } from "@/components/ui";

interface Props {
  params: Promise<{ id: string; membershipId: string }>;
}

export default function MembershipDetailPage({ params }: Props) {
  const { id: studentId, membershipId } = use(params);
  const t = useTranslations("memberships");
  const tStudents = useTranslations("students");
  const tCommon = useTranslations("common");
  const { membership, loading, error, refetch } = useMembershipDetail(membershipId);
  const { student } = useStudentDetail(studentId);
  const studentName = student ? `${student.firstName} ${student.lastName}` : studentId;
  const { user } = useAuth();

  const isAdmin = (user?.roles.includes("ADMIN") || user?.roles.includes("SUPERADMIN")) ?? false;

  return (
    <div>
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
          <span className="text-k-subtle">{t("detailBreadcrumbDetail")}</span>
        </nav>
      </div>

      {loading && (
        <div className="py-8 text-center text-sm text-k-muted">{t("detailLoading")}</div>
      )}

      {error && (
        <div className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text">
          {error}
        </div>
      )}

      {membership && (
        <div className="space-y-6">
          <MembershipDetail
            membership={membership}
            onRefresh={refetch}
            isAdmin={isAdmin}
          />
          <PaymentProofTimeline membershipId={membershipId} membershipStatus={membership.status} />
        </div>
      )}
    </div>
  );
}
