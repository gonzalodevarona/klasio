"use client";

import { Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import StudentMembershipCreationForm from "@/components/memberships/StudentMembershipCreationForm";
import { useMembershipActions, useMembershipDetail } from "@/hooks/useMemberships";

function NewMembershipContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const renewId = searchParams.get("renew");

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
    return <p className="py-8 text-center text-sm text-gray-500">Loading membership...</p>;
  }

  return (
    <div className="max-w-xl">
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/student/memberships" className="hover:text-gray-700 hover:underline">
          My Memberships
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">{isRenewing ? "Renew" : "New Membership"}</span>
      </nav>

      <h1 className="text-2xl font-bold text-gray-900 mb-2">
        {isRenewing ? "Renew Membership" : "Subscribe to a Plan"}
      </h1>
      <p className="text-sm text-gray-500 mb-6">
        {isRenewing
          ? "Your renewed membership will start pending payment validation."
          : "Select a plan from your enrolled program, attach your payment proof, and submit."}
      </p>

      <div className="rounded-lg border border-gray-200 bg-white p-6">
        <StudentMembershipCreationForm
          initialProgramId={initialProgramId}
          initialPlanId={initialPlanId}
          renewBanner={renewBanner}
          onSubmit={handleSubmit}
          onCancel={() => router.push("/student/memberships")}
        />
      </div>
    </div>
  );
}

export default function NewMembershipPage() {
  return (
    <Suspense fallback={<p className="py-8 text-center text-sm text-gray-500">Loading...</p>}>
      <NewMembershipContent />
    </Suspense>
  );
}
