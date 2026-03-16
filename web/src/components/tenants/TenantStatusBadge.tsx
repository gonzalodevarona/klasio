import { TenantStatus } from "@/lib/types/tenant";

interface TenantStatusBadgeProps {
  status: TenantStatus;
}

const STATUS_STYLES: Record<TenantStatus, string> = {
  ACTIVE: "bg-green-100 text-green-800",
  INACTIVE: "bg-red-100 text-red-800",
};

export default function TenantStatusBadge({ status }: TenantStatusBadgeProps) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[status]}`}
    >
      {status}
    </span>
  );
}
