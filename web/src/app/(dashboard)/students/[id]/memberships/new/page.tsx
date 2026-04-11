"use client";

import { use, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
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
  const { createMembership } = useMembershipActions();
  const { student, loading: studentLoading } = useStudentDetail(studentId);

  const activeEnrollments = student?.enrollments?.filter((e) => e.status === "ACTIVE") ?? [];
  const [selectedProgramId, setSelectedProgramId] = useState<string>("");

  // Scope plans to the selected program instead of fetching all tenant plans
  const { plans, loading: plansLoading } = useProgramPlansByProgram(
    selectedProgramId || null,
    "HOURS_BASED"
  );

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

      {studentLoading ? (
        <div className="py-8 text-center text-sm text-gray-500">Loading student…</div>
      ) : activeEnrollments.length === 0 ? (
        <div className="rounded-md bg-yellow-50 border border-yellow-200 p-4 text-sm text-yellow-800">
          This student has no active program enrollments. Enroll them in a program first.
        </div>
      ) : (
        <div className="rounded-md border border-gray-200 p-6 bg-white space-y-5">
          {/* Program selector — scopes plan list */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Program enrollment</label>
            <select
              value={selectedProgramId}
              onChange={(e) => setSelectedProgramId(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">— Select a program —</option>
              {activeEnrollments.map((e) => (
                <option key={e.programId} value={e.programId}>
                  {e.programName} ({e.level})
                </option>
              ))}
            </select>
          </div>

          {selectedProgramId && (
            plansLoading ? (
              <div className="py-4 text-center text-sm text-gray-500">Loading plans…</div>
            ) : plans.length === 0 ? (
              <div className="rounded-md bg-yellow-50 border border-yellow-200 p-3 text-sm text-yellow-800">
                No active HOURS_BASED plans found for this program.
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
      )}
    </div>
  );
}
