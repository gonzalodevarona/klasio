"use client";

import Link from "next/link";
import { AlertTriangle } from "lucide-react";
import { useTranslations } from "next-intl";
import { useMyMemberships } from "@/hooks/useMemberships";
import { useMyEnrollments } from "@/hooks/useMyEnrollments";
import { useMyRegistrations } from "@/hooks/useMyRegistrations";
import HourBalance from "@/components/memberships/HourBalance";
import { UnlimitedBadge } from "@/components/memberships/UnlimitedBadge";
import MembershipStatusBadge from "@/components/memberships/MembershipStatusBadge";
import { Badge, Button, Card } from "@/components/ui";
import { todayInTenantZone, formatSessionDate } from "@/lib/attendanceConstants";

function formatDate(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString();
}

export default function StudentDashboard() {
  const t = useTranslations("studentDashboard");
  const today = todayInTenantZone();

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
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
        <p className="font-[var(--font-mono)] text-xs text-k-muted mt-1">
          {t("subtitle")}
        </p>
      </div>

      <Card padding="md">
        <h2 className="text-base font-semibold text-k-dark mb-4">
          {t("activeMembership")}
        </h2>
        {membershipsLoading ? (
          <p className="text-sm text-k-muted">{t("loading")}</p>
        ) : activeMembership ? (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-k-dark">
                {activeMembership.planName}
              </span>
              <MembershipStatusBadge status={activeMembership.status} />
            </div>
            {activeMembership.modality === "UNLIMITED" ? (
              <UnlimitedBadge expiresAt={new Date(activeMembership.expirationDate)} />
            ) : (
              <HourBalance
                available={activeMembership.availableHours ?? 0}
                purchased={activeMembership.purchasedHours ?? 0}
              />
            )}
            <p className="text-xs text-k-muted font-mono">
              {t("membershipPeriod")} {formatDate(activeMembership.startDate)} → {formatDate(activeMembership.expirationDate)}
            </p>
            <Link
              href={`/student/memberships/${activeMembership.id}`}
              className="inline-block text-sm text-k-subtle hover:text-k-dark font-medium"
            >
              {t("viewDetails")}
            </Link>
          </div>
        ) : (
          <p className="text-sm text-k-muted">{t("noMembership")}</p>
        )}
      </Card>

      <Card padding="md">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-base font-semibold text-k-dark">
            {t("enrollmentsTitle")}
          </h2>
          <Link
            href="/student/enrollments"
            className="text-xs text-k-subtle hover:text-k-dark font-medium"
          >
            {t("viewAll")}
          </Link>
        </div>
        {enrollmentsLoading ? (
          <p className="text-sm text-k-muted">{t("loading")}</p>
        ) : enrollments.length === 0 ? (
          <p className="text-sm text-k-muted">{t("noEnrollments")}</p>
        ) : (
          <ul className="space-y-2">
            {enrollments.slice(0, 3).map((e) => (
              <li key={e.id} className="flex items-center justify-between text-sm">
                <span className="text-k-dark">{e.programName}</span>
                <div className="flex items-center gap-2">
                  <span className="text-xs text-k-muted">{e.level}</span>
                  <Badge
                    variant={e.status === "ACTIVE" ? "active" : "inactive"}
                    label={e.status}
                    small
                  />
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Card padding="md">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-base font-semibold text-k-dark">
            {t("upcomingRegistrations")}
          </h2>
          <Link
            href="/student/classes"
            className="text-xs text-k-subtle hover:text-k-dark font-medium"
          >
            {t("viewAll")}
          </Link>
        </div>
        {registrationsLoading ? (
          <p className="text-sm text-k-muted">{t("loading")}</p>
        ) : upcomingRegistrations.length === 0 ? (
          <p className="text-sm text-k-muted">{t("noRegistrations")}</p>
        ) : (
          <ul className="space-y-2">
            {upcomingRegistrations.map((r) => (
              <li key={r.id} className="flex items-center justify-between text-sm">
                <div className="flex items-center gap-3">
                  <span className="text-k-dark">
                    {formatSessionDate(r.sessionDate)}
                  </span>
                  <span className="text-k-muted">
                    {r.sessionStartTime.slice(0, 5)} – {r.sessionEndTime.slice(0, 5)}
                  </span>
                  {r.sessionStatus === "ALERTED" && (
                    <span
                      title={r.sessionAlertReason ?? t("alertTooltip")}
                      className="inline-flex text-k-warn-text"
                    >
                      <AlertTriangle className="w-4 h-4" />
                    </span>
                  )}
                </div>
                <Badge
                  variant={
                    r.level === "BEGINNER"
                      ? "beginner"
                      : r.level === "INTERMEDIATE"
                      ? "intermediate"
                      : r.level === "ADVANCED"
                      ? "advanced"
                      : "info"
                  }
                  label={r.level}
                  small
                />
              </li>
            ))}
          </ul>
        )}
      </Card>

      <div className="grid grid-cols-3 gap-3">
        {[
          { label: t("quickLinksMemberships"), href: "/student/memberships" },
          { label: t("quickLinksEnrollments"), href: "/student/enrollments" },
          { label: t("quickLinksClasses"), href: "/student/classes" },
        ].map(({ label, href }) => (
          <Button key={href} variant="outline" asChild>
            <Link href={href}>{label}</Link>
          </Button>
        ))}
      </div>
    </div>
  );
}
