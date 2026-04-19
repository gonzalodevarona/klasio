"use client";

import { useState } from "react";
import { Plus, Pencil } from "lucide-react";
import {
  useProfessors,
  useDeactivateProfessor,
  useReactivateProfessor,
} from "@/hooks/useProfessors";
import { ProfessorSummary } from "@/lib/types/professor";
import CreateProfessorModal from "./CreateProfessorModal";
import EditProfessorModal from "./EditProfessorModal";

// ── Types ─────────────────────────────────────────────────────────────────────

type StatusFilter = "ACTIVE" | "DEACTIVATED" | "";

const STATUS_TABS: { label: string; value: StatusFilter }[] = [
  { label: "Active",      value: "ACTIVE" },
  { label: "Deactivated", value: "DEACTIVATED" },
  { label: "All",         value: "" },
];

// ── Toggle ────────────────────────────────────────────────────────────────────

function Toggle({ checked, disabled, onChange }: {
  checked: boolean; disabled?: boolean; onChange: () => void;
}) {
  return (
    <button
      type="button" role="switch" aria-checked={checked} disabled={disabled} onClick={onChange}
      className={`relative inline-flex h-5 w-9 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent
        transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1
        disabled:opacity-40 disabled:cursor-not-allowed ${checked ? "bg-green-500" : "bg-gray-300"}`}
    >
      <span className={`pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow ring-0
        transition duration-200 ease-in-out ${checked ? "translate-x-4" : "translate-x-0"}`} />
    </button>
  );
}

// ── Status badge ──────────────────────────────────────────────────────────────

const STATUS_COLORS: Record<string, string> = {
  ACTIVE:      "bg-green-100 text-green-700",
  INVITED:     "bg-yellow-100 text-yellow-700",
  DEACTIVATED: "bg-gray-100 text-gray-500",
};

function StatusBadge({ status }: { status: string }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_COLORS[status] ?? "bg-gray-100 text-gray-600"}`}>
      {status.charAt(0) + status.slice(1).toLowerCase()}
    </span>
  );
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString("en-US", {
    year: "numeric", month: "short", day: "numeric",
  });
}

// ── Deactivate confirmation modal ─────────────────────────────────────────────

function DeactivateModal({ professor, loading, error, onConfirm, onCancel }: {
  professor: ProfessorSummary; loading: boolean; error: string | null;
  onConfirm: () => void; onCancel: () => void;
}) {
  const name = [professor.firstName, professor.lastName].filter(Boolean).join(" ") || professor.email;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={onCancel} aria-hidden="true" />
      <div className="relative bg-white rounded-xl shadow-2xl w-full max-w-sm p-6">
        <h2 className="text-base font-semibold text-gray-900 mb-2">Deactivate Professor</h2>
        <p className="text-sm text-gray-600 mb-1">
          Are you sure you want to deactivate <span className="font-medium text-gray-900">{name}</span>?
        </p>
        <p className="text-xs text-gray-500 mb-6">
          The account will be disabled immediately. You can re-activate it at any time.
        </p>
        {error && (
          <div className="mb-4 rounded-md bg-red-50 border border-red-200 p-3 text-sm text-red-700">{error}</div>
        )}
        <div className="flex justify-end gap-3">
          <button onClick={onCancel}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition-colors">
            Cancel
          </button>
          <button onClick={onConfirm} disabled={loading}
            className="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-md hover:bg-red-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
            {loading ? "Deactivating..." : "Deactivate"}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Main component ────────────────────────────────────────────────────────────

export default function ProfessorList() {
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ACTIVE");
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editTarget, setEditTarget] = useState<ProfessorSummary | null>(null);
  const [deactivateTarget, setDeactivateTarget] = useState<ProfessorSummary | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [togglingId, setTogglingId] = useState<string | null>(null);
  const SIZE = 20;

  const { professors, totalPages, totalElements, loading, error, refetch } = useProfessors(
    page, SIZE, statusFilter || undefined
  );
  const { deactivate, loading: deactivating } = useDeactivateProfessor();
  const { reactivate } = useReactivateProfessor();

  function handleStatusFilter(value: StatusFilter) {
    setStatusFilter(value);
    setPage(0);
    setActionError(null);
  }

  function handleToggleClick(professor: ProfessorSummary) {
    setActionError(null);
    if (professor.status === "ACTIVE" || professor.status === "INVITED") {
      setDeactivateTarget(professor);
    } else {
      void handleReactivate(professor);
    }
  }

  async function handleReactivate(professor: ProfessorSummary) {
    setTogglingId(professor.id);
    try {
      await reactivate(professor.id);
      refetch();
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Failed to reactivate professor.");
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
      setActionError(err instanceof Error ? err.message : "Failed to deactivate professor.");
    } finally {
      setTogglingId(null);
    }
  }

  return (
    <div className="space-y-4">
      {/* Controls */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex rounded-md border border-gray-300 overflow-hidden shadow-sm">
          {STATUS_TABS.map((tab, i) => (
            <button key={tab.value} type="button" onClick={() => handleStatusFilter(tab.value)}
              className={`px-3 py-1.5 text-sm font-medium transition-colors
                ${i < STATUS_TABS.length - 1 ? "border-r border-gray-300" : ""}
                ${statusFilter === tab.value ? "bg-blue-600 text-white" : "bg-white text-gray-700 hover:bg-gray-50"}`}>
              {tab.label}
            </button>
          ))}
        </div>

        <button onClick={() => setShowCreateModal(true)}
          className="inline-flex items-center gap-2 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2">
          <Plus className="h-4 w-4" />
          Add Professor
        </button>
      </div>

      {(error || actionError) && (
        <div className="rounded-md bg-red-50 border border-red-200 p-4 text-sm text-red-700">
          {error || actionError}
        </div>
      )}

      {loading ? (
        <div className="text-center py-10 text-sm text-gray-500">Loading professors...</div>
      ) : professors.length === 0 ? (
        <div className="text-center py-10 text-sm text-gray-500">No professors match the current filter.</div>
      ) : (
        <>
          <div className="overflow-x-auto rounded-lg border border-gray-200 shadow-sm">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  {["Name", "Email", "Phone", "Document", "Status", "Created", "Actions"].map((h) => (
                    <th key={h} className={`px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider ${h === "Actions" ? "text-right" : "text-left"}`}>
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {professors.map((p: ProfessorSummary) => (
                  <tr key={p.id} className={`hover:bg-gray-50 ${p.status === "DEACTIVATED" ? "opacity-60" : ""}`}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {p.firstName} {p.lastName}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">{p.email}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {p.phoneNumber ?? <span className="text-gray-300 italic">—</span>}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      <span className="font-mono">{p.identityDocumentType}</span>{" "}{p.identityNumber}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <StatusBadge status={p.status} />
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{formatDate(p.createdAt)}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-right">
                      <div className="flex items-center justify-end gap-3">
                        <button onClick={() => { setActionError(null); setEditTarget(p); }} title="Edit"
                          className="p-1.5 text-gray-400 hover:text-blue-600 rounded transition-colors">
                          <Pencil className="h-4 w-4" />
                        </button>
                        <Toggle
                          checked={p.status === "ACTIVE" || p.status === "INVITED"}
                          disabled={togglingId === p.id}
                          onChange={() => handleToggleClick(p)}
                        />
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="flex items-center justify-between border-t border-gray-200 pt-4">
            <p className="text-sm text-gray-700">Page {page + 1} of {totalPages} ({totalElements} total)</p>
            <div className="flex gap-2">
              <button type="button" onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}
                className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed">
                Previous
              </button>
              <button type="button" onClick={() => setPage((p) => p + 1)} disabled={page >= totalPages - 1}
                className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed">
                Next
              </button>
            </div>
          </div>
        </>
      )}

      {showCreateModal && (
        <CreateProfessorModal
          onClose={() => setShowCreateModal(false)}
          onCreated={() => { setShowCreateModal(false); refetch(); }}
        />
      )}

      {editTarget && (
        <EditProfessorModal
          professor={editTarget}
          onClose={() => setEditTarget(null)}
          onUpdated={() => { setEditTarget(null); refetch(); }}
        />
      )}

      {deactivateTarget && (
        <DeactivateModal
          professor={deactivateTarget}
          loading={deactivating}
          error={actionError}
          onConfirm={handleConfirmDeactivate}
          onCancel={() => { setDeactivateTarget(null); setActionError(null); }}
        />
      )}
    </div>
  );
}
