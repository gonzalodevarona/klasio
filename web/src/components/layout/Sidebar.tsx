"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useTranslations } from "next-intl";
import { useAuth } from "@/hooks/useAuth";
import { useSidebarIdentity } from "@/hooks/useSidebarIdentity";
import { usePendingProofsCount } from "@/hooks/usePaymentProofs";
import NotificationBell from "@/components/notifications/NotificationBell";
import KLogo from "@/components/layout/KLogo";
import type { Role } from "@/lib/types/auth";
import { primaryRole } from "@/lib/types/auth";
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
  CalendarCheck,
  LogOut,
  Menu,
  X,
  ChevronLeft,
  ShieldCheck,
  UserCog,
  type LucideProps,
} from "lucide-react";

type IconComponent = React.ComponentType<LucideProps>;

interface NavItem {
  label: string;
  href: string;
  icon: IconComponent;
}

function NotificationBadge({ count, badgeMax }: { count: number; badgeMax: string }) {
  const label = count > 10 ? badgeMax : String(count);
  return (
    <span className="ml-auto flex items-center justify-center min-w-[1.25rem] h-5 px-1 rounded-full bg-k-danger-text text-white text-[10px] font-bold leading-none shrink-0">
      {label}
    </span>
  );
}

type LayoutT = ReturnType<typeof useTranslations<"layout">>;

function makeNavItemsByRole(t: LayoutT): Record<Role, NavItem[]> {
  return {
    SUPERADMIN: [
      { label: t("navTenants"),       href: "/tenants",        icon: Building2 },
      { label: t("navAdmins"),        href: "/admins",         icon: ShieldCheck },
      { label: t("navManagers"),      href: "/managers",       icon: UserCog },
      { label: t("navProfessors"),    href: "/professors",     icon: GraduationCap },
      { label: t("navStudents"),      href: "/students",       icon: Users },
      { label: t("navPrograms"),      href: "/programs",       icon: BookOpen },
      { label: t("navPlans"),         href: "/plans",          icon: ListChecks },
      { label: t("navClasses"),       href: "/classes",        icon: CalendarDays },
      { label: t("navPaymentProofs"), href: "/payment-proofs", icon: FileCheck },
    ],
    ADMIN: [
      { label: t("navManagers"),      href: "/managers",       icon: UserCog },
      { label: t("navProfessors"),    href: "/professors",     icon: GraduationCap },
      { label: t("navStudents"),      href: "/students",       icon: Users },
      { label: t("navPrograms"),      href: "/programs",       icon: BookOpen },
      { label: t("navPlans"),         href: "/plans",          icon: ListChecks },
      { label: t("navClasses"),       href: "/classes",        icon: CalendarDays },
      { label: t("navPaymentProofs"), href: "/payment-proofs", icon: FileCheck },
    ],
    MANAGER: [
      { label: t("navProfessors"), href: "/professors", icon: GraduationCap },
      { label: t("navStudents"),   href: "/students",   icon: Users },
      { label: t("navPrograms"),   href: "/programs",   icon: BookOpen },
      { label: t("navClasses"),    href: "/classes",    icon: CalendarDays },
    ],
    PROFESSOR: [
      { label: t("navClasses"), href: "/classes", icon: CalendarDays },
    ],
    STUDENT: [
      { label: t("navDashboard"),       href: "/student/dashboard",     icon: LayoutDashboard },
      { label: t("navMyMemberships"),   href: "/student/memberships",   icon: BadgeCheck },
      { label: t("navMyEnrollments"),   href: "/student/enrollments",   icon: ClipboardList },
      { label: t("navMyClasses"),       href: "/student/classes",       icon: Calendar },
      { label: t("navMyRegistrations"), href: "/student/registrations", icon: CalendarCheck },
    ],
  };
}

/** Union of nav items across all granted roles, deduplicated by href, ordered by role privilege. */
function computeNavItems(roles: Role[], navItemsByRole: Record<Role, NavItem[]>): NavItem[] {
  const seen = new Set<string>();
  const result: NavItem[] = [];
  for (const r of roles) {
    for (const item of (navItemsByRole[r] ?? [])) {
      if (!seen.has(item.href)) {
        seen.add(item.href);
        result.push(item);
      }
    }
  }
  return result;
}

function navItemClasses(active: boolean, collapsed: boolean) {
  return [
    "relative flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors",
    active
      ? "bg-k-sidebar-active text-white"
      : "text-k-muted hover:bg-k-sidebar-active hover:text-white",
    collapsed ? "justify-center" : "",
  ].join(" ");
}

// Module-level component so its identity is stable across renders.
function NavLinks({
  items,
  pathname,
  collapsed,
  onLinkClick,
  pendingProofsCount,
  badgeMax,
}: {
  items: NavItem[];
  pathname: string;
  collapsed: boolean;
  onLinkClick?: () => void;
  pendingProofsCount?: number | null;
  badgeMax: string;
}) {
  return (
    <ul className="space-y-1">
      {items.map(({ label, href, icon: Icon }) => {
        const isActive =
          pathname === href || pathname.startsWith(href + "/");
        const showBadge =
          href === "/payment-proofs" &&
          !collapsed &&
          pendingProofsCount != null &&
          pendingProofsCount > 0;
        return (
          <li key={href} className="relative">
            <Link
              href={href}
              onClick={onLinkClick}
              title={collapsed ? label : undefined}
              className={navItemClasses(isActive, collapsed)}
            >
              {isActive && (
                <span
                  aria-hidden="true"
                  className="absolute left-0 top-[20%] bottom-[20%] w-[3px] bg-k-volt rounded-r-full"
                />
              )}
              <div className="relative shrink-0">
                <Icon className={`h-5 w-5 ${isActive ? "text-k-volt" : "text-k-subtle"}`} />
                {/* Collapsed mode: dot indicator on the icon itself */}
                {collapsed && pendingProofsCount != null && pendingProofsCount > 0 && href === "/payment-proofs" && (
                  <span className="absolute -top-1 -right-1 flex h-2 w-2 rounded-full bg-k-danger-text" />
                )}
              </div>
              {!collapsed && <span className="truncate">{label}</span>}
              {showBadge && <NotificationBadge count={pendingProofsCount!} badgeMax={badgeMax} />}
            </Link>
          </li>
        );
      })}
    </ul>
  );
}

// Brand block shown in the sidebar header (desktop expanded only).
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
      <KLogo />
      {tenantName && (
        <p className="text-xs text-k-subtle whitespace-nowrap mt-0.5 truncate">
          {tenantName}
        </p>
      )}
    </div>
  );
}

// User identity block shown at the bottom of the sidebar.
// `forceExpanded` lets the mobile drawer reuse this component without honoring `collapsed`.
function UserFooter({
  role,
  displayName,
  identityDocumentType,
  identityNumber,
  collapsed,
  forceExpanded,
  onLogout,
  signOut,
}: {
  role: Role;
  displayName: string | null;
  identityDocumentType: string | null;
  identityNumber: string | null;
  collapsed: boolean;
  forceExpanded?: boolean;
  onLogout: () => void;
  signOut: string;
}) {
  const expanded = forceExpanded || !collapsed;
  return (
    <div className="px-2 py-4 border-t border-k-sidebar-active shrink-0">
      {expanded && (
        <div className="px-3 mb-2 space-y-0.5">
          {displayName && (
            <p className="text-xs font-medium text-white truncate">
              {displayName}
            </p>
          )}
          <p className="text-xs text-k-subtle truncate">{role}</p>
          {identityDocumentType && identityNumber && (
            <p className="text-xs text-k-subtle truncate">
              {identityDocumentType} {identityNumber}
            </p>
          )}
        </div>
      )}
      <button
        onClick={onLogout}
        title={!expanded ? signOut : undefined}
        className={[
          "flex items-center gap-3 w-full px-3 py-2 rounded-md text-sm text-k-subtle",
          "hover:text-white hover:bg-k-sidebar-active transition-colors",
          !expanded ? "justify-center" : "",
        ].join(" ")}
      >
        <LogOut className="h-5 w-5 shrink-0" />
        {expanded && <span>{signOut}</span>}
      </button>
    </div>
  );
}

export default function Sidebar() {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const { user, loading, logout } = useAuth();
  const pathname = usePathname();
  const t = useTranslations("layout");

  const primaryUserRole = user ? primaryRole(user.roles) : undefined;
  const { tenantName, displayName, identityDocumentType, identityNumber } =
    useSidebarIdentity(primaryUserRole, user?.tenantId);

  const canSeeProofQueue =
    user?.roles.includes("ADMIN") ||
    user?.roles.includes("SUPERADMIN") ||
    false;
  const pendingProofsCount = usePendingProofsCount(canSeeProofQueue);

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
        <div className="lg:hidden fixed top-0 inset-x-0 z-40 h-14 bg-k-dark border-b border-k-sidebar-active animate-pulse" />
        <aside className="hidden lg:flex w-[220px] bg-k-dark h-screen sticky top-0 flex-col shrink-0">
          <div className="p-6 space-y-2">
            <div className="h-6 w-24 bg-k-sidebar-active rounded animate-pulse" />
            <div className="h-4 w-32 bg-k-sidebar-active rounded animate-pulse" />
          </div>
        </aside>
      </>
    );
  }

  const navItemsByRole = makeNavItemsByRole(t);
  const navItems = user ? computeNavItems(user.roles, navItemsByRole) : [];

  return (
    <>
      {/* ── Mobile: top bar ─────────────────────────────────────── */}
      <header className="lg:hidden fixed top-0 inset-x-0 z-40 flex items-center h-14 px-4 gap-3 bg-k-dark border-b border-k-sidebar-active">
        <button
          onClick={() => setMobileOpen(true)}
          aria-label={t("openNav")}
          className="p-1 text-k-subtle hover:text-white rounded transition-colors"
        >
          <Menu className="h-6 w-6" />
        </button>
        <div className="min-w-0 flex-1">
          <KLogo />
          {tenantName && (
            <span className="ml-2 text-xs text-k-subtle truncate hidden sm:inline">
              {tenantName}
            </span>
          )}
        </div>
        <NotificationBell />
      </header>

      {/* ── Mobile: slide-in drawer ──────────────────────────────── */}
      {mobileOpen && (
        <div className="lg:hidden fixed inset-0 z-50 flex">
          <div
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            onClick={() => setMobileOpen(false)}
            aria-hidden="true"
          />

          <aside className="relative flex flex-col w-64 h-full bg-k-dark shadow-2xl">
            {/* Drawer header */}
            <div className="flex items-center justify-between px-4 py-3 shrink-0 border-b border-k-sidebar-active">
              <div className="min-w-0">
                <KLogo />
                {tenantName && (
                  <p className="text-xs text-k-subtle truncate mt-0.5">
                    {tenantName}
                  </p>
                )}
              </div>
              <button
                onClick={() => setMobileOpen(false)}
                aria-label={t("closeNav")}
                className="p-1 text-k-subtle hover:text-white rounded transition-colors shrink-0 ml-2"
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
                pendingProofsCount={pendingProofsCount}
                badgeMax={t("notificationsBadgeMax")}
              />
            </nav>

            {/* Drawer footer */}
            {user && (
              <UserFooter
                role={primaryUserRole!}
                displayName={displayName}
                identityDocumentType={identityDocumentType}
                identityNumber={identityNumber}
                collapsed={false}
                forceExpanded
                onLogout={logout}
                signOut={t("signOut")}
              />
            )}
          </aside>
        </div>
      )}

      {/* ── Desktop: collapsible sidebar ─────────────────────────── */}
      <aside
        className={[
          "hidden lg:flex flex-col bg-k-dark text-white h-screen sticky top-0 shrink-0",
          "transition-[width] duration-300 ease-in-out overflow-hidden",
          collapsed ? "w-16" : "w-[220px]",
        ].join(" ")}
      >
        {/* Sidebar header */}
        <div
          className={[
            "flex items-center shrink-0 border-b border-k-sidebar-active px-3 py-4",
            collapsed ? "justify-center" : "justify-between",
          ].join(" ")}
        >
          <Brand tenantName={tenantName} collapsed={collapsed} />
          <div className="flex items-center gap-1 shrink-0">
            {!collapsed && <NotificationBell />}
            <button
              onClick={() => setCollapsed((c) => !c)}
              aria-label={collapsed ? t("expandSidebar") : t("collapseSidebar")}
              className="p-1 text-k-subtle hover:text-white rounded transition-colors"
            >
              {collapsed ? (
                <Menu className="h-5 w-5" />
              ) : (
                <ChevronLeft className="h-5 w-5" />
              )}
            </button>
          </div>
        </div>

        {/* Sidebar nav */}
        <nav className="flex-1 px-2 py-4 overflow-y-auto overflow-x-hidden">
          <NavLinks
            items={navItems}
            pathname={pathname}
            collapsed={collapsed}
            pendingProofsCount={pendingProofsCount}
            badgeMax={t("notificationsBadgeMax")}
          />
        </nav>

        {/* Sidebar footer */}
        {user && (
          <UserFooter
            role={primaryUserRole!}
            displayName={displayName}
            identityDocumentType={identityDocumentType}
            identityNumber={identityNumber}
            collapsed={collapsed}
            onLogout={logout}
            signOut={t("signOut")}
          />
        )}
      </aside>
    </>
  );
}
