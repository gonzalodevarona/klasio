import { NextRequest, NextResponse } from "next/server";

const API_BASE_URL =
  process.env.BACKEND_URL ?? "http://localhost:8080/api/v1";

export async function POST(request: NextRequest) {
  const body = await request.json();

  const backendResponse = await fetch(`${API_BASE_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  const data = await backendResponse.json();
  const nextResponse = NextResponse.json(data, {
    status: backendResponse.status,
  });

  // Forward all HttpOnly JWT cookies from backend.
  // We use headers.append() for ALL cookies to avoid mixing with cookies.set(),
  // which internally calls headers.set() and would overwrite previously appended cookies.
  const setCookieHeaders = backendResponse.headers.getSetCookie();
  for (const cookie of setCookieHeaders) {
    nextResponse.headers.append("Set-Cookie", cookie);
  }

  // Append a non-HttpOnly cookie with public user info so client-side code can
  // read the role and userId without touching the secure JWT.
  if (backendResponse.ok) {
    const { userId, role, tenantId } = data as {
      userId: string;
      role: string;
      tenantId: string;
      dashboardUrl: string;
    };
    const userInfoValue = encodeURIComponent(
      JSON.stringify({ userId, role, tenantId: tenantId || null })
    );
    nextResponse.headers.append(
      "Set-Cookie",
      `userInfo=${userInfoValue}; Path=/; SameSite=Lax; Max-Age=${60 * 60 * 8}`
    );
  }

  return nextResponse;
}
