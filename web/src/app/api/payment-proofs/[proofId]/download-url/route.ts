import { NextRequest, NextResponse } from "next/server";

const API_BASE_URL = process.env.BACKEND_URL ?? "http://localhost:8080/api/v1";

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ proofId: string }> }
) {
  const { proofId } = await params;
  const cookieHeader = request.headers.get("cookie") ?? "";

  const backendResponse = await fetch(
    `${API_BASE_URL}/payment-proofs/${proofId}/download-url`,
    { headers: { cookie: cookieHeader } }
  );

  const data = await backendResponse.json().catch(() => ({}));
  return NextResponse.json(data, { status: backendResponse.status });
}
