export type Role = "SUPERADMIN" | "ADMIN" | "MANAGER" | "PROFESSOR" | "STUDENT";

export type UserStatus = "ACTIVE" | "INVITED" | "INACTIVE";

/** Ordered from highest to lowest privilege. */
const ROLE_HIERARCHY: Role[] = ["SUPERADMIN", "ADMIN", "MANAGER", "PROFESSOR", "STUDENT"];

/** Returns the highest-privilege role from the given array. */
export function primaryRole(roles: Role[]): Role {
  return ROLE_HIERARCHY.find(r => roles.includes(r)) ?? roles[0];
}

export interface User {
  id: string;
  email: string;
  roles: Role[];
  tenantId: string | null;
  status: UserStatus;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  userId: string;
  role: Role;          // backend returns single primary role
  tenantId: string | null;
  dashboardUrl: string;
}

export interface RefreshResponse {
  userId: string;
  role: string;
}

export interface StudentRegistrationRequest {
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  documentType: string;
  documentNumber: string;
  eps: string;
  email: string;
  password: string;
  tutorFullName?: string;
  tutorRelationship?: string;
  tutorContact?: string;
}

export interface RegistrationResponse {
  userId: string;
  message: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface UserSummary {
  id: string;
  email: string;
  roles: Role[];
  tenantId: string | null;
  status: UserStatus;
}

export interface AuthError {
  error: {
    code: string;
    message: string;
    lockedUntil?: string;
    violations?: string[];
    details?: Array<{ field: string; message: string }>;
  };
}
