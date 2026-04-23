import { NextRequest, NextResponse } from "next/server";

const API_BASE_URL =
  process.env.BACKEND_URL ?? "http://localhost:8080/api/v1";

const LOCALE_MAX_AGE = 60 * 60 * 8; // 8 hours — matches accessToken lifetime

async function fetchTenantLanguage(accessToken: string): Promise<string> {
  try {
    const res = await fetch(`${API_BASE_URL}/me/tenant`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    if (!res.ok) return "es";
    const data = await res.json();
    const lang = data?.language;
    return lang === "en" || lang === "es" ? lang : "es";
  } catch {
    return "es";
  }
}

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

  if (backendResponse.ok) {
    const { userId, role, tenantId } = data as {
      userId: string;
      role: string;
      tenantId: string | null;
      dashboardUrl: string;
    };

    const userInfoValue = encodeURIComponent(
      JSON.stringify({ userId, roles: [role], tenantId: tenantId || null })
    );
    nextResponse.headers.append(
      "Set-Cookie",
      `userInfo=${userInfoValue}; Path=/; SameSite=Lax; Max-Age=${LOCALE_MAX_AGE}`
    );

    // Resolve locale: superadmin has no tenantId → "en"
    // Tenant user: read language from /me/tenant using the accessToken just issued
    let locale = "en";
    if (tenantId) {
      // Extract accessToken value from the Set-Cookie headers the backend just sent
      const atCookie = setCookieHeaders.find((c) => c.startsWith("accessToken="));
      const accessToken = atCookie
        ? atCookie.split(";")[0].substring("accessToken=".length)
        : null;

      locale = accessToken ? await fetchTenantLanguage(accessToken) : "es";
    }

    nextResponse.headers.append(
      "Set-Cookie",
      `NEXT_LOCALE=${locale}; Path=/; SameSite=Lax; Max-Age=${LOCALE_MAX_AGE}`
    );
  }

  return nextResponse;
}
