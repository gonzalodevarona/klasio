import { NextRequest, NextResponse } from "next/server";

const API_BASE_URL =
  process.env.BACKEND_URL ?? "http://localhost:8080/api/v1";

export async function POST(request: NextRequest) {
  const cookieHeader = request.headers.get("cookie") ?? "";

  await fetch(`${API_BASE_URL}/auth/logout`, {
    method: "POST",
    headers: { Cookie: cookieHeader },
  });

  const response = NextResponse.json({ message: "Logged out" });

  response.cookies.set("accessToken", "", { maxAge: 0, path: "/" });
  response.cookies.set("refreshToken", "", { maxAge: 0, path: "/" });
  response.cookies.set("userInfo", "", { maxAge: 0, path: "/" });
  response.cookies.set("NEXT_LOCALE", "", { maxAge: 0, path: "/" });

  return response;
}
