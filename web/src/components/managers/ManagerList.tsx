"use client";

import { useState } from "react";
import { Plus, Pencil } from "lucide-react";
import { useTranslations } from "next-intl";
import { useManagers, useDeactivateManager, useActivateManager, useManagerTenantOptions } from "@/hooks/useManagers";
import { useAuth } from "@/hooks/useAuth";
import { ManagerSummary } from "@/lib/types/manager";
import { Table, Thead, Th, Tr, Td, Select, Button, Badge, type BadgeVariant } from "@/components/ui";
import CreateManagerModal from "./CreateManagerModal";
import EditManagerModal from "./EditManagerModal";

// ── Status filter ─────────────────────────────────────────────────────────────

type StatusFilter = "ACTIVE" | "INVITED" | "INACTIVE" | "";

// ── Toggle ────────────────────────────────────────────────────────────────────

function Toggle({ checked, disabled, onChange }: { checked: boolean; disabled?: boolean; onChange: () => void }) {
  return (
    <button
      type="button" role="switch" aria-checked={checked} disabled={disabled} onClick={onChange}
      className={`relative inline-flex h-5 w-9 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent
        transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-k-volt focus:ring-offset-1
        disabled:opacity-40 disabled:cursor-not-allowed ${checked ? "bg-k-volt" : "bg-k-border"}`}
    >
      <span className={`pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow ring-0
        transition duration-200 ease-in-out ${checked ? "translate-x-4" : "translate-x-0"}`} />
    </button>
  );
}

const MANAGER_STATUS_VARIANT: Record<string, BadgeVariant> = {
  ACTIVE:   "active",
  INVITED:  "pending",
  INACTIVE: "inactive",
};

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString("en-US", { year: "numeric", month: "short", day: "numeric" });
}

// ── Deactivate confirmation modal ─────────────────────────────────────────────

function DeactivateModal({ manager, loading, error, onConfirm, onCancel }: {
  manager: ManagerSummary; loading: boolean; error: string | null;
  onConfirm: () => void; onCancel: () => void;
}) {
  const t = useTranslations("managers");
  const name = [manager.firstName, manager.lastName].filter(Boolean).join(" ") || manager.email;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={onCancel} aria-hidden="true" />
      <div className="relative bg-white rounded-xl shadow-2xl w-full max-w-sm p-6">
        <h2 className="text-base font-semibold text-gray-900 mb-2">{t("modalDeactivateTitle")}</h2>
        <p className="text-sm text-gray-600 mb-1">
          {t("modalDeactivateConfirm", { name })}
        </p>
        <p className="text-xs text-gray-500 mb-6">
          {t("modalDeactivateHint")}
        </p>
        {error && (
          <div className="mb-4 rounded-md bg-red-50 border border-red-200 p-3 text-sm text-red-700">{error}</div>
        )}
        <div className="flex justify-end gap-3">
          <button onClick={onCancel}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition-colors">
            {t("modalCancelButton")}
          </button>
          <button onClick={onConfirm} disabled={loading}
            className="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-md hover:bg-red-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
            {loading ? t("modalDeactivatingButton") : t("modalDeactivateButton")}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Main component ────────────────────────────────────────────────────────────

export default function ManagerList() {
  const t = useTranslations("managers");
  const tPagination = useTranslations("pagination");
  const tBadge = useTranslations("badges.managerStatus");
  const { user } = useAuth();
  // ADMIN users are always scoped to their own tenant — they cannot select another tenant.
  const isAdmin = user?.roles.includes("ADMIN") ?? false;
  const forcedTenantId = isAdmin ? (user?.tenantId ?? undefined) : undefined;

  const [page, setPage] = useState(0);
  const [tenantFilter, setTenantFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ACTIVE");
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editTarget, setEditTarget] = useState<ManagerSummary | null>(null);
  const [deactivateTarget, setDeactivateTarget] = useState<ManagerSummary | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [togglingId, setTogglingId] = useState<string | null>(null);
  const SIZE = 20;

  // For ADMIN: always send their tenantId; ignore the local filter state.
  const effectiveTenantId = forcedTenantId ?? (tenantFilter || undefined);

  const { managers, totalPages, totalElements, loading, error, refetch } = useManagers({
    page, size: SIZE,
    tenantId: effectiveTenantId,
    status: statusFilter || undefined,
  });

  const { options: tenantOptions, loading: loadingTenants } = useManagerTenantOptions();
  const { deactivate, loading: deactivating } = useDeactivateManager();
  const { activate } = useActivateManager();

  function handleStatusFilter(value: StatusFilter) {
    setStatusFilter(value);
    setPage(0);
    setActionError(null);
  }

  function handleToggleClick(manager: ManagerSummary) {
    setActionError(null);
    if (manager.status === "ACTIVE") {
      setDeactivateTarget(manager);
    } else {
      void handleActivate(manager);
    }
  }

  async function handleActivate(manager: ManagerSummary) {
    setTogglingId(manager.id);
    try {
      await activate(manager.id);
      refetch();
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Failed to activate manager.");
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
      setActionError(err instanceof Error ? err.message : "Failed to deactivate manager.");
    } finally {
      setTogglingId(null);
    }
  }

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
      {/* Page header */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
        <Button variant="volt" onClick={() => setShowCreateModal(true)}>
          <Plus className="h-4 w-4 mr-1" />
          {t("createButton")}
        </Button>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3">
        {/* Tenant filter — hidden for ADMIN (they are scoped to their own tenant) */}
        {!isAdmin && (
          <Select
            label={t("filterTenantLabel")}
            value={tenantFilter}
            onChange={(e) => { setTenantFilter(e.target.value); setPage(0); }}
            disabled={loadingTenants}
          >
            <option value="">{t("filterAllTenants")}</option>
            {Object.entries(tenantOptions).map(([id, name]) => (
              <option key={id} value={id}>{name}</option>
            ))}
          </Select>
        )}

        <Select
          label={t("filterStatusLabel")}
          value={statusFilter}
          onChange={(e) => handleStatusFilter(e.target.value as StatusFilter)}
        >
          <option value="">{t("filterAll")}</option>
          <option value="ACTIVE">{t("filterActive")}</option>
          <option value="INACTIVE">{t("filterInactive")}</option>
        </Select>
      </div>

      {(error || actionError) && (
        <div className="rounded-md bg-red-50 border border-red-200 p-4 text-sm text-red-700">
          {error || actionError}
        </div>
      )}

      {loading ? (
        <div className="text-center py-10 text-sm text-gray-500">{t("listLoading")}</div>
      ) : managers.length === 0 ? (
        <div className="text-center py-10 text-sm text-gray-500">{t("listEmpty")}</div>
      ) : (
        <>
          <Table>
            <Thead>
              <tr>
                {COLUMNS.map((col) => (
                  col.right ? <Th key={col.key} right>{col.label}</Th> : <Th key={col.key}>{col.label}</Th>
                ))}
              </tr>
            </Thead>
            <tbody>
              {managers.map((m: ManagerSummary) => (
                <Tr key={m.id} className={m.status === "INACTIVE" || m.status === "INVITED" ? "opacity-60" : ""}>
                  <Td bold>
                    {m.firstName || m.lastName
                      ? [m.firstName, m.lastName].filter(Boolean).join(" ")
                      : <span className="text-gray-400 italic">—</span>}
                  </Td>
                  <Td>{m.email}</Td>
                  <Td muted>{m.tenantName}</Td>
                  <Td muted>
                    <span className="font-mono">{m.identityDocumentType}</span>{" "}{m.identityNumber}
                  </Td>
                  <Td><Badge variant={MANAGER_STATUS_VARIANT[m.status] ?? "inactive"} label={tBadge(m.status)} /></Td>
                  <Td muted>{formatDate(m.createdAt)}</Td>
                  <Td right>
                    <div className="flex items-center justify-end gap-3">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => { setActionError(null); setEditTarget(m); }}
                        title="Edit"
                      >
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Toggle
                        checked={m.status === "ACTIVE"}
                        disabled={togglingId === m.id || m.status === "INVITED"}
                        onChange={() => handleToggleClick(m)}
                      />
                    </div>
                  </Td>
                </Tr>
              ))}
            </tbody>
          </Table>

          <div className="flex items-center justify-between border-t border-k-line pt-4">
            <p className="text-sm text-k-subtle">{tPagination("summary", { current: page + 1, total: totalPages, count: totalElements })}</p>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                {tPagination("previous")}
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => p + 1)}
                disabled={page >= totalPages - 1}
              >
                {tPagination("next")}
              </Button>
            </div>
          </div>
        </>
      )}

      {showCreateModal && (
        <CreateManagerModal
          onClose={() => setShowCreateModal(false)}
          onCreated={() => { setShowCreateModal(false); refetch(); }}
          defaultTenantId={forcedTenantId ?? (tenantFilter || undefined)}
        />
      )}

      {editTarget && (
        <EditManagerModal
          manager={editTarget}
          onClose={() => setEditTarget(null)}
          onUpdated={(updated) => { setEditTarget(null); refetch(); void updated; }}
        />
      )}

      {deactivateTarget && (
        <DeactivateModal
          manager={deactivateTarget}
          loading={deactivating}
          error={actionError}
          onConfirm={handleConfirmDeactivate}
          onCancel={() => { setDeactivateTarget(null); setActionError(null); }}
        />
      )}
    </div>
  );
}
