"use client";

import Link from "next/link";
import { AlertTriangle } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { useMyMemberships } from "@/hooks/useMemberships";
import { useMyEnrollments } from "@/hooks/useMyEnrollments";
import { useMyRegistrations } from "@/hooks/useMyRegistrations";
import HourBalance from "@/components/memberships/HourBalance";
import MembershipStatusBadge from "@/components/memberships/MembershipStatusBadge";
import { todayInTenantZone } from "@/lib/attendanceConstants";

function formatDate(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString();
}

export default function StudentDashboard() {
  const today = todayInTenantZone();

  const { user } = useAuth();
  const { memberships, loading: membershipsLoading } = useMyMemberships();
  const { enrollments, loading: enrollmentsLoading } = useMyEnrollments();
  const { registrations, loading: registrationsLoading } = useMyRegistrations({
    status: "REGISTERED",
    from: today,
  });
  const upcomingRegistrations = registrations.slice(0, 3);

  const activeMembership = memberships.find(
    (m) =>
      m.status === "ACTIVE" ||
      m.status === "PENDING_PAYMENT" ||
      m.status === "PENDING_PAYMENT_VALIDATION" ||
      m.status === "PENDING_MANAGER_ACTIVATION"
  );

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="mt-1 text-sm text-gray-500">
          Welcome back.
        </p>
      </div>

      {/* Active membership card */}
      <div className="rounded-lg border border-gray-200 bg-white p-5">
        <h2 className="text-sm font-semibold text-gray-700 uppercase tracking-wide mb-3">
          Active Membership
        </h2>
        {membershipsLoading ? (
          <p className="text-sm text-gray-400">Loading…</p>
        ) : activeMembership ? (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-gray-900">
                {activeMembership.planName}
              </span>
              <MembershipStatusBadge status={activeMembership.status} />
            </div>
            <HourBalance
              available={activeMembership.availableHours}
              purchased={activeMembership.purchasedHours}
            />
            <p className="text-xs text-gray-400">
              Expires: {formatDate(activeMembership.expirationDate)}
            </p>
            <Link
              href={`/student/memberships/${activeMembership.id}`}
              className="inline-block text-sm text-indigo-600 hover:text-indigo-800 font-medium"
            >
              View details →
            </Link>
          </div>
        ) : (
          <p className="text-sm text-gray-400">No active membership.</p>
        )}
      </div>

      {/* Enrollments summary */}
      <div className="rounded-lg border border-gray-200 bg-white p-5">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-sm font-semibold text-gray-700 uppercase tracking-wide">
            Enrollments
          </h2>
          <Link
            href="/student/enrollments"
            className="text-xs text-indigo-600 hover:text-indigo-800"
          >
            View all
          </Link>
        </div>
        {enrollmentsLoading ? (
          <p className="text-sm text-gray-400">Loading…</p>
        ) : enrollments.length === 0 ? (
          <p className="text-sm text-gray-400">No enrollments yet.</p>
        ) : (
          <ul className="space-y-2">
            {enrollments.slice(0, 3).map((e) => (
              <li key={e.id} className="flex items-center justify-between text-sm">
                <span className="text-gray-900">{e.programName}</span>
                <div className="flex items-center gap-2">
                  <span className="text-xs text-gray-400">{e.level}</span>
                  <span
                    className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                      e.status === "ACTIVE"
                        ? "bg-green-100 text-green-700"
                        : "bg-gray-100 text-gray-500"
                    }`}
                  >
                    {e.status}
                  </span>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* Upcoming registrations */}
      <div className="rounded-lg border border-gray-200 bg-white p-5">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-sm font-semibold text-gray-700 uppercase tracking-wide">
            Upcoming Registrations
          </h2>
          <Link
            href="/student/registrations"
            className="text-xs text-indigo-600 hover:text-indigo-800"
          >
            View all
          </Link>
        </div>
        {registrationsLoading ? (
          <p className="text-sm text-gray-400">Loading…</p>
        ) : upcomingRegistrations.length === 0 ? (
          <p className="text-sm text-gray-400">No upcoming registrations.</p>
        ) : (
          <ul className="space-y-2">
            {upcomingRegistrations.map((r) => (
              <li key={r.id} className="flex items-center justify-between text-sm">
                <div className="flex items-center gap-3">
                  <span className="text-gray-900">
                    {new Date(r.sessionDate).toLocaleDateString()}
                  </span>
                  <span className="text-gray-400">
                    {r.sessionStartTime.slice(0, 5)} – {r.sessionEndTime.slice(0, 5)}
                  </span>
                  {r.sessionStatus === "ALERTED" && (
                    <span
                      title={r.sessionAlertReason ?? "Alert issued for this session"}
                      className="inline-flex text-amber-600"
                    >
                      <AlertTriangle className="w-4 h-4" />
                    </span>
                  )}
                </div>
                <span
                  className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                    r.level === "BEGINNER"
                      ? "bg-blue-100 text-blue-700"
                      : r.level === "INTERMEDIATE"
                      ? "bg-yellow-100 text-yellow-700"
                      : r.level === "ADVANCED"
                      ? "bg-red-100 text-red-700"
                      : "bg-gray-100 text-gray-600"
                  }`}
                >
                  {r.level}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* Quick links */}
      <div className="grid grid-cols-3 gap-3">
        {[
          { label: "Memberships", href: "/student/memberships" },
          { label: "Enrollments", href: "/student/enrollments" },
          { label: "Classes", href: "/student/classes" },
        ].map(({ label, href }) => (
          <Link
            key={href}
            href={href}
            className="flex items-center justify-center rounded-lg border border-gray-200 bg-white px-4 py-3 text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors"
          >
            {label}
          </Link>
        ))}
      </div>
    </div>
  );
}
