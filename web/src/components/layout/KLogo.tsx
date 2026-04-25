import { cn } from "@/lib/utils";

interface KLogoProps {
  className?: string;
}

export default function KLogo({ className }: KLogoProps) {
  return (
    <span
      className={cn(
        "text-[18px] font-extrabold text-white tracking-[-0.03em] leading-none select-none",
        className,
      )}
    >
      klasio
    </span>
  );
}
