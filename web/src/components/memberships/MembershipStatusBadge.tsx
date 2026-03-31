import { MembershipStatus } from "@/lib/types/membership";

interface MembershipStatusBadgeProps {
  status: MembershipStatus;
}

const STATUS_STYLES: Record<MembershipStatus, string> = {
  ACTIVE: "bg-green-100 text-green-800",
  INACTIVE: "bg-yellow-100 text-yellow-800",
  EXPIRED: "bg-gray-100 text-gray-700",
  PENDING_PAYMENT_VALIDATION: "bg-blue-100 text-blue-800",
  PENDING_MANAGER_ACTIVATION: "bg-blue-100 text-blue-700",
};

const STATUS_LABELS: Record<MembershipStatus, string> = {
  ACTIVE: "Active",
  INACTIVE: "Inactive",
  EXPIRED: "Expired",
  PENDING_PAYMENT_VALIDATION: "Pending Payment",
  PENDING_MANAGER_ACTIVATION: "Pending Activation",
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
