import { NextRequest, NextResponse } from "next/server";

const API_BASE_URL = process.env.BACKEND_URL ?? "http://localhost:8080/api/v1";

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ programId: string }> }
) {
  const { programId } = await params;
  const cookieHeader = request.headers.get("cookie") ?? "";

  const backendResponse = await fetch(
    `${API_BASE_URL}/programs/${programId}/delegated-memberships`,
    { headers: { cookie: cookieHeader } }
  );

  const data = await backendResponse.json().catch(() => ({}));
  return NextResponse.json(data, { status: backendResponse.status });
}
