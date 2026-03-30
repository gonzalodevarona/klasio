import { NextRequest, NextResponse } from "next/server";

const API_BASE_URL =
  process.env.BACKEND_URL ?? "http://localhost:8080/api/v1";

export async function POST(request: NextRequest) {
  const cookieHeader = request.headers.get("cookie") ?? "";

  const backendResponse = await fetch(`${API_BASE_URL}/auth/refresh`, {
    method: "POST",
    headers: { Cookie: cookieHeader },
  });

  const data = await backendResponse.json();
  const nextResponse = NextResponse.json(data, {
    status: backendResponse.status,
  });

  // Forward all HttpOnly JWT cookies from backend using headers.append() consistently.
  const setCookieHeaders = backendResponse.headers.getSetCookie();
  for (const cookie of setCookieHeaders) {
    nextResponse.headers.append("Set-Cookie", cookie);
  }

  // Keep the non-HttpOnly userInfo cookie in sync with the refreshed token's claims.
  if (backendResponse.ok) {
    const { userId, role } = data as { userId: string; role: string };
    const existingRaw = request.cookies.get("userInfo")?.value;
    const existing = existingRaw
      ? (JSON.parse(decodeURIComponent(existingRaw)) as Record<string, unknown>)
      : {};
    const userInfoValue = encodeURIComponent(
      JSON.stringify({ ...existing, userId, role })
    );
    nextResponse.headers.append(
      "Set-Cookie",
      `userInfo=${userInfoValue}; Path=/; SameSite=Lax; Max-Age=${60 * 60 * 8}`
    );
  }

  return nextResponse;
}
