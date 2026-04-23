"use client";

import { useState } from "react";
import { Plus, Pencil } from "lucide-react";
import { useTranslations } from "next-intl";
import { useAdmins, useDeactivateAdmin, useActivateAdmin, useTenantOptions } from "@/hooks/useAdmins";
import { AdminSummary } from "@/lib/types/admin";
import CreateAdminModal from "./CreateAdminModal";
import EditAdminModal from "./EditAdminModal";

// ── Status filter tabs ───────────────────────────────────────────────────────

type StatusFilter = "ACTIVE" | "INVITED" | "INACTIVE" | "";

// ── Toggle switch ─────────────────────────────────────────────────────────────

interface ToggleProps {
  checked: boolean;
  disabled?: boolean;
  onChange: () => void;
}

function Toggle({ checked, disabled, onChange }: ToggleProps) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      disabled={disabled}
      onClick={onChange}
      className={`
        relative inline-flex h-5 w-9 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent
        transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1
        disabled:opacity-40 disabled:cursor-not-allowed
        ${checked ? "bg-green-500" : "bg-gray-300"}
      `}
    >
      <span
        className={`
          pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow ring-0
          transition duration-200 ease-in-out
          ${checked ? "translate-x-4" : "translate-x-0"}
        `}
      />
    </button>
  );
}

// ── Status badge ─────────────────────────────────────────────────────────────

const STATUS_COLORS: Record<string, string> = {
  ACTIVE:   "bg-green-100 text-green-700",
  INVITED:  "bg-yellow-100 text-yellow-700",
  INACTIVE: "bg-gray-100 text-gray-500",
};

function StatusBadge({ status }: { status: string }) {
  const classes = STATUS_COLORS[status] ?? "bg-gray-100 text-gray-600";
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${classes}`}>
      {status}
    </span>
  );
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

// ── Deactivate confirmation modal ─────────────────────────────────────────────

interface DeactivateModalProps {
  admin: AdminSummary;
  loading: boolean;
  error: string | null;
  onConfirm: () => void;
  onCancel: () => void;
}

function DeactivateModal({ admin, loading, error, onConfirm, onCancel }: DeactivateModalProps) {
  const t = useTranslations("admins");
  const name = [admin.firstName, admin.lastName].filter(Boolean).join(" ") || admin.email;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={onCancel}
        aria-hidden="true"
      />
      <div className="relative bg-white rounded-xl shadow-2xl w-full max-w-sm p-6">
        <h2 className="text-base font-semibold text-gray-900 mb-2">{t("modalDeactivateTitle")}</h2>
        <p className="text-sm text-gray-600 mb-1">
          {t("modalDeactivateConfirm", { name: <span className="font-medium text-gray-900">{name}</span> })}
        </p>
        <p className="text-xs text-gray-500 mb-6">
          {t("modalDeactivateHint")}
        </p>

        {error && (
          <div className="mb-4 rounded-md bg-red-50 border border-red-200 p-3 text-sm text-red-700">
            {error}
          </div>
        )}

        <div className="flex justify-end gap-3">
          <button
            onClick={onCancel}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition-colors"
          >
            {t("modalCancelButton")}
          </button>
          <button
            onClick={onConfirm}
            disabled={loading}
            className="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-md hover:bg-red-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? t("modalDeactivatingButton") : t("modalDeactivateButton")}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Main component ────────────────────────────────────────────────────────────

export default function AdminList() {
  const t = useTranslations("admins");
  const tPagination = useTranslations("pagination");
  const [page, setPage] = useState(0);
  const [tenantFilter, setTenantFilter] = useState<string>("");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ACTIVE");
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editTarget, setEditTarget] = useState<AdminSummary | null>(null);
  const [deactivateTarget, setDeactivateTarget] = useState<AdminSummary | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [togglingId, setTogglingId] = useState<string | null>(null);
  const SIZE = 20;

  const { admins, totalPages, totalElements, loading, error, refetch } = useAdmins({
    page,
    size: SIZE,
    tenantId: tenantFilter || undefined,
    status: statusFilter || undefined,
  });

  const { options: tenantOptions, loading: loadingTenants } = useTenantOptions();
  const { deactivate, loading: deactivating } = useDeactivateAdmin();
  const { activate } = useActivateAdmin();

  function handleTenantChange(value: string) {
    setTenantFilter(value);
    setPage(0);
  }

  function handleStatusFilter(value: StatusFilter) {
    setStatusFilter(value);
    setPage(0);
    setActionError(null);
  }

  function handleToggleClick(admin: AdminSummary) {
    setActionError(null);
    if (admin.status === "ACTIVE") {
      // Deactivation needs confirmation
      setDeactivateTarget(admin);
    } else {
      // Activation is direct — no confirmation needed
      void handleActivate(admin);
    }
  }

  async function handleActivate(admin: AdminSummary) {
    setTogglingId(admin.id);
    try {
      await activate(admin.id);
      refetch();
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Failed to activate admin.");
    } finally {
      setTogglingId(null);
    }
  }

  async function handleConfirmDeactivate() {
    if (!deactivateTarget) return;
    setActionError(null);
    setTogglingId(deactivateTarget.id);
    try {
      await deactivate(deactivateTarget.id);
      setDeactivateTarget(null);
      refetch();
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Failed to deactivate admin.");
    } finally {
      setTogglingId(null);
    }
  }

  const STATUS_TABS: { label: string; value: StatusFilter }[] = [
    { label: t("filterActive"),   value: "ACTIVE" },
    { label: t("filterInactive"), value: "INACTIVE" },
    { label: t("filterAll"),      value: "" },
  ];

  const COLUMNS = [
    { key: "colName",     label: t("colName"),     right: false },
    { key: "colEmail",    label: t("colEmail"),    right: false },
    { key: "colTenant",   label: t("colTenant"),   right: false },
    { key: "colDocument", label: t("colDocument"), right: false },
    { key: "colStatus",   label: t("colStatus"),   right: false },
    { key: "colCreated",  label: t("colCreated"),  right: false },
    { key: "colActions",  label: t("colActions"),  right: true },
  ];

  return (
    <div className="space-y-4">
      {/* Controls bar */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          {/* Tenant filter */}
          <label htmlFor="tenantFilter" className="text-sm font-medium text-gray-700 whitespace-nowrap">
            {t("filterTenantLabel")}
          </label>
          <select
            id="tenantFilter"
            value={tenantFilter}
            onChange={(e) => handleTenantChange(e.target.value)}
            disabled={loadingTenants}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
          >
            <option value="">{t("filterAllTenants")}</option>
            {Object.entries(tenantOptions).map(([id, name]) => (
              <option key={id} value={id}>{name}</option>
            ))}
          </select>

          {/* Status filter tabs */}
          <div className="flex rounded-md border border-gray-300 overflow-hidden shadow-sm">
            {STATUS_TABS.map((tab, i) => (
              <button
                key={tab.value}
                type="button"
                onClick={() => handleStatusFilter(tab.value)}
                className={`
                  px-3 py-1.5 text-sm font-medium transition-colors
                  ${i < STATUS_TABS.length - 1 ? "border-r border-gray-300" : ""}
                  ${statusFilter === tab.value
                    ? "bg-blue-600 text-white"
                    : "bg-white text-gray-700 hover:bg-gray-50"}
                `}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        <button
          onClick={() => setShowCreateModal(true)}
          className="inline-flex items-center gap-2 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
        >
          <Plus className="h-4 w-4" />
          {t("createButton")}
        </button>
      </div>

      {/* Errors */}
      {(error || actionError) && (
        <div className="rounded-md bg-red-50 border border-red-200 p-4 text-sm text-red-700">
          {error || actionError}
        </div>
      )}

      {/* Table */}
      {loading ? (
        <div className="text-center py-10 text-sm text-gray-500">{t("listLoading")}</div>
      ) : admins.length === 0 ? (
        <div className="text-center py-10 text-sm text-gray-500">
          {t("listEmpty")}
        </div>
      ) : (
        <>
          <div className="overflow-x-auto rounded-lg border border-gray-200 shadow-sm">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  {COLUMNS.map((col) => (
                    <th key={col.key} className={`px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider ${col.right ? "text-right" : "text-left"}`}>
                      {col.label}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {admins.map((admin: AdminSummary) => (
                  <tr
                    key={admin.id}
                    className={`hover:bg-gray-50 ${admin.status === "INACTIVE" || admin.status === "INVITED" ? "opacity-60" : ""}`}
                  >
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {admin.firstName || admin.lastName
                        ? [admin.firstName, admin.lastName].filter(Boolean).join(" ")
                        : <span className="text-gray-400 italic">—</span>}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">{admin.email}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{admin.tenantName}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      <span className="font-mono">{admin.identityDocumentType}</span>{" "}{admin.identityNumber}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <StatusBadge status={admin.status} />
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {formatDate(admin.createdAt)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right">
                      <div className="flex items-center justify-end gap-3">
                        <button
                          onClick={() => { setActionError(null); setEditTarget(admin); }}
                          title="Edit"
                          className="p-1.5 text-gray-400 hover:text-blue-600 rounded transition-colors"
                        >
                          <Pencil className="h-4 w-4" />
                        </button>
                        <Toggle
                          checked={admin.status === "ACTIVE"}
                          disabled={togglingId === admin.id || admin.status === "INVITED"}
                          onChange={() => handleToggleClick(admin)}
                        />
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          <div className="flex items-center justify-between border-t border-gray-200 pt-4">
            <p className="text-sm text-gray-700">
              {tPagination("summary", { current: page + 1, total: totalPages, count: totalElements })}
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {tPagination("previous")}
              </button>
              <button
                type="button"
                onClick={() => setPage((p) => p + 1)}
                disabled={page >= totalPages - 1}
                className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {tPagination("next")}
              </button>
            </div>
          </div>
        </>
      )}

      {/* Create modal */}
      {showCreateModal && (
        <CreateAdminModal
          onClose={() => setShowCreateModal(false)}
          onCreated={() => { setShowCreateModal(false); refetch(); }}
          defaultTenantId={tenantFilter || undefined}
        />
      )}

      {/* Edit modal */}
      {editTarget && (
        <EditAdminModal
          admin={editTarget}
          onClose={() => setEditTarget(null)}
          onUpdated={(updated) => {
            setEditTarget(null);
            refetch();
            void updated;
          }}
        />
      )}

      {/* Deactivate confirmation modal */}
      {deactivateTarget && (
        <DeactivateModal
          admin={deactivateTarget}
          loading={deactivating}
          error={actionError}
          onConfirm={handleConfirmDeactivate}
          onCancel={() => { setDeactivateTarget(null); setActionError(null); }}
        />
      )}
    </div>
  );
}
