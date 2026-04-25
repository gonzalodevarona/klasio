"use client";

import { Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { Card } from "@/components/ui";
import StudentMembershipCreationForm from "@/components/memberships/StudentMembershipCreationForm";
import { useMembershipActions, useMembershipDetail } from "@/hooks/useMemberships";

function NewMembershipContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const renewId = searchParams.get("renew");
  const t = useTranslations("studentMembershipsPage");
  const tCommon = useTranslations("common");

  const { createSelfMembership, renewMembership } = useMembershipActions();

  // When renewing, fetch the source membership to pre-fill plan/program.
  // Pass a non-empty string always; the hook result is ignored when renewId is null.
  const { membership: sourceMembership, loading: sourceLoading } = useMembershipDetail(renewId ?? "00000000-0000-0000-0000-000000000000");

  const isRenewing = !!renewId;
  const initialProgramId = isRenewing ? sourceMembership?.programId : undefined;
  const initialPlanId = isRenewing ? sourceMembership?.planId : undefined;
  const renewBanner = isRenewing && sourceMembership
    ? `Renewing plan "${sourceMembership.planName}" — your existing membership will be reactivated.`
    : undefined;

  async function handleSubmit(planId: string, file: File) {
    if (isRenewing && renewId) {
      // Reactivate the same membership (same UUID)
      await renewMembership(renewId, file);
      router.push(`/student/memberships/${renewId}`);
    } else {
      const membership = await createSelfMembership({ planId }, file);
      router.push(`/student/memberships/${membership.id}`);
    }
  }

  if (isRenewing && sourceLoading) {
    return <p className="py-8 text-center text-sm text-k-muted">{tCommon("loading")}</p>;
  }

  return (
    <div className="max-w-xl">
      <nav className="mb-6 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
        <Link href="/student/memberships" className="hover:text-k-subtle">
          {t("title")}
        </Link>
        <span className="mx-2">/</span>
        <span className="text-k-subtle">{isRenewing ? t("renew") : t("newButton")}</span>
      </nav>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-2">
        {isRenewing ? t("renew") : t("newButton")}
      </h1>
      <p className="font-[var(--font-mono)] text-xs text-k-muted mb-6">
        {isRenewing
          ? "Your renewed membership will start pending payment validation."
          : "Select a plan from your enrolled program, attach your payment proof, and submit."}
      </p>

      <Card padding="md">
        <StudentMembershipCreationForm
          initialProgramId={initialProgramId}
          initialPlanId={initialPlanId}
          renewBanner={renewBanner}
          onSubmit={handleSubmit}
          onCancel={() => router.push("/student/memberships")}
        />
      </Card>
    </div>
  );
}

export default function NewMembershipPage() {
  return (
    <Suspense fallback={<p className="py-8 text-center text-sm text-k-muted">{/* fallback */}</p>}>
      <NewMembershipContent />
    </Suspense>
  );
}
