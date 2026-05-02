/**
 * Frontend mirror of backend AttendanceTimeConstants.
 * Keep in sync with api/src/main/java/com/klasio/attendance/AttendanceTimeConstants.java.
 */
export const AttendanceTimeConstants = {
  CANCELLATION_CUTOFF_MINUTES: 10,
  REGISTRATION_CUTOFF_MINUTES: 30,
  MARKING_WINDOW_MINUTES_BEFORE: 20,
  MARKING_WINDOW_MINUTES_AFTER: 10,
  CORRECTION_WINDOW_HOURS: 24,
} as const;

export const TENANT_TIMEZONE = "America/Bogota";

/**
 * Returns the current date as a YYYY-MM-DD string in the tenant timezone (America/Bogota).
 * Never use new Date().toISOString().split("T")[0] — that gives the UTC date, which
 * can be one day behind local time after ~7 PM in Bogotá.
 */
export function todayInTenantZone(): string {
  return new Intl.DateTimeFormat("en-CA", { timeZone: TENANT_TIMEZONE }).format(new Date());
}

/**
 * Adds `days` calendar days to a YYYY-MM-DD string and returns the result.
 * Operates purely on the date string — no timezone conversion.
 */
export function addDays(dateStr: string, days: number): string {
  const [y, m, d] = dateStr.split("-").map(Number);
  const dt = new Date(Date.UTC(y, m - 1, d + days));
  return dt.toISOString().split("T")[0];
}

/**
 * Formats a YYYY-MM-DD string for display in the tenant locale without timezone shift.
 * new Date("2026-04-20") parses as UTC midnight and shifts back one day in GMT-5.
 * This function avoids that by splitting the string directly.
 */
export function formatSessionDate(dateStr: string): string {
  const [y, m, d] = dateStr.split("-").map(Number);
  const date = new Date(y, m - 1, d);
  const month = date.toLocaleDateString(undefined, { month: "short" });
  return `${month} ${d}`;
}
