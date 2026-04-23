export type TenantStatus = "ACTIVE" | "INACTIVE";

export interface TenantSummary {
  id: string;
  slug: string;
  name: string;
  discipline: string;
  status: TenantStatus;
  createdAt: string;
}

export interface TenantDetail extends TenantSummary {
  language: string;
  logoUrl: string | null;
  contactEmail: string;
  contactPhone: string;
  contactPhoneIndicator: string;
  contactStreet: string;
  contactCity: string;
  contactState: string;
  contactCountry: string;
  createdBy: string;
  deactivatedAt: string | null;
  deactivatedBy: string | null;
}

export interface TenantListResponse {
  content: TenantSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateTenantRequest {
  name: string;
  discipline: string;
  language: string;
  slug?: string;
  contactEmail: string;
  contactPhone: string;
  contactPhoneIndicator: string;
  contactStreet: string;
  contactCity: string;
  contactState: string;
  contactCountry: string;
  logo: File;
}
