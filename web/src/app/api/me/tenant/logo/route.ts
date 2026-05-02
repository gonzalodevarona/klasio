import { NextRequest, NextResponse } from "next/server";

const API_BASE_URL = process.env.BACKEND_URL ?? "http://localhost:8080/api/v1";

export async function GET(request: NextRequest) {
  const cookieHeader = request.headers.get("cookie") ?? "";

  const tenantRes = await fetch(`${API_BASE_URL}/me/tenant`, {
    headers: { cookie: cookieHeader },
  }).catch(() => null);

  if (!tenantRes?.ok) {
    return new NextResponse(null, { status: 404 });
  }

  const tenant = await tenantRes.json().catch(() => null);
  const logoUrl: string | null = tenant?.logoUrl ?? null;

  if (!logoUrl) {
    return new NextResponse(null, { status: 404 });
  }

  const imageRes = await fetch(logoUrl).catch(() => null);
  if (!imageRes?.ok) {
    return new NextResponse(null, { status: 404 });
  }

  const contentType = imageRes.headers.get("content-type") ?? "image/jpeg";
  const buffer = await imageRes.arrayBuffer();

  return new NextResponse(buffer, {
    headers: {
      "content-type": contentType,
      "cache-control": "private, max-age=3600",
    },
  });
}
