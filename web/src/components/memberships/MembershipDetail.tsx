"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { MembershipDetail as MembershipDetailType } from "@/lib/types/membership";
import MembershipStatusBadge from "./MembershipStatusBadge";
import HourBalance from "./HourBalance";
import HourTransactionList from "./HourTransactionList";
import HourAdjustmentForm from "./HourAdjustmentForm";
import { useMembershipActions } from "@/hooks/useMemberships";

interface MembershipDetailProps {
  membership: MembershipDetailType;
  onRefresh: () => void;
  isAdmin?: boolean;
  isManager?: boolean;
}

function InfoRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex flex-col sm:flex-row sm:gap-4">
      <span className="text-sm text-gray-500 sm:w-48 shrink-0">{label}</span>
      <span className="text-sm text-gray-900">{value ?? "—"}</span>
    </div>
  );
}

function formatDate(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString();
}

function formatDateTime(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleString();
}

export default function MembershipDetail({
  membership,
  onRefresh,
  isAdmin = false,
  isManager = false,
}: MembershipDetailProps) {
  const [showAdjust, setShowAdjust] = useState(false);
  const t = useTranslations("memberships");
  const tCommon = useTranslations("common");
  const { activateMembership, validatePayment, adjustHours, loading, error } =
    useMembershipActions();

  async function handleActivate() {
    await activateMembership(membership.id);
    onRefresh();
  }

  async function handleValidatePayment(activateDirectly: boolean) {
    await validatePayment(membership.id, { activateDirectly });
    onRefresh();
  }

  async function handleAdjustHours(delta: number, reason: string) {
    await adjustHours(membership.id, { delta, reason });
    setShowAdjust(false);
    onRefresh();
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div className="space-y-1">
          <div className="flex items-center gap-3">
            <h2 className="text-xl font-semibold text-gray-900">{t("detailTitle")}</h2>
            <MembershipStatusBadge status={membership.status} />
          </div>
          <p className="text-xs font-mono text-gray-400">{membership.id}</p>
        </div>

        <div className="flex items-center gap-2">
          {membership.status === "PENDING_PAYMENT_VALIDATION" && isAdmin && (
            <div className="flex gap-2">
              <button
                onClick={() => handleValidatePayment(true)}
                disabled={loading}
                className="rounded-md bg-green-600 px-3 py-1.5 text-sm text-white hover:bg-green-700 disabled:opacity-50"
              >
                {t("btnValidateActivate")}
              </button>
              <button
                onClick={() => handleValidatePayment(false)}
                disabled={loading}
                className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-50"
              >
                {t("btnValidateDelegate")}
              </button>
            </div>
          )}
          {membership.status === "PENDING_MANAGER_ACTIVATION" && (isAdmin || isManager) && (
            <button
              onClick={handleActivate}
              disabled={loading}
              className="rounded-md bg-blue-600 px-3 py-1.5 text-sm text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {t("btnActivate")}
            </button>
          )}
          {membership.status === "ACTIVE" && isAdmin && (
            <button
              onClick={() => setShowAdjust(true)}
              className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50"
            >
              {t("btnAdjustHours")}
            </button>
          )}
        </div>
      </div>

      {error && (
        <div className="rounded-md bg-red-50 p-3 text-sm text-red-700 border border-red-200">
          {error}
        </div>
      )}

      {/* Hours balance */}
      <div className="rounded-md border border-gray-200 p-4 bg-gray-50">
        <HourBalance available={membership.availableHours} purchased={membership.purchasedHours} />
      </div>

      {/* Details */}
      <div className="rounded-md border border-gray-200 p-4 space-y-3">
        <h3 className="text-sm font-semibold text-gray-700 mb-2">{t("detailSectionDetails")}</h3>
        <InfoRow label={t("detailLabelPlan")} value={membership.planName} />
        <InfoRow label={t("detailLabelProgram")} value={membership.programName} />
        <InfoRow label={t("detailLabelStudent")} value={membership.studentName} />
        <InfoRow label={t("detailLabelPurchasedHours")} value={membership.purchasedHours} />
        <InfoRow label={t("detailLabelAvailableHours")} value={membership.availableHours} />
        <InfoRow label={t("detailLabelStartDate")} value={formatDate(membership.startDate)} />
        <InfoRow label={t("detailLabelExpirationDate")} value={formatDate(membership.expirationDate)} />
        <InfoRow label={t("detailLabelPaymentValidated")} value={membership.paymentValidated ? tCommon("yes") : tCommon("no")} />
        <InfoRow label={t("detailLabelActivatedAt")} value={formatDateTime(membership.activatedAt)} />
        <InfoRow label={t("detailLabelCreatedAt")} value={formatDateTime(membership.createdAt)} />
        <InfoRow label={t("detailLabelUpdatedAt")} value={formatDateTime(membership.updatedAt)} />
      </div>

      {/* Transaction ledger */}
      <div className="rounded-md border border-gray-200 p-4">
        <h3 className="text-sm font-semibold text-gray-700 mb-4">{t("detailSectionTxHistory")}</h3>
        <HourTransactionList membershipId={membership.id} />
      </div>

      {/* Adjust hours modal */}
      {showAdjust && (
        <HourAdjustmentForm
          membershipId={membership.id}
          onSubmit={handleAdjustHours}
          onCancel={() => setShowAdjust(false)}
        />
      )}
    </div>
  );
}
