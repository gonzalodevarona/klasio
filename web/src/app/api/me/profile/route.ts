import { NextRequest, NextResponse } from "next/server";

const API_BASE_URL = process.env.BACKEND_URL ?? "http://localhost:8080/api/v1";

export async function GET(request: NextRequest) {
  const cookieHeader = request.headers.get("cookie") ?? "";

  const backendResponse = await fetch(`${API_BASE_URL}/me/profile`, {
    headers: { cookie: cookieHeader },
  });

  const data = await backendResponse.json().catch(() => ({}));
  return NextResponse.json(data, { status: backendResponse.status });
}
