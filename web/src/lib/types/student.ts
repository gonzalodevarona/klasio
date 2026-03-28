export type StudentStatus = "ACTIVE" | "INACTIVE";

export type IdentityDocumentType = "CC" | "TI" | "CE" | "PA" | "RC";

export const IDENTITY_DOCUMENT_TYPES: { value: IdentityDocumentType; label: string }[] = [
  { value: "CC", label: "Cédula de Ciudadanía" },
  { value: "TI", label: "Tarjeta de Identidad" },
  { value: "CE", label: "Cédula de Extranjería" },
  { value: "PA", label: "Pasaporte" },
  { value: "RC", label: "Registro Civil" },
];

export const BLOOD_TYPES = ["O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-"] as const;
export type BloodType = (typeof BLOOD_TYPES)[number];

export interface StudentSummary {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  identityNumber: string;
  identityDocumentType: string;
  age: number;
  status: StudentStatus;
  hasActiveMembership: boolean;
  createdAt: string;
}

export interface StudentDetail extends StudentSummary {
  tenantId: string;
  dateOfBirth: string;
  eps: string;
  bloodType: string | null;
  phone: string | null;
  tutorFirstName: string | null;
  tutorLastName: string | null;
  tutorRelationship: string | null;
  tutorPhone: string | null;
  tutorEmail: string | null;
  createdBy: string;
  updatedAt: string | null;
  updatedBy: string | null;
  enrollments: EnrollmentSummary[];
}

export interface StudentListResponse {
  content: StudentSummary[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateStudentRequest {
  firstName: string;
  lastName: string;
  email: string;
  dateOfBirth: string;
  eps: string;
  identityNumber: string;
  identityDocumentType: string;
  bloodType?: string | null;
  phone?: string | null;
  tutorFirstName?: string | null;
  tutorLastName?: string | null;
  tutorRelationship?: string | null;
  tutorPhone?: string | null;
  tutorEmail?: string | null;
}

export interface UpdateStudentRequest {
  firstName: string;
  lastName: string;
  email: string;
  dateOfBirth: string;
  eps: string;
  identityNumber: string;
  identityDocumentType: string;
  bloodType?: string | null;
  phone?: string | null;
  tutorFirstName?: string | null;
  tutorLastName?: string | null;
  tutorRelationship?: string | null;
  tutorPhone?: string | null;
  tutorEmail?: string | null;
}

// Re-export enrollment types used in student detail
import { EnrollmentSummary } from "./enrollment";
export type { EnrollmentSummary as StudentEnrollmentSummary };
