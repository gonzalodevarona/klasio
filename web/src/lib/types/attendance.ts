export type RegistrationStatus =
  | "REGISTERED"
  | "CANCELLED_BY_STUDENT"
  | "CANCELLED_BY_SYSTEM"
  | "SESSION_CANCELLED"
  | "PRESENT"
  | "PRESENT_NO_HOURS"
  | "ABSENT";

export type SessionStatus = "SCHEDULED" | "ALERTED" | "CANCELLED";

export interface AvailableSession {
  classId: string;
  className: string;
  sessionId: string | null;
  sessionDate: string;
  startTime: string;
  endTime: string;
  level: string;
  programId: string;
  currentCapacity: number;
  maxStudents: number;
  status: SessionStatus;
  registrationOpen: boolean;
  alertReason?: string | null;
  registrationId?: string | null;
  registrationStatus?: string | null;
}

export interface AttendanceStats {
  attended: number;
  cancelledByStudent: number;
  cancelledBySystem: number;
  absent: number;
  totalHoursConsumed: number;
  attendanceRatePercent: number;
}

export interface Registration {
  id: string;
  sessionId: string;
  classId: string;
  className: string;
  sessionDate: string;
  sessionStartTime: string;
  sessionEndTime: string;
  level: string;
  intendedHours: number;
  status: RegistrationStatus;
  createdAt: string;
  sessionCancellationReason?: string | null;
  sessionStatus?: SessionStatus | null;
  sessionAlertReason?: string | null;
}

export interface RegistrationListResponse {
  content: Registration[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface RosterRegistrantView {
  registrationId: string;
  studentId: string;
  studentName: string;
  level: string;
  intendedHours: number;
  status: RegistrationStatus;
  /** userId of the actor who created this registration (null when self-registered). */
  createdBy?: string | null;
}

export interface ClassSessionRoster {
  sessionDate: string;
  startTime: string;
  endTime: string;
  registrantCount: number;
  registrants: RosterRegistrantView[];
  /** Session lifecycle status. Defaults to SCHEDULED when not returned by the API. */
  status?: SessionStatus;
  /** Populated when status is ALERTED. */
  alertReason?: string | null;
  /** Populated when status is CANCELLED. */
  cancellationReason?: string | null;
}

// ── Attendance Marking (RF-25 / RF-26) ───────────────────────────────────────

export interface MarkEntry {
  registrationId: string;
  mark: "PRESENT" | "ABSENT";
}

export interface MarkAttendanceRequest {
  startTime: string; // "HH:mm:ss"
  marks: MarkEntry[];
}

export interface MarkedRegistration {
  registrationId: string;
  status: RegistrationStatus;
  noHoursWarning: boolean;
}

export interface MarkAttendanceResponse {
  results: MarkedRegistration[];
}

export interface CorrectMarkRequest {
  newMark: "PRESENT" | "ABSENT";
  reason: string;
}
