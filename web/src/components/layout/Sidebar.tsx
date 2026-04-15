"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { useSidebarIdentity } from "@/hooks/useSidebarIdentity";
import type { Role } from "@/lib/types/auth";
import {
  Building2,
  BookOpen,
  GraduationCap,
  Users,
  ListChecks,
  CalendarDays,
  FileCheck,
  LayoutDashboard,
  BadgeCheck,
  ClipboardList,
  Calendar,
  LogOut,
  Menu,
  X,
  ChevronLeft,
  type LucideProps,
} from "lucide-react";

type IconComponent = React.ComponentType<LucideProps>;

interface NavItem {
  label: string;
  href: string;
  icon: IconComponent;
}

const NAV_ITEMS_BY_ROLE: Record<Role, NavItem[]> = {
  SUPERADMIN: [
    { label: "Tenants",        href: "/tenants",        icon: Building2 },
    { label: "Programs",       href: "/programs",       icon: BookOpen },
    { label: "Professors",     href: "/professors",     icon: GraduationCap },
    { label: "Students",       href: "/students",       icon: Users },
    { label: "Plans",          href: "/plans",          icon: ListChecks },
    { label: "Classes",        href: "/classes",        icon: CalendarDays },
    { label: "Payment Proofs", href: "/payment-proofs", icon: FileCheck },
  ],
  ADMIN: [
    { label: "Programs",       href: "/programs",       icon: BookOpen },
    { label: "Professors",     href: "/professors",     icon: GraduationCap },
    { label: "Students",       href: "/students",       icon: Users },
    { label: "Plans",          href: "/plans",          icon: ListChecks },
    { label: "Classes",        href: "/classes",        icon: CalendarDays },
    { label: "Payment Proofs", href: "/payment-proofs", icon: FileCheck },
  ],
  MANAGER: [
    { label: "Programs",   href: "/programs",   icon: BookOpen },
    { label: "Professors", href: "/professors", icon: GraduationCap },
    { label: "Students",   href: "/students",   icon: Users },
    { label: "Classes",    href: "/classes",    icon: CalendarDays },
  ],
  PROFESSOR: [
    { label: "Classes", href: "/classes", icon: CalendarDays },
  ],
  STUDENT: [
    { label: "Dashboard",      href: "/student/dashboard",   icon: LayoutDashboard },
    { label: "My Memberships", href: "/student/memberships", icon: BadgeCheck },
    { label: "My Enrollments", href: "/student/enrollments", icon: ClipboardList },
    { label: "My Classes",     href: "/student/classes",     icon: Calendar },
  ],
};

// Module-level component so its identity is stable across renders.
function NavLinks({
  items,
  pathname,
  collapsed,
  onLinkClick,
}: {
  items: NavItem[];
  pathname: string;
  collapsed: boolean;
  onLinkClick?: () => void;
}) {
  return (
    <ul className="space-y-1">
      {items.map(({ label, href, icon: Icon }) => {
        const isActive =
          pathname === href || pathname.startsWith(href + "/");
        return (
          <li key={href}>
            <Link
              href={href}
              onClick={onLinkClick}
              title={collapsed ? label : undefined}
              className={[
                "flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors",
                isActive
                  ? "bg-gray-700 text-white"
                  : "text-gray-300 hover:bg-gray-800 hover:text-white",
                collapsed ? "justify-center" : "",
              ].join(" ")}
            >
              <Icon className="h-5 w-5 shrink-0" />
              {!collapsed && <span className="truncate">{label}</span>}
            </Link>
          </li>
        );
      })}
    </ul>
  );
}

// Brand block shown in the sidebar header.
function Brand({
  tenantName,
  collapsed,
}: {
  tenantName: string | null;
  collapsed: boolean;
}) {
  if (collapsed) return null;
  return (
    <div className="overflow-hidden">
      <h1 className="text-xl font-bold text-white whitespace-nowrap">Klasio</h1>
      <p className="text-xs text-gray-400 whitespace-nowrap">League Management</p>
      {tenantName && (
        <p className="text-xs font-medium text-indigo-400 whitespace-nowrap mt-0.5 truncate">
          {tenantName}
        </p>
      )}
    </div>
  );
}

// User identity block shown at the bottom of the sidebar.
function UserFooter({
  role,
  displayName,
  identityDocumentType,
  identityNumber,
  collapsed,
  onLogout,
}: {
  role: Role;
  displayName: string | null;
  identityDocumentType: string | null;
  identityNumber: string | null;
  collapsed: boolean;
  onLogout: () => void;
}) {
  return (
    <div className="px-2 py-4 border-t border-gray-700 shrink-0">
      {!collapsed && (
        <div className="px-3 mb-2 space-y-0.5">
          {displayName && (
            <p className="text-xs font-medium text-white truncate">
              {displayName}
            </p>
          )}
          <p className="text-xs text-gray-400 truncate">{role}</p>
          {identityDocumentType && identityNumber && (
            <p className="text-xs text-gray-500 truncate">
              {identityDocumentType} {identityNumber}
            </p>
          )}
        </div>
      )}
      <button
        onClick={onLogout}
        title={collapsed ? "Sign out" : undefined}
        className={[
          "flex items-center gap-3 w-full px-3 py-2 rounded-md text-sm text-gray-300",
          "hover:text-white hover:bg-gray-800 transition-colors",
          collapsed ? "justify-center" : "",
        ].join(" ")}
      >
        <LogOut className="h-5 w-5 shrink-0" />
        {!collapsed && <span>Sign out</span>}
      </button>
    </div>
  );
}

// Mobile-drawer version of the user footer (never collapsed).
function MobileUserFooter({
  role,
  displayName,
  identityDocumentType,
  identityNumber,
  onLogout,
}: {
  role: Role;
  displayName: string | null;
  identityDocumentType: string | null;
  identityNumber: string | null;
  onLogout: () => void;
}) {
  return (
    <div className="px-3 py-4 border-t border-gray-700 shrink-0">
      <div className="px-3 mb-2 space-y-0.5">
        {displayName && (
          <p className="text-xs font-medium text-white truncate">
            {displayName}
          </p>
        )}
        <p className="text-xs text-gray-400 truncate">{role}</p>
        {identityDocumentType && identityNumber && (
          <p className="text-xs text-gray-500 truncate">
            {identityDocumentType} {identityNumber}
          </p>
        )}
      </div>
      <button
        onClick={onLogout}
        className="flex items-center gap-3 w-full px-3 py-2 rounded-md text-sm text-gray-300 hover:text-white hover:bg-gray-800 transition-colors"
      >
        <LogOut className="h-5 w-5 shrink-0" />
        <span>Sign out</span>
      </button>
    </div>
  );
}

export default function Sidebar() {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const { user, loading, logout } = useAuth();
  const pathname = usePathname();

  const { tenantName, displayName, identityDocumentType, identityNumber } =
    useSidebarIdentity(user?.role, user?.tenantId);

  // Close mobile drawer on route change.
  useEffect(() => {
    setMobileOpen(false);
  }, [pathname]);

  // Close mobile drawer on Escape key.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setMobileOpen(false);
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, []);

  // Prevent body scroll while mobile drawer is open.
  useEffect(() => {
    document.body.style.overflow = mobileOpen ? "hidden" : "";
    return () => {
      document.body.style.overflow = "";
    };
  }, [mobileOpen]);

  if (loading) {
    return (
      <>
        <div className="lg:hidden fixed top-0 inset-x-0 z-40 h-14 bg-gray-900 border-b border-gray-700 animate-pulse" />
        <aside className="hidden lg:flex w-64 bg-gray-900 h-screen sticky top-0 flex-col shrink-0">
          <div className="p-6 space-y-2">
            <div className="h-6 w-24 bg-gray-700 rounded animate-pulse" />
            <div className="h-4 w-32 bg-gray-700 rounded animate-pulse" />
          </div>
        </aside>
      </>
    );
  }

  const navItems = user ? (NAV_ITEMS_BY_ROLE[user.role] ?? []) : [];

  return (
    <>
      {/* ── Mobile: top bar ─────────────────────────────────────── */}
      <header className="lg:hidden fixed top-0 inset-x-0 z-40 flex items-center h-14 px-4 gap-3 bg-gray-900 border-b border-gray-700">
        <button
          onClick={() => setMobileOpen(true)}
          aria-label="Open navigation"
          className="p-1 text-gray-300 hover:text-white rounded transition-colors"
        >
          <Menu className="h-6 w-6" />
        </button>
        <div className="min-w-0">
          <span className="text-lg font-bold text-white">Klasio</span>
          {tenantName && (
            <span className="ml-2 text-xs text-indigo-400 truncate hidden sm:inline">
              {tenantName}
            </span>
          )}
        </div>
      </header>

      {/* ── Mobile: slide-in drawer ──────────────────────────────── */}
      {mobileOpen && (
        <div className="lg:hidden fixed inset-0 z-50 flex">
          <div
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            onClick={() => setMobileOpen(false)}
            aria-hidden="true"
          />

          <aside className="relative flex flex-col w-64 h-full bg-gray-900 shadow-2xl">
            {/* Drawer header */}
            <div className="flex items-center justify-between px-4 py-3 shrink-0 border-b border-gray-700">
              <div className="min-w-0">
                <h1 className="text-lg font-bold text-white">Klasio</h1>
                <p className="text-xs text-gray-400">League Management</p>
                {tenantName && (
                  <p className="text-xs font-medium text-indigo-400 truncate mt-0.5">
                    {tenantName}
                  </p>
                )}
              </div>
              <button
                onClick={() => setMobileOpen(false)}
                aria-label="Close navigation"
                className="p-1 text-gray-400 hover:text-white rounded transition-colors shrink-0 ml-2"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {/* Drawer nav */}
            <nav className="flex-1 px-3 py-4 overflow-y-auto">
              <NavLinks
                items={navItems}
                pathname={pathname}
                collapsed={false}
                onLinkClick={() => setMobileOpen(false)}
              />
            </nav>

            {/* Drawer footer */}
            {user && (
              <MobileUserFooter
                role={user.role}
                displayName={displayName}
                identityDocumentType={identityDocumentType}
                identityNumber={identityNumber}
                onLogout={logout}
              />
            )}
          </aside>
        </div>
      )}

      {/* ── Desktop: collapsible sidebar ─────────────────────────── */}
      <aside
        className={[
          "hidden lg:flex flex-col bg-gray-900 text-white h-screen sticky top-0 shrink-0",
          "transition-[width] duration-300 ease-in-out overflow-hidden",
          collapsed ? "w-16" : "w-64",
        ].join(" ")}
      >
        {/* Sidebar header */}
        <div
          className={[
            "flex items-center shrink-0 border-b border-gray-700 px-3 py-4",
            collapsed ? "justify-center" : "justify-between",
          ].join(" ")}
        >
          <Brand tenantName={tenantName} collapsed={collapsed} />
          <button
            onClick={() => setCollapsed((c) => !c)}
            aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
            className="p-1 text-gray-400 hover:text-white rounded transition-colors shrink-0"
          >
            {collapsed ? (
              <Menu className="h-5 w-5" />
            ) : (
              <ChevronLeft className="h-5 w-5" />
            )}
          </button>
        </div>

        {/* Sidebar nav */}
        <nav className="flex-1 px-2 py-4 overflow-y-auto overflow-x-hidden">
          <NavLinks
            items={navItems}
            pathname={pathname}
            collapsed={collapsed}
          />
        </nav>

        {/* Sidebar footer */}
        {user && (
          <UserFooter
            role={user.role}
            displayName={displayName}
            identityDocumentType={identityDocumentType}
            identityNumber={identityNumber}
            collapsed={collapsed}
            onLogout={logout}
          />
        )}
      </aside>
    </>
  );
}
