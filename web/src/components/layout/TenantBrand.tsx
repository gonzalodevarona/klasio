import { cn } from "@/lib/utils";

interface TenantBrandProps {
  tenantName: string | null;
  tenantLogoUrl: string | null;
  loading: boolean;
  className?: string;
}

export default function TenantBrand({
  tenantName,
  tenantLogoUrl,
  loading,
  className,
}: TenantBrandProps) {
  if (loading) {
    return (
      <div
        className={cn("h-4 w-28 bg-k-sidebar-active rounded animate-pulse", className)}
        aria-hidden="true"
      />
    );
  }

  return (
    <div className={cn("flex items-center gap-2 select-none min-w-0", className)}>
      {tenantLogoUrl && (
        <img
          src={tenantLogoUrl}
          alt={tenantName ?? ""}
          width={24}
          height={24}
          className="shrink-0 rounded-sm object-contain"
          onError={(e) => { e.currentTarget.style.display = "none"; }}
        />
      )}
      <span className="text-[13px] font-semibold text-white leading-none truncate">
        {tenantName}
      </span>
    </div>
  );
}
