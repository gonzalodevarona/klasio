export type MembershipStatus =
  | "PENDING_PAYMENT"
  | "PENDING_PAYMENT_VALIDATION"
  | "PENDING_MANAGER_ACTIVATION"
  | "ACTIVE"
  | "INACTIVE"
  | "EXPIRED";

export type HourTransactionType =
  | "ATTENDANCE_DEDUCTION"
  | "MANUAL_ADDITION"
  | "MANUAL_SUBTRACTION";

export interface MembershipSummary {
  id: string;
  studentId: string;
  programId: string;
  planId: string;
  planName: string;
  purchasedHours: number;
  availableHours: number;
  startDate: string;
  expirationDate: string;
  status: MembershipStatus;
  paymentValidated: boolean;
  createdAt: string;
}

export interface MembershipDetail extends MembershipSummary {
  tenantId: string;
  studentName: string;
  programName: string;
  enrollmentId: string;
  paymentValidatedBy: string | null;
  paymentValidatedAt: string | null;
  activatedBy: string | null;
  activatedAt: string | null;
  createdBy: string;
  updatedAt: string | null;
  updatedBy: string | null;
}

export interface HourTransactionSummary {
  id: string;
  membershipId: string;
  type: HourTransactionType;
  delta: number;
  reason: string | null;
  actorId: string;
  actorRole: string;
  createdAt: string;
}

export interface MembershipHistoryEntry {
  id: string;
  purchasedHours: number;
  consumedHours: number;
  availableHours: number;
  startDate: string;
  expirationDate: string;
  status: MembershipStatus;
  activatedAt: string | null;
}

export interface MembershipListResponse {
  content: MembershipSummary[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

export interface HourTransactionListResponse {
  content: HourTransactionSummary[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

export interface CreateMembershipRequest {
  studentId: string;
  planId: string;
  startDate: string;
  paymentValidated: boolean;
  activateDirectly: boolean;
}

export interface ValidatePaymentRequest {
  activateDirectly: boolean;
}

export interface AdjustHoursRequest {
  delta: number;
  reason: string;
}

export interface CreateSelfMembershipRequest {
  planId: string;
}
