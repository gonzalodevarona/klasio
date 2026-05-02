"use client";

import Link from "next/link";
import { AlertTriangle } from "lucide-react";
import { useTranslations, useLocale } from "next-intl";
import { useMyMemberships } from "@/hooks/useMemberships";
import { useMyEnrollments } from "@/hooks/useMyEnrollments";
import { useMyRegistrations } from "@/hooks/useMyRegistrations";
import HourBalance from "@/components/memberships/HourBalance";
import MembershipStatusBadge from "@/components/memberships/MembershipStatusBadge";
import { Badge } from "@/components/ui";
import { todayInTenantZone, formatSessionDate } from "@/lib/attendanceConstants";

function formatDate(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString();
}

export default function StudentDashboard() {
  const t = useTranslations("studentDashboard");
  const locale = useLocale();
  const today = todayInTenantZone();

  const { memberships, loading: membershipsLoading } = useMyMemberships();
  const { enrollments } = useMyEnrollments();
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
    <div className="space-y-5">
      {/* Page heading */}
      <div>
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">
          {t("title")}
        </h1>
        <p
          className="text-xs text-k-muted mt-1"
          style={{ fontFamily: "var(--font-mono)" }}
        >
          {t("subtitle")}
        </p>
      </div>

      {/* ── Membership hero card (dark) ── */}
      <div
        className="relative overflow-hidden rounded-[20px] px-8 py-7"
        style={{ background: "#0A0A0A" }}
      >
        {/* Decorative circle */}
        <div
          className="pointer-events-none absolute -top-10 -right-10 w-44 h-44 rounded-full"
          style={{ background: "rgba(202,255,77,0.04)" }}
        />

        {/* Status badge top-right */}
        <div className="absolute top-5 right-6">
          {activeMembership && (
            <MembershipStatusBadge status={activeMembership.status} />
          )}
        </div>

        {/* Label */}
        <p
          className="text-[10px] uppercase tracking-[0.12em] mb-2"
          style={{ fontFamily: "var(--font-mono)", color: "#4A4A48" }}
        >
          {t("activeMembership")}
        </p>

        {membershipsLoading ? (
          <p className="text-sm" style={{ color: "#4A4A48" }}>
            {t("loading")}
          </p>
        ) : activeMembership ? (
          <>
            <h2
              className="text-[22px] font-extrabold tracking-[-0.02em] mb-1"
              style={{ color: "#FAFAF8" }}
            >
              {activeMembership.planName}
            </h2>

            {/* HourBalance renders the big volt number + progress bar */}
            <div className="mt-5">
              <HourBalance
                available={activeMembership.availableHours ?? 0}
                purchased={activeMembership.purchasedHours ?? 0}
              />
            </div>

            <p
              className="text-[11px] mt-3"
              style={{ fontFamily: "var(--font-mono)", color: "#4A4A48" }}
            >
              {t("membershipPeriod")}{" "}
              {formatDate(activeMembership.startDate)} →{" "}
              {formatDate(activeMembership.expirationDate)}
            </p>

            <Link
              href={`/student/memberships/${activeMembership.id}`}
              className="inline-block mt-3 text-xs font-semibold transition-colors"
              style={{ color: "#CAFF4D" }}
            >
              {t("viewDetails")} →
            </Link>
          </>
        ) : (
          <p className="text-sm" style={{ color: "#4A4A48" }}>
            {t("noMembership")}
          </p>
        )}
      </div>

      {/* ── Upcoming registrations card ── */}
      <div className="rounded-k-lg border border-k-border bg-k-surface px-6 py-5">
        <div className="flex items-center justify-between mb-4">
          <p
            className="text-[10px] uppercase tracking-[0.1em] text-k-muted"
            style={{ fontFamily: "var(--font-mono)" }}
          >
            {t("upcomingRegistrations")}
          </p>
          <Link
            href="/student/classes"
            className="text-xs font-semibold text-k-subtle hover:text-k-dark transition-colors"
          >
            {t("viewAll")} →
          </Link>
        </div>

        {registrationsLoading ? (
          <p className="text-sm text-k-muted">{t("loading")}</p>
        ) : upcomingRegistrations.length === 0 ? (
          <p className="text-sm text-k-muted">{t("noRegistrations")}</p>
        ) : (
          <div className="flex flex-col gap-2.5">
            {upcomingRegistrations.map((r) => (
              <div
                key={r.id}
                className="flex items-center justify-between"
              >
                {/* Date badge + time */}
                <div className="flex items-center gap-3">
                  <div className="w-11 h-11 rounded-[10px] bg-k-bg flex flex-col items-center justify-center shrink-0">
                    <span
                      className="text-[9px] uppercase tracking-[0.06em] text-k-muted"
                      style={{ fontFamily: "var(--font-mono)" }}
                    >
                      {formatSessionDate(r.sessionDate, locale).split(" ")[0]}
                    </span>
                    <span className="text-base font-extrabold text-k-dark leading-none">
                      {formatSessionDate(r.sessionDate, locale).split(" ")[1]}
                    </span>
                  </div>
                  <div>
                    <div className="text-sm font-semibold text-k-dark">
                      {r.sessionStartTime.slice(0, 5)} –{" "}
                      {r.sessionEndTime.slice(0, 5)}
                    </div>
                    {r.sessionStatus === "ALERTED" && (
                      <div className="flex items-center gap-1 mt-0.5">
                        <AlertTriangle className="w-3 h-3 text-k-warn-text" />
                        <span className="text-[10px] text-k-warn-text">
                          {r.sessionAlertReason ?? t("alertTooltip")}
                        </span>
                      </div>
                    )}
                  </div>
                </div>

                <Badge
                  variant={
                    r.level === "BEGINNER"
                      ? "beginner"
                      : r.level === "INTERMEDIATE"
                      ? "intermediate"
                      : r.level === "ADVANCED"
                      ? "advanced"
                      : "inactive"
                  }
                  label={r.level}
                  small
                />
              </div>
            ))}
          </div>
        )}
      </div>

      {/* ── Quick stats row ── */}
      <div className="grid grid-cols-3 gap-3">
        {[
          {
            label: t("quickLinksMemberships"),
            href: "/student/memberships",
            value: memberships.length,
            sub: "membresías",
          },
          {
            label: t("quickLinksEnrollments"),
            href: "/student/enrollments",
            value: enrollments.filter((e) => e.status === "ACTIVE").length,
            sub: "clases activas",
          },
          {
            label: t("quickLinksClasses"),
            href: "/student/classes",
            value: upcomingRegistrations.length,
            sub: "próximas sesiones",
          },
        ].map(({ label, href, value, sub }) => (
          <Link
            key={href}
            href={href}
            className="rounded-k-lg border border-k-border bg-k-surface px-5 py-4 hover:border-k-volt transition-colors group block"
          >
            <p
              className="text-[10px] uppercase tracking-[0.1em] text-k-muted mb-2"
              style={{ fontFamily: "var(--font-mono)" }}
            >
              {label}
            </p>
            <p className="text-[32px] font-extrabold tracking-[-0.03em] leading-none text-k-dark mb-1">
              {value}
            </p>
            <p className="text-xs text-k-muted">{sub}</p>
          </Link>
        ))}
      </div>
    </div>
  );
}
