"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useAdminDashboard, DashboardStudent } from "@/hooks/useAdminDashboard";

// ── HoursBar ──────────────────────────────────────────────────────────────
function HoursBar({ available, purchased }: { available: number; purchased: number }) {
  const consumed = purchased - available;
  const pct = purchased > 0 ? Math.min(100, Math.round((consumed / purchased) * 100)) : 0;
  const barColor =
    pct <= 33 ? "#CAFF4D" : pct <= 66 ? "#8AE800" : pct <= 85 ? "#FFC107" : "#CC2200";
  return (
    <div className="flex items-center gap-2">
      <div className="w-20 h-1 rounded-full overflow-hidden" style={{ background: "#EBEBEA" }}>
        <div className="h-full rounded-full" style={{ width: `${pct}%`, background: barColor }} />
      </div>
      <span className="text-[11px]" style={{ fontFamily: "var(--font-mono)", color: "#4A4A48" }}>
        {available}/{purchased}h
      </span>
    </div>
  );
}

// ── Status badge ──────────────────────────────────────────────────────────
const STATUS_STYLE: Record<string, { bg: string; color: string; border?: string }> = {
  ACTIVE:   { bg: "#CAFF4D", color: "#2A4A00" },
  EXPIRING: { bg: "#FFF0C2", color: "#8A5A00" },
  INACTIVE: { bg: "#F4F4F2", color: "#6A6A68", border: "#DDDDD8" },
  NEW:      { bg: "#E8F4FF", color: "#0066BB" },
  EXPIRED:  { bg: "#FFE8E8", color: "#CC2200" },
};

function StatusBadge({ status, label }: { status: string | null; label: string }) {
  if (!status) return <span style={{ color: "#9A9A98", fontSize: 12 }}>—</span>;
  const s = STATUS_STYLE[status.toUpperCase()] ?? { bg: "#F4F4F2", color: "#6A6A68" };
  return (
    <span
      className="inline-flex items-center rounded-full text-[11px] font-semibold px-2.5 py-0.5 whitespace-nowrap"
      style={{
        background: s.bg,
        color: s.color,
        border: s.border ? `1px solid ${s.border}` : undefined,
      }}
    >
      {label}
    </span>
  );
}

// ── Stat card ─────────────────────────────────────────────────────────────
function StatCard({
  label,
  value,
  sub,
  subColor,
  dark,
}: {
  label: string;
  value: string | number;
  sub?: string;
  subColor?: string;
  dark?: boolean;
}) {
  return (
    <div
      className="rounded-[16px] px-7 py-6 flex flex-col gap-1.5"
      style={{
        background: dark ? "#0A0A0A" : "#FAFAF8",
        border: dark ? "none" : "1.5px solid #DDDDD8",
      }}
    >
      <span
        className="text-[10px] uppercase tracking-[0.1em]"
        style={{ fontFamily: "var(--font-mono)", color: dark ? "#666" : "#9A9A98" }}
      >
        {label}
      </span>
      <span
        className="text-[40px] font-extrabold tracking-[-0.03em] leading-none"
        style={{ color: dark ? "#CAFF4D" : "#0A0A0A" }}
      >
        {value}
      </span>
      {sub && (
        <span className="text-xs font-medium" style={{ color: subColor ?? (dark ? "#CAFF4D" : "#9A9A98") }}>
          {sub}
        </span>
      )}
    </div>
  );
}

// ── Status label lookup ───────────────────────────────────────────────────
const STATUS_KEY_MAP: Record<string, string> = {
  ACTIVE:   "statusActive",
  INACTIVE: "statusInactive",
  EXPIRED:  "statusExpired",
  EXPIRING: "statusExpiring",
  NEW:      "statusNew",
};

// ── Page ──────────────────────────────────────────────────────────────────
export default function AdminDashboard() {
  const t = useTranslations("adminDashboard");
  const { data, loading, error } = useAdminDashboard();
  const [activeFilter, setActiveFilter] = useState("all");

  const FILTERS = [
    { id: "all",      label: t("filterAll"),      bg: "#0A0A0A", color: "#FAFAF8" },
    { id: "ACTIVE",   label: t("filterActive"),   bg: "#CAFF4D", color: "#2A4A00" },
    { id: "EXPIRING", label: t("filterExpiring"), bg: "#FFF0C2", color: "#8A5A00" },
    { id: "INACTIVE", label: t("filterInactive"), bg: "#F4F4F2", color: "#6A6A68" },
    { id: "EXPIRED",  label: t("filterExpired"),  bg: "#FFE8E8", color: "#CC2200" },
    { id: "NEW",      label: t("filterNew"),      bg: "#E8F4FF", color: "#0066BB" },
  ];

  const filteredStudents: DashboardStudent[] = !data
    ? []
    : activeFilter === "all"
    ? data.students
    : data.students.filter((s) => s.membershipStatus?.toUpperCase() === activeFilter);

  return (
    <div>
      {/* Header */}
      <div className="mb-7">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em]" style={{ color: "#0A0A0A" }}>
          {t("title")}
        </h1>
        <p className="text-xs mt-1" style={{ fontFamily: "var(--font-mono)", color: "#9A9A98" }}>
          {t("subtitle")}
        </p>
      </div>

      {/* Quick actions */}
      <div className="flex gap-2.5 flex-wrap mb-7">
        <Link
          href="/payment-proofs"
          className="inline-flex items-center rounded-[8px] px-4 py-2 text-sm font-semibold"
          style={{ background: "#0A0A0A", color: "#FAFAF8" }}
        >
          {t("actionValidatePayment")}
        </Link>
        <Link
          href="/classes"
          className="inline-flex items-center rounded-[8px] px-4 py-2 text-sm font-semibold"
          style={{ background: "#CAFF4D", color: "#0A0A0A" }}
        >
          {t("actionRegisterClass")}
        </Link>
        <Link
          href="/students"
          className="inline-flex items-center rounded-[8px] px-4 py-2 text-sm font-semibold border"
          style={{ background: "transparent", color: "#0A0A0A", borderColor: "#DDDDD8" }}
        >
          {t("actionViewStudents")}
        </Link>
        <Link
          href="/programs"
          className="inline-flex items-center rounded-[8px] px-4 py-2 text-sm font-semibold"
          style={{ background: "#F4F4F2", color: "#4A4A48" }}
        >
          {t("actionPrograms")}
        </Link>
      </div>

      {/* KPI cards */}
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          {[...Array(4)].map((_, i) => (
            <div
              key={i}
              className="rounded-[16px] h-32 animate-pulse"
              style={{ background: i === 1 ? "#1A1A1A" : "#EBEBEA" }}
            />
          ))}
        </div>
      ) : error ? (
        <div
          className="rounded-[8px] p-4 text-sm mb-8"
          style={{ background: "#FFE8E8", color: "#CC2200", border: "1px solid #FFCCCC" }}
        >
          {t("error")}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          <StatCard
            label={t("kpiStudents")}
            value={data?.studentCount ?? 0}
            sub={
              (data?.newStudentsThisMonth ?? 0) > 0
                ? t("kpiNewThisMonth", { count: data!.newStudentsThisMonth })
                : undefined
            }
            subColor="#2A8A00"
          />
          <StatCard
            label={t("kpiHoursConsumed")}
            value={(data?.totalHoursConsumed ?? 0).toLocaleString()}
            sub={t("kpiHoursConsumedSub")}
            dark
          />
          <StatCard
            label={t("kpiPendingPayments")}
            value={data?.pendingPaymentProofs ?? 0}
            sub={
              (data?.pendingPaymentProofs ?? 0) > 0 ? t("kpiPendingAction") : t("kpiUpToDate")
            }
            subColor={(data?.pendingPaymentProofs ?? 0) > 0 ? "#C87000" : undefined}
          />
          <StatCard
            label={t("kpiActivePrograms")}
            value={data?.activeProgramCount ?? 0}
          />
        </div>
      )}

      {/* Attendance control table */}
      <div
        className="rounded-[16px] p-6"
        style={{ background: "#FAFAF8", border: "1.5px solid #DDDDD8" }}
      >
        <div className="flex items-center justify-between mb-5">
          <h2
            className="text-[15px] font-bold tracking-[-0.01em]"
            style={{ color: "#0A0A0A" }}
          >
            {t("attendanceTitle")}
          </h2>
          <Link
            href="/classes"
            className="inline-flex items-center rounded-[8px] px-3 py-1.5 text-xs font-semibold"
            style={{ background: "#CAFF4D", color: "#0A0A0A" }}
          >
            {t("attendanceStartClass")}
          </Link>
        </div>

        {/* Filter pills */}
        <div className="flex gap-2 flex-wrap mb-5">
          {FILTERS.map((f) => {
            const isActive = activeFilter === f.id;
            return (
              <button
                key={f.id}
                onClick={() => setActiveFilter(f.id)}
                className="text-xs font-semibold px-3.5 py-1.5 rounded-full border-none cursor-pointer"
                style={{
                  background: isActive ? f.bg : "#F4F4F2",
                  color: isActive ? f.color : "#4A4A48",
                }}
              >
                {f.label}
              </button>
            );
          })}
        </div>

        {/* Table */}
        {loading ? (
          <div className="space-y-2">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="h-10 rounded animate-pulse" style={{ background: "#EBEBEA" }} />
            ))}
          </div>
        ) : filteredStudents.length === 0 ? (
          <p className="text-sm py-6 text-center" style={{ color: "#9A9A98" }}>
            {t("attendanceEmpty")}
          </p>
        ) : (
          <div className="overflow-x-auto rounded-[12px]" style={{ border: "1.5px solid #DDDDD8" }}>
            <table className="w-full border-collapse">
              <thead style={{ background: "#F4F4F2", borderBottom: "1.5px solid #DDDDD8" }}>
                <tr>
                  {(
                    [
                      t("tableStudent"),
                      t("tableProgram"),
                      t("tableHours"),
                      t("tableStatus"),
                    ] as string[]
                  ).map((h) => (
                    <th
                      key={h}
                      className="text-left px-4 py-2.5 whitespace-nowrap"
                      style={{
                        fontFamily: "var(--font-mono)",
                        fontSize: 10,
                        letterSpacing: "0.1em",
                        textTransform: "uppercase",
                        color: "#9A9A98",
                        fontWeight: 500,
                      }}
                    >
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {filteredStudents.map((s, i) => {
                  const statusKey = s.membershipStatus
                    ? STATUS_KEY_MAP[s.membershipStatus.toUpperCase()]
                    : undefined;
                  const statusLabel = statusKey ? t(statusKey as Parameters<typeof t>[0]) : "—";
                  return (
                    <tr
                      key={s.id}
                      style={{
                        borderBottom:
                          i < filteredStudents.length - 1 ? "1px solid #EBEBEA" : "none",
                        background: "white",
                      }}
                      className="hover:bg-[#FAFAF8] transition-colors"
                    >
                      <td
                        className="px-4 py-3 text-sm font-semibold whitespace-nowrap"
                        style={{ color: "#0A0A0A" }}
                      >
                        {s.name}
                      </td>
                      <td
                        className="px-4 py-3 text-sm whitespace-nowrap"
                        style={{ color: "#9A9A98" }}
                      >
                        {s.programName ?? "—"}
                      </td>
                      <td className="px-4 py-3">
                        {s.availableHours != null && s.purchasedHours != null ? (
                          <HoursBar available={s.availableHours} purchased={s.purchasedHours} />
                        ) : (
                          <span style={{ color: "#9A9A98", fontSize: 12 }}>—</span>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        <StatusBadge status={s.membershipStatus} label={statusLabel} />
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
