import { NextRequest, NextResponse } from "next/server";

const API_BASE_URL = process.env.BACKEND_URL ?? "http://localhost:8080/api/v1";

export async function POST(request: NextRequest) {
  const cookieHeader = request.headers.get("cookie") ?? "";

  const backendResponse = await fetch(`${API_BASE_URL}/me/notifications/mark-all-read`, {
    method: "POST",
    headers: { cookie: cookieHeader },
  });

  return new NextResponse(null, { status: backendResponse.status });
}
