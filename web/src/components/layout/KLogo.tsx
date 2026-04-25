import Image from "next/image";
import { cn } from "@/lib/utils";

interface KLogoProps {
  size?: number;
  className?: string;
}

export default function KLogo({ size = 28, className }: KLogoProps) {
  return (
    <Image
      src="/logo.svg"
      alt="Klasio"
      width={size}
      height={size}
      className={cn("select-none", className)}
      priority
    />
  );
}
