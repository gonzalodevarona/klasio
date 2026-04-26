"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { useMyMemberships } from "@/hooks/useMemberships";
import MembershipStatusBadge from "@/components/memberships/MembershipStatusBadge";
import HourBalance from "@/components/memberships/HourBalance";
import { Button, Card } from "@/components/ui";

function formatDate(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString();
}

export default function StudentMembershipsPage() {
  const t = useTranslations("studentMembershipsPage");
  const { memberships, loading, error } = useMyMemberships();

  const hasActiveMembership = memberships.some(
    (m) =>
      m.status === "ACTIVE" ||
      m.status === "PENDING_PAYMENT" ||
      m.status === "PENDING_PAYMENT_VALIDATION" ||
      m.status === "PENDING_MANAGER_ACTIVATION"
  );

  return (
    <div>
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
          <p className="font-[var(--font-mono)] text-xs text-k-muted mt-1">
            {t("subtitle")}
          </p>
        </div>
        {!hasActiveMembership && (
          <Button variant="volt" asChild>
            <Link href="/student/memberships/new">{t("newButton")}</Link>
          </Button>
        )}
      </div>

      {loading && (
        <p className="py-8 text-center text-sm text-k-muted">{t("loading")}</p>
      )}

      {error && (
        <div className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text">
          {error}
        </div>
      )}

      {!loading && !error && memberships.length === 0 && (
        <p className="py-8 text-center text-sm text-k-muted">
          {t("empty")}
        </p>
      )}

      <div className="space-y-6">
        {memberships.map((m) => (
          <Card key={m.id} padding="md">
            <div className="space-y-4">
              {/* Header */}
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-semibold text-k-dark">{m.planName}</p>
                  <p className="text-xs text-k-muted mt-0.5 font-mono">{m.id}</p>
                </div>
                <MembershipStatusBadge status={m.status} />
              </div>

              {/* Hour balance bar */}
              <HourBalance available={m.availableHours} purchased={m.purchasedHours} />

              {/* Key info */}
              <div className="grid grid-cols-2 gap-x-8 gap-y-1 text-sm">
                <div>
                  <span className="text-k-muted">{t("period")} </span>
                  <span className="font-medium text-k-dark font-mono text-xs">
                    {formatDate(m.startDate)} → {formatDate(m.expirationDate)}
                  </span>
                </div>
                <div>
                  <span className="text-k-muted">{t("paymentLabel")} </span>
                  <span className="font-medium text-k-dark">
                    {m.paymentValidated ? t("paymentValidated") : t("paymentPending")}
                  </span>
                </div>
              </div>

              {/* Actions row */}
              <div className="pt-1 flex items-center gap-4">
                <Link
                  href={`/student/memberships/${m.id}`}
                  className="text-sm text-k-subtle hover:text-k-dark font-medium"
                >
                  {t("viewDetails")}
                </Link>
                {m.status === "PENDING_PAYMENT" && (
                  <Link
                    href={`/student/memberships/${m.id}`}
                    className="text-sm font-medium text-k-warn-text hover:text-k-dark"
                  >
                    {t("uploadProof")}
                  </Link>
                )}
                {(m.status === "EXPIRED" || m.status === "INACTIVE") && (
                  <Link
                    href={`/student/memberships/new?renew=${m.id}`}
                    className="text-sm font-medium text-k-volt-text hover:text-k-dark"
                  >
                    {t("renew")}
                  </Link>
                )}
              </div>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}
