import { Level } from "@/lib/types/enrollment";

interface LevelBadgeProps {
  level: Level;
}

const LEVEL_STYLES: Record<Level, string> = {
  BEGINNER: "bg-green-100 text-green-800",
  INTERMEDIATE: "bg-yellow-100 text-yellow-800",
  ADVANCED: "bg-purple-100 text-purple-800",
};

const LEVEL_LABELS: Record<Level, string> = {
  BEGINNER: "Beginner",
  INTERMEDIATE: "Intermediate",
  ADVANCED: "Advanced",
};

export default function LevelBadge({ level }: LevelBadgeProps) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${LEVEL_STYLES[level]}`}
    >
      {LEVEL_LABELS[level]}
    </span>
  );
}
