import { NextRequest, NextResponse } from "next/server";

const API_BASE_URL = process.env.BACKEND_URL ?? "http://localhost:8080/api/v1";

// Upload a payment proof file (multipart/form-data).
// XHR on the client sends credentials (cookies) — we forward them here.
export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ membershipId: string }> }
) {
  const { membershipId } = await params;

  const cookieHeader = request.headers.get("cookie") ?? "";
  const formData = await request.formData();

  const backendResponse = await fetch(
    `${API_BASE_URL}/memberships/${membershipId}/payment-proof`,
    {
      method: "POST",
      headers: { cookie: cookieHeader },
      body: formData,
    }
  );

  const data = await backendResponse.json().catch(() => ({}));
  return NextResponse.json(data, { status: backendResponse.status });
}

// Get the active proof for a membership.
export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ membershipId: string }> }
) {
  const { membershipId } = await params;
  const cookieHeader = request.headers.get("cookie") ?? "";

  const backendResponse = await fetch(
    `${API_BASE_URL}/memberships/${membershipId}/payment-proof`,
    { headers: { cookie: cookieHeader } }
  );

  const data = await backendResponse.json().catch(() => ({}));
  return NextResponse.json(data, { status: backendResponse.status });
}
