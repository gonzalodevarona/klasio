import { ProgramStatus } from "@/lib/types/program";

interface ProgramStatusBadgeProps {
  status: ProgramStatus;
}

const STATUS_STYLES: Record<ProgramStatus, string> = {
  ACTIVE: "bg-green-100 text-green-800",
  INACTIVE: "bg-red-100 text-red-800",
};

export default function ProgramStatusBadge({ status }: ProgramStatusBadgeProps) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[status]}`}
    >
      {status}
    </span>
  );
}
