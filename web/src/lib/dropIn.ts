const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

export interface DropInAttendeeLookupResponse {
  id: string;
  fullName: string;
  phone: string;
  totalVisits: number;
  firstVisitAt: string | null;
  lastVisitAt: string | null;
  converted: boolean;
}

export interface RegisterDropInInput {
  startTime: string;
  attendee:
    | { existingId: string; newAttendee?: never }
    | { existingId?: never; newAttendee: { fullName: string; phone: string } };
  amount: string;
  paymentMethod: "CASH" | "TRANSFER";
}

export interface RegisterDropInResponse {
  registrationId: string;
  attendeeId: string;
  paymentId: string;
  status: string;
  attendeeWasNew: boolean;
  attendeeTotalVisits: number;
}

export async function lookupDropIn(
  phone: string,
  signal?: AbortSignal
): Promise<DropInAttendeeLookupResponse | null> {
  const res = await fetch(
    `${API_BASE}/drop-in-attendees/lookup?phone=${encodeURIComponent(phone)}`,
    { credentials: "include", signal }
  );
  if (res.status === 404) return null;
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw Object.assign(new Error(body?.error?.message ?? res.statusText), {
      status: res.status,
      code: body?.error?.code ?? "UNKNOWN",
    });
  }
  return res.json();
}

export async function registerDropIn(
  classId: string,
  sessionDate: string,
  payload: RegisterDropInInput
): Promise<RegisterDropInResponse> {
  const { api } = await import("@/lib/api");
  return api.post<RegisterDropInResponse>(
    `/classes/${classId}/sessions/${sessionDate}/drop-in`,
    payload
  );
}
