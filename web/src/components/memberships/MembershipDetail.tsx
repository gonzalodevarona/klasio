"use client";

import { useState } from "react";
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
            <h2 className="text-xl font-semibold text-gray-900">Membership</h2>
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
                Validate & Activate
              </button>
              <button
                onClick={() => handleValidatePayment(false)}
                disabled={loading}
                className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-50"
              >
                Validate (delegate)
              </button>
            </div>
          )}
          {membership.status === "PENDING_MANAGER_ACTIVATION" && (isAdmin || isManager) && (
            <button
              onClick={handleActivate}
              disabled={loading}
              className="rounded-md bg-blue-600 px-3 py-1.5 text-sm text-white hover:bg-blue-700 disabled:opacity-50"
            >
              Activate
            </button>
          )}
          {membership.status === "ACTIVE" && isAdmin && (
            <button
              onClick={() => setShowAdjust(true)}
              className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50"
            >
              Adjust Hours
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
        <h3 className="text-sm font-semibold text-gray-700 mb-2">Details</h3>
        <InfoRow label="Plan" value={membership.planName} />
        <InfoRow label="Program" value={membership.programName} />
        <InfoRow label="Student" value={membership.studentName} />
        <InfoRow label="Purchased hours" value={membership.purchasedHours} />
        <InfoRow label="Available hours" value={membership.availableHours} />
        <InfoRow label="Start date" value={formatDate(membership.startDate)} />
        <InfoRow label="Expiration date" value={formatDate(membership.expirationDate)} />
        <InfoRow label="Payment validated" value={membership.paymentValidated ? "Yes" : "No"} />
        <InfoRow label="Activated at" value={formatDateTime(membership.activatedAt)} />
        <InfoRow label="Created at" value={formatDateTime(membership.createdAt)} />
        <InfoRow label="Updated at" value={formatDateTime(membership.updatedAt)} />
      </div>

      {/* Transaction ledger */}
      <div className="rounded-md border border-gray-200 p-4">
        <h3 className="text-sm font-semibold text-gray-700 mb-4">Hour Transaction History</h3>
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
