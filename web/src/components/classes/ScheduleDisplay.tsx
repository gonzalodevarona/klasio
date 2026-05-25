import { ClassScheduleEntry, ClassType } from "@/lib/types/programClass";

interface ScheduleDisplayProps {
  entries: ClassScheduleEntry[];
  type: ClassType;
}

function capitalize(value: string): string {
  return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
}

function formatDayOfWeek(dayOfWeek: string): string {
  return capitalize(dayOfWeek.slice(0, 3));
}

function formatSpecificDate(dateStr: string): string {
  const date = new Date(dateStr + "T00:00:00");
  return date.toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

function formatTimeRange(startTime: string, endTime: string): string {
  return `${startTime.slice(0, 5)}-${endTime.slice(0, 5)}`;
}

export default function ScheduleDisplay({ entries, type }: ScheduleDisplayProps) {
  if (type === "RECURRING") {
    const grouped = new Map<string, { days: string[]; location?: string | null }>();

    for (const entry of entries) {
      const timeRange = formatTimeRange(entry.startTime, entry.endTime);
      const day = entry.dayOfWeek ? formatDayOfWeek(entry.dayOfWeek) : "";
      const key = `${timeRange}|${entry.location ?? ""}`;

      if (!grouped.has(key)) {
        grouped.set(key, { days: [], location: entry.location });
      }
      grouped.get(key)!.days.push(day);
    }

    return (
      <div>
        {Array.from(grouped.entries()).map(([key, { days, location }]) => (
          <p key={key} className="text-sm text-gray-700">
            {days.join(", ")} {key.split("|")[0]}
            {location ? ` · ${location}` : ""}
          </p>
        ))}
      </div>
    );
  }

  return (
    <div>
      {entries.map((entry, index) => (
        <p key={index} className="text-sm text-gray-700">
          {entry.specificDate ? formatSpecificDate(entry.specificDate) : ""}{" "}
          {formatTimeRange(entry.startTime, entry.endTime)}
          {entry.location ? ` · ${entry.location}` : ""}
        </p>
      ))}
    </div>
  );
}
