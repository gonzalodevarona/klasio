import { MembershipStatus } from "@/lib/types/membership";

interface MembershipStatusBadgeProps {
  status: MembershipStatus;
}

// Color scale: red (no active membership) → green (active)
const STATUS_STYLES: Record<MembershipStatus, string> = {
  EXPIRED:                    "bg-red-100 text-red-800",
  INACTIVE:                   "bg-orange-100 text-orange-800",
  PENDING_PAYMENT:            "bg-red-50 text-red-700 border border-red-200",
  PENDING_PAYMENT_VALIDATION: "bg-orange-50 text-orange-700 border border-orange-200",
  PENDING_MANAGER_ACTIVATION: "bg-amber-100 text-amber-800",
  ACTIVE:                     "bg-green-100 text-green-800",
};

const STATUS_LABELS: Record<MembershipStatus, string> = {
  EXPIRED:                    "Expired",
  INACTIVE:                   "Inactive",
  PENDING_PAYMENT:            "Pending Payment",
  PENDING_PAYMENT_VALIDATION: "Under Review",
  PENDING_MANAGER_ACTIVATION: "Pending Activation",
  ACTIVE:                     "Active",
};

export default function MembershipStatusBadge({ status }: MembershipStatusBadgeProps) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[status]}`}
    >
      {STATUS_LABELS[status] ?? status}
    </span>
  );
}
