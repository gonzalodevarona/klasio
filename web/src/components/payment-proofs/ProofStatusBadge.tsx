import { ProofStatus } from "@/lib/types/paymentProof";

const STATUS_STYLES: Record<ProofStatus, string> = {
  PENDING:    "bg-yellow-100 text-yellow-800 border border-yellow-300",
  APPROVED:   "bg-green-100 text-green-800 border border-green-300",
  REJECTED:   "bg-red-100 text-red-800 border border-red-300",
  SUPERSEDED: "bg-gray-100 text-gray-500 border border-gray-300",
};

const STATUS_LABELS: Record<ProofStatus, string> = {
  PENDING:    "Pending Review",
  APPROVED:   "Approved",
  REJECTED:   "Rejected",
  SUPERSEDED: "Superseded",
};

interface Props {
  status: ProofStatus;
}

export function ProofStatusBadge({ status }: Props) {
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${STATUS_STYLES[status]}`}>
      {STATUS_LABELS[status]}
    </span>
  );
}
