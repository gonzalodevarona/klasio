"use client";

import { useState } from "react";
import Image from "next/image";
import { TenantDetail as TenantDetailType } from "@/lib/types/tenant";
import { api, ApiError } from "@/lib/api";
import TenantStatusBadge from "./TenantStatusBadge";

interface TenantDetailProps {
  tenant: TenantDetailType;
  onDeactivated?: () => void;
}

export default function TenantDetail({
  tenant,
  onDeactivated,
}: TenantDetailProps) {
  const [showConfirm, setShowConfirm] = useState(false);
  const [deactivating, setDeactivating] = useState(false);
  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  async function handleDeactivate() {
    setDeactivating(true);
    setFeedback(null);

    try {
      await api.post(`/tenants/${tenant.slug}/deactivate`);
      setFeedback({
        type: "success",
        message: "Tenant has been deactivated successfully.",
      });
      setShowConfirm(false);
      onDeactivated?.();
    } catch (err) {
      const message =
        err instanceof ApiError
          ? err.message
          : "Failed to deactivate tenant. Please try again.";
      setFeedback({ type: "error", message });
    } finally {
      setDeactivating(false);
    }
  }

  function formatDate(dateString: string | null): string {
    if (!dateString) return "-";
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "long",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  }

  return (
    <div className="space-y-6">
      {feedback && (
        <div
          className={`rounded-md p-4 text-sm border ${
            feedback.type === "success"
              ? "bg-green-50 text-green-700 border-green-200"
              : "bg-red-50 text-red-700 border-red-200"
          }`}
          role="alert"
        >
          {feedback.message}
        </div>
      )}

      <div className="bg-white shadow rounded-lg overflow-hidden">
        {/* Header */}
        <div className="px-6 py-5 border-b border-gray-200 flex items-center justify-between">
          <div className="flex items-center gap-4">
            {tenant.logoUrl && (
              <Image
                src={tenant.logoUrl}
                alt={`${tenant.name} logo`}
                width={48}
                height={48}
                className="h-12 w-12 rounded-full object-cover"
                unoptimized
              />
            )}
            <div>
              <h2 className="text-xl font-semibold text-gray-900">
                {tenant.name}
              </h2>
              <p className="text-sm text-gray-500 font-mono">{tenant.slug}</p>
            </div>
          </div>
          <TenantStatusBadge status={tenant.status} />
        </div>

        {/* Details */}
        <div className="px-6 py-5">
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-4">
            <div>
              <dt className="text-sm font-medium text-gray-500">
                Sport Discipline
              </dt>
              <dd className="mt-1 text-sm text-gray-900">
                {tenant.sportDiscipline}
              </dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-gray-500">
                Contact Email
              </dt>
              <dd className="mt-1 text-sm text-gray-900">
                {tenant.contactEmail}
              </dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-gray-500">
                Contact Phone
              </dt>
              <dd className="mt-1 text-sm text-gray-900">
                {tenant.contactPhone ?? "-"}
              </dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-gray-500">
                Contact Address
              </dt>
              <dd className="mt-1 text-sm text-gray-900">
                {tenant.contactAddress ?? "-"}
              </dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-gray-500">Created At</dt>
              <dd className="mt-1 text-sm text-gray-900">
                {formatDate(tenant.createdAt)}
              </dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-gray-500">Created By</dt>
              <dd className="mt-1 text-sm text-gray-900">
                {tenant.createdBy}
              </dd>
            </div>

            {tenant.deactivatedAt && (
              <>
                <div>
                  <dt className="text-sm font-medium text-gray-500">
                    Deactivated At
                  </dt>
                  <dd className="mt-1 text-sm text-gray-900">
                    {formatDate(tenant.deactivatedAt)}
                  </dd>
                </div>

                <div>
                  <dt className="text-sm font-medium text-gray-500">
                    Deactivated By
                  </dt>
                  <dd className="mt-1 text-sm text-gray-900">
                    {tenant.deactivatedBy ?? "-"}
                  </dd>
                </div>
              </>
            )}
          </dl>
        </div>

        {/* Actions */}
        {tenant.status === "ACTIVE" && (
          <div className="px-6 py-4 border-t border-gray-200 bg-gray-50">
            {!showConfirm ? (
              <button
                type="button"
                onClick={() => setShowConfirm(true)}
                className="inline-flex items-center rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2"
              >
                Deactivate Tenant
              </button>
            ) : (
              <div className="space-y-3">
                <p className="text-sm text-gray-700">
                  Are you sure? This will invalidate all sessions.
                </p>
                <div className="flex gap-3">
                  <button
                    type="button"
                    onClick={handleDeactivate}
                    disabled={deactivating}
                    className="inline-flex items-center rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {deactivating ? "Deactivating..." : "Confirm Deactivation"}
                  </button>
                  <button
                    type="button"
                    onClick={() => setShowConfirm(false)}
                    disabled={deactivating}
                    className="inline-flex items-center rounded-md bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
