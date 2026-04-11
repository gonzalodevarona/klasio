"use client";

import { useEffect, useState } from "react";
import { usePaymentProofs } from "@/hooks/usePaymentProofs";
import { ProofReviewModal } from "./ProofReviewModal";
import type { ProofQueueItem } from "@/lib/types/paymentProof";

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString();
}

export function ProofQueue() {
  const { listPendingProofs, loading, error } = usePaymentProofs();
  const [queue, setQueue] = useState<ProofQueueItem[]>([]);
  const [selected, setSelected] = useState<ProofQueueItem | null>(null);

  async function load() {
    const items = await listPendingProofs();
    setQueue(items);
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleDone() {
    setSelected(null);
    load();
  }

  return (
    <div className="space-y-4">
      {error && (
        <div className="rounded-md bg-red-50 border border-red-200 p-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {loading && queue.length === 0 && (
        <p className="py-8 text-center text-sm text-gray-500">Loading queue…</p>
      )}

      {!loading && queue.length === 0 && (
        <p className="py-8 text-center text-sm text-gray-400">No pending proofs.</p>
      )}

      {queue.length > 0 && (
        <div className="overflow-x-auto rounded-lg border border-gray-200">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Student
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Program
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Uploaded
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Type
                </th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 bg-white">
              {queue.map((item) => (
                <tr key={item.proofId} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm text-gray-900">{item.studentName}</td>
                  <td className="px-4 py-3 text-sm text-gray-700">{item.programName}</td>
                  <td className="px-4 py-3 text-sm text-gray-500">
                    {formatDateTime(item.uploadedAt)}
                  </td>
                  <td className="px-4 py-3 text-xs text-gray-500">{item.contentType}</td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => setSelected(item)}
                      className="rounded-md bg-indigo-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-indigo-700"
                    >
                      Review
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selected && (
        <ProofReviewModal
          proof={selected}
          onClose={() => setSelected(null)}
          onDone={handleDone}
        />
      )}
    </div>
  );
}
