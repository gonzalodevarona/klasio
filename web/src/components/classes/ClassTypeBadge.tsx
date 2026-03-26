import { ClassType } from "@/lib/types/programClass";

interface ClassTypeBadgeProps {
  type: ClassType;
}

const TYPE_STYLES: Record<ClassType, string> = {
  RECURRING: "bg-blue-100 text-blue-800",
  ONE_TIME: "bg-purple-100 text-purple-800",
};

const TYPE_LABELS: Record<ClassType, string> = {
  RECURRING: "Recurring",
  ONE_TIME: "One-Time",
};

export default function ClassTypeBadge({ type }: ClassTypeBadgeProps) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${TYPE_STYLES[type]}`}
    >
      {TYPE_LABELS[type]}
    </span>
  );
}
