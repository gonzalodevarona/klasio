export type RegistrationStatus =
  | "REGISTERED"
  | "CANCELLED_BY_STUDENT"
  | "CANCELLED_BY_SYSTEM"
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
}

export interface ClassSessionRoster {
  sessionDate: string;
  startTime: string;
  endTime: string;
  registrantCount: number;
  registrants: RosterRegistrantView[];
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
