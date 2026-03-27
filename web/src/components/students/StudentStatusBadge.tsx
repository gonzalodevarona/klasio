import { StudentStatus } from "@/lib/types/student";

interface StudentStatusBadgeProps {
  status: StudentStatus;
}

const STATUS_STYLES: Record<StudentStatus, string> = {
  ACTIVE: "bg-green-100 text-green-800",
  INACTIVE: "bg-red-100 text-red-800",
};

export default function StudentStatusBadge({ status }: StudentStatusBadgeProps) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[status]}`}
    >
      {status}
    </span>
  );
}
