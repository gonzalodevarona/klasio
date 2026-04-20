import { NextRequest, NextResponse } from "next/server";

const API_BASE_URL = process.env.BACKEND_URL ?? "http://localhost:8080/api/v1";

export async function PATCH(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;
  const cookieHeader = request.headers.get("cookie") ?? "";

  const backendResponse = await fetch(`${API_BASE_URL}/me/notifications/${id}/read`, {
    method: "PATCH",
    headers: { cookie: cookieHeader },
  });

  return new NextResponse(null, { status: backendResponse.status });
}
