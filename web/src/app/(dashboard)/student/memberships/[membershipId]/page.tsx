"use client";

import { use } from "react";
import Link from "next/link";
import { useMembershipDetail } from "@/hooks/useMemberships";
import MembershipDetail from "@/components/memberships/MembershipDetail";
import { PaymentProofPanel } from "@/components/payment-proofs/PaymentProofPanel";
import { PaymentProofTimeline } from "@/components/payment-proofs/PaymentProofTimeline";

interface Props {
  params: Promise<{ membershipId: string }>;
}

export default function StudentMembershipDetailPage({ params }: Props) {
  const { membershipId } = use(params);
  const { membership, loading, error, refetch } = useMembershipDetail(membershipId);

  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/student/memberships" className="hover:text-gray-700 hover:underline">
          My Memberships
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">Detail</span>
      </nav>

      {loading && (
        <div className="py-8 text-center text-sm text-gray-500">Loading membership...</div>
      )}

      {error && (
        <div className="rounded-md bg-red-50 p-4 text-sm text-red-700 border border-red-200">
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
          {membership.status === "EXPIRED" && (
            <PaymentProofPanel membershipId={membershipId} membershipStatus={membership.status} />
          )}
          <PaymentProofTimeline membershipId={membershipId} membershipStatus={membership.status} />
        </div>
      )}
    </div>
  );
}
