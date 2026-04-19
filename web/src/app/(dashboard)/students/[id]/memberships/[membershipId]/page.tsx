"use client";

import { use } from "react";
import Link from "next/link";
import { useMembershipDetail } from "@/hooks/useMemberships";
import { useStudentDetail } from "@/hooks/useStudents";
import { useAuth } from "@/hooks/useAuth";
import MembershipDetail from "@/components/memberships/MembershipDetail";
import { PaymentProofTimeline } from "@/components/payment-proofs/PaymentProofTimeline";

interface Props {
  params: Promise<{ id: string; membershipId: string }>;
}

export default function MembershipDetailPage({ params }: Props) {
  const { id: studentId, membershipId } = use(params);
  const { membership, loading, error, refetch } = useMembershipDetail(membershipId);
  const { student } = useStudentDetail(studentId);
  const studentName = student ? `${student.firstName} ${student.lastName}` : studentId;
  const { user } = useAuth();

  const isAdmin = (user?.roles.includes("ADMIN") || user?.roles.includes("SUPERADMIN")) ?? false;

  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/students" className="hover:text-gray-700 hover:underline">
          Students
        </Link>
        <span className="mx-2">/</span>
        <Link href={`/students/${studentId}`} className="hover:text-gray-700 hover:underline">
          {studentName}
        </Link>
        <span className="mx-2">/</span>
        <Link
          href={`/students/${studentId}/memberships`}
          className="hover:text-gray-700 hover:underline"
        >
          Memberships
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
            isAdmin={isAdmin}
          />
          <PaymentProofTimeline membershipId={membershipId} membershipStatus={membership.status} />
        </div>
      )}
    </div>
  );
}
