import { NextRequest, NextResponse } from "next/server";

const API_BASE_URL = process.env.BACKEND_URL ?? "http://localhost:8080/api/v1";

export async function POST(request: NextRequest) {
  const slug = request.headers.get("x-tenant-slug");
  if (!slug) {
    return NextResponse.json(
      { error: { code: "TENANT_REQUIRED", message: "Missing tenant" } },
      { status: 400 }
    );
  }
  const body = await request.json();
  const backendResponse = await fetch(
    `${API_BASE_URL}/tenants/${encodeURIComponent(slug)}/register`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }
  );
  const data = await backendResponse.json().catch(() => ({}));
  return NextResponse.json(data, { status: backendResponse.status });
}
