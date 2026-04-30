import { cn } from "@/lib/utils";

interface KLogoProps {
  size?: number;
  className?: string;
}

export default function KLogo({ size = 24, className }: KLogoProps) {
  return (
    <div className={cn("flex items-center gap-2 select-none", className)}>
      <img src="/logo.svg" alt="" width={size} height={size} />
      <span className="text-[18px] font-extrabold text-white tracking-[-0.03em] leading-none">
        klasio
      </span>
    </div>
  );
}
