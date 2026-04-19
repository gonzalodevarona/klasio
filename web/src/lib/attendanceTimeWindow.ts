import { AttendanceTimeConstants } from "./attendanceConstants";

/** Timezone used by all tenants (Colombia). */
const TENANT_TZ = "America/Bogota";

/** Returns the current wall-clock time interpreted as if it were in the tenant timezone. */
function tenantNowAsLocalDate(): Date {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone: TENANT_TZ,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).formatToParts(new Date());

  const get = (type: string) =>
    parseInt(parts.find((p) => p.type === type)?.value ?? "0", 10);

  return new Date(
    get("year"),
    get("month") - 1,
    get("day"),
    get("hour"),
    get("minute"),
    get("second")
  );
}

function buildWindowDates(
  sessionDate: string,
  startTime: string,
  endTime: string
): { windowOpen: Date; windowClose: Date } {
  const [startH, startM] = startTime.split(":").map(Number);
  const [endH, endM] = endTime.split(":").map(Number);
  const [syear, smonth, sday] = sessionDate.split("-").map(Number);

  const sessionStart = new Date(syear, smonth - 1, sday, startH, startM, 0);
  const sessionEnd   = new Date(syear, smonth - 1, sday, endH,   endM,   0);

  return {
    windowOpen: new Date(
      sessionStart.getTime() -
        AttendanceTimeConstants.MARKING_WINDOW_MINUTES_BEFORE * 60 * 1000
    ),
    windowClose: new Date(
      sessionEnd.getTime() +
        AttendanceTimeConstants.MARKING_WINDOW_MINUTES_AFTER * 60 * 1000
    ),
  };
}

/**
 * Returns true if the current moment is within the marking window:
 *   [ sessionStart - 20min , sessionEnd + 10min ]
 */
export function isWithinMarkingWindow(
  sessionDate: string,
  startTime: string,
  endTime: string
): boolean {
  const now = tenantNowAsLocalDate();
  const { windowOpen, windowClose } = buildWindowDates(sessionDate, startTime, endTime);
  return now >= windowOpen && now <= windowClose;
}

/**
 * Returns milliseconds until the marking window opens (negative if already open or past).
 * Uses real wall-clock Date.now() for accurate timer scheduling.
 */
export function msUntilWindowOpen(
  sessionDate: string,
  startTime: string,
  endTime: string
): number {
  const { windowOpen } = buildWindowDates(sessionDate, startTime, endTime);
  // windowOpen was built using local Date arithmetic — convert to UTC ms via getTime()
  // The values match because tenantNowAsLocalDate() uses the same local-time trick.
  const nowLocal = tenantNowAsLocalDate();
  return windowOpen.getTime() - nowLocal.getTime();
}
