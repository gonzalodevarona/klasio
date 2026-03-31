"use client";

import { use } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import MembershipForm from "@/components/memberships/MembershipForm";
import { useMembershipActions } from "@/hooks/useMemberships";
import { useProgramPlans } from "@/hooks/usePrograms";
import { CreateMembershipRequest } from "@/lib/types/membership";

interface Props {
  params: Promise<{ id: string }>;
}

export default function NewMembershipPage({ params }: Props) {
  const { id: studentId } = use(params);
  const router = useRouter();
  const { createMembership } = useMembershipActions();
  // Only HOURS_BASED plans can be purchased as memberships
  const { plans, loading: plansLoading } = useProgramPlans("HOURS_BASED");

  async function handleSubmit(data: CreateMembershipRequest) {
    await createMembership(data);
    router.push(`/students/${studentId}/memberships`);
  }

  return (
    <div className="max-w-xl">
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/students" className="hover:text-gray-700 hover:underline">
          Students
        </Link>
        <span className="mx-2">/</span>
        <Link href={`/students/${studentId}`} className="hover:text-gray-700 hover:underline">
          Student
        </Link>
        <span className="mx-2">/</span>
        <Link
          href={`/students/${studentId}/memberships`}
          className="hover:text-gray-700 hover:underline"
        >
          Memberships
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">New</span>
      </nav>

      <h1 className="text-2xl font-bold text-gray-900 mb-6">Create Membership</h1>

      {plansLoading ? (
        <div className="py-8 text-center text-sm text-gray-500">Loading plans...</div>
      ) : (
        <div className="rounded-md border border-gray-200 p-6 bg-white">
          <MembershipForm
            studentId={studentId}
            plans={plans}
            onSubmit={handleSubmit}
            onCancel={() => router.push(`/students/${studentId}/memberships`)}
          />
        </div>
      )}
    </div>
  );
}
