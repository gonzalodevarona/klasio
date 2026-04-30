"use client";

import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { usePaymentProofs } from "@/hooks/usePaymentProofs";
import { ProofReviewModal } from "./ProofReviewModal";
import type { ProofQueueItem } from "@/lib/types/paymentProof";
import { IDENTITY_DOCUMENT_TYPES } from "@/lib/types/student";
import { Table, Thead, Th, Tr, Td, Button } from "@/components/ui";

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString();
}

function formatDocType(code: string): string {
  return IDENTITY_DOCUMENT_TYPES.find((t) => t.value === code)?.label ?? code;
}

export function ProofQueue() {
  const t = useTranslations("paymentProofs");
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

  // After approving or rejecting a proof, remove it optimistically so the row
  // disappears immediately (no flicker waiting on the refetch) and then refresh
  // from the server to pick up any new items that arrived in the meantime.
  function handleDone(resolvedProofId: string) {
    setQueue((prev) => prev.filter((item) => item.proofId !== resolvedProofId));
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
        <p className="py-8 text-center text-sm text-gray-500">{t("queueLoading")}</p>
      )}

      {!loading && queue.length === 0 && (
        <p className="py-8 text-center text-sm text-gray-400">{t("queueEmpty")}</p>
      )}

      {queue.length > 0 && (
        <Table>
          <Thead>
            <tr>
              <Th>{t("colStudent")}</Th>
              <Th>{t("colIdDocument")}</Th>
              <Th>{t("colPlan")}</Th>
              <Th>{t("colUploaded")}</Th>
              <Th>{t("colType")}</Th>
              <Th>{null}</Th>
            </tr>
          </Thead>
          <tbody>
            {queue.map((item) => (
              <Tr key={item.proofId}>
                <Td>{item.studentName}</Td>
                <Td>
                  <span className="font-medium">{formatDocType(item.studentIdentityDocumentType)}</span>
                  <span className="ml-1 text-k-muted">{item.studentIdentityNumber}</span>
                </Td>
                <Td>{item.planName}</Td>
                <Td muted>{formatDateTime(item.uploadedAt)}</Td>
                <Td muted>{item.contentType}</Td>
                <Td right>
                  <Button variant="primary" size="sm" onClick={() => setSelected(item)}>
                    {t("reviewBtn")}
                  </Button>
                </Td>
              </Tr>
            ))}
          </tbody>
        </Table>
      )}

      {selected && (
        <ProofReviewModal
          proof={selected}
          onClose={() => setSelected(null)}
          onDone={() => handleDone(selected.proofId)}
        />
      )}
    </div>
  );
}
