export type ProofStatus = "PENDING" | "APPROVED" | "REJECTED" | "SUPERSEDED";

export interface PaymentProofResponse {
  proofId: string;
  membershipId: string;
  studentId: string;
  status: ProofStatus;
  originalFileName: string;
  contentType: string;
  fileSizeBytes: number;
  uploadedAt: string;
  rejectionReason: string | null;
  validatedAt: string | null;
  validatedBy: string | null;
}

export interface PaymentProofDto {
  proofId: string;
  membershipId: string;
  status: ProofStatus;
  originalFileName: string;
  uploadedAt: string;
  rejectionReason: string | null;
  validatedAt: string | null;
  validatedBy: string | null;
}

export interface ProofQueueItem {
  proofId: string;
  membershipId: string;
  studentName: string;
  programName: string;
  uploadedAt: string;
  contentType: string;
}

export interface DelegatedMembership {
  membershipId: string;
  studentName: string;
  programName: string;
  delegatedAt: string;
  proofId: string | null;
}
