import { NextRequest, NextResponse } from "next/server";
import { jwtVerify } from "jose";

const JWT_SECRET = new TextEncoder().encode(
  process.env.JWT_SECRET ??
    "local-dev-secret-key-that-is-at-least-256-bits-long-for-hs256-signing"
);

const PUBLIC_PATHS = [
  "/login",
  "/register",
  "/forgot-password",
  "/reset-password",
  "/verify-email",
  "/api/auth",
];

const ROLE_PREFIXES: Record<string, string> = {
  SUPERADMIN: "/superadmin",
  ADMIN: "/admin",
  MANAGER: "/manager",
  PROFESSOR: "/professor",
  STUDENT: "/student",
};

// Management routes with explicit role allowlists.
// Uses exact-boundary matching (prefix + "/" or exact match) to avoid
// substring collisions like /professor matching /professors.
const MANAGEMENT_ROUTE_PERMISSIONS: Array<{ prefix: string; roles: string[] }> = [
  { prefix: "/tenants",    roles: ["SUPERADMIN"] },
  { prefix: "/professors", roles: ["SUPERADMIN", "ADMIN", "MANAGER"] },
  { prefix: "/students",   roles: ["SUPERADMIN", "ADMIN", "MANAGER"] },
  { prefix: "/programs",   roles: ["SUPERADMIN", "ADMIN", "MANAGER"] },
  { prefix: "/classes",    roles: ["SUPERADMIN", "ADMIN", "MANAGER", "PROFESSOR"] },
  { prefix: "/plans",      roles: ["SUPERADMIN", "ADMIN", "MANAGER"] },
];

function matchesPrefix(pathname: string, prefix: string): boolean {
  return pathname === prefix || pathname.startsWith(prefix + "/");
}

export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Allow public paths
  if (PUBLIC_PATHS.some((p) => pathname.startsWith(p)) || pathname === "/") {
    return NextResponse.next();
  }

  const accessToken = request.cookies.get("accessToken")?.value;

  if (!accessToken) {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  try {
    const { payload } = await jwtVerify(accessToken, JWT_SECRET);
    const roles = payload.roles as string[] | undefined;
    const role = roles?.[0];

    if (!role) {
      return NextResponse.redirect(new URL("/login", request.url));
    }

    const ownDashboard = `${ROLE_PREFIXES[role] ?? ""}/dashboard`;

    // Guard role-dashboard routes: prevent accessing another role's dashboard prefix.
    // Uses boundary matching to avoid /professor matching /professors.
    for (const [r, prefix] of Object.entries(ROLE_PREFIXES)) {
      if (matchesPrefix(pathname, prefix) && r !== role) {
        return NextResponse.redirect(new URL(ownDashboard, request.url));
      }
    }

    // Guard management routes: redirect to own dashboard if role is not allowed.
    for (const { prefix, roles: allowed } of MANAGEMENT_ROUTE_PERMISSIONS) {
      if (matchesPrefix(pathname, prefix) && !allowed.includes(role)) {
        return NextResponse.redirect(new URL(ownDashboard, request.url));
      }
    }

    return NextResponse.next();
  } catch {
    // JWT expired or invalid — redirect to login
    return NextResponse.redirect(new URL("/login", request.url));
  }
}

export const config = {
  matcher: [
    "/((?!_next/static|_next/image|favicon.ico|api/).*)",
  ],
};
