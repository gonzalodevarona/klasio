"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useMembershipDetail } from "@/hooks/useMemberships";
import MembershipDetail from "@/components/memberships/MembershipDetail";
import { PaymentProofPanel } from "@/components/payment-proofs/PaymentProofPanel";
import { PaymentProofTimeline } from "@/components/payment-proofs/PaymentProofTimeline";
import { Button } from "@/components/ui";

interface Props {
  params: Promise<{ membershipId: string }>;
}

export default function StudentMembershipDetailPage({ params }: Props) {
  const { membershipId } = use(params);
  const t = useTranslations("memberships");
  const tPage = useTranslations("studentMembershipsPage");
  const tCommon = useTranslations("common");
  const { membership, loading, error, refetch } = useMembershipDetail(membershipId);

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href="/student/memberships">← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/student/memberships" className="hover:text-k-subtle">{tPage("title")}</Link>
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
            isAdmin={false}
            isManager={false}
          />
          {(membership.status === "PENDING_PAYMENT" ||
            membership.status === "PENDING_PAYMENT_VALIDATION" ||
            membership.status === "EXPIRED") && (
            <PaymentProofPanel membershipId={membershipId} membershipStatus={membership.status} />
          )}
          <PaymentProofTimeline membershipId={membershipId} membershipStatus={membership.status} />
        </div>
      )}
    </div>
  );
}
