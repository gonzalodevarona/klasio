import { ClassLevel } from "@/lib/types/programClass";

interface ClassLevelBadgeProps {
  level: ClassLevel;
}

const LEVEL_STYLES: Record<ClassLevel, string> = {
  BEGINNER: "bg-green-100 text-green-800",
  INTERMEDIATE: "bg-yellow-100 text-yellow-800",
  ADVANCED: "bg-red-100 text-red-800",
};

const LEVEL_LABELS: Record<ClassLevel, string> = {
  BEGINNER: "Beginner",
  INTERMEDIATE: "Intermediate",
  ADVANCED: "Advanced",
};

export default function ClassLevelBadge({ level }: ClassLevelBadgeProps) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${LEVEL_STYLES[level]}`}
    >
      {LEVEL_LABELS[level]}
    </span>
  );
}
