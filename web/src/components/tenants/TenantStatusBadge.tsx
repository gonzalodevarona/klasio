import { Badge, type BadgeVariant } from "@/components/ui";
import { TenantStatus } from "@/lib/types/tenant";

interface TenantStatusBadgeProps {
  status: TenantStatus;
}

const STATUS_VARIANT: Record<TenantStatus, BadgeVariant> = {
  ACTIVE:   "active",
  INACTIVE: "inactive",
};

export default function TenantStatusBadge({ status }: TenantStatusBadgeProps) {
  return <Badge variant={STATUS_VARIANT[status]} label={status} />;
}
