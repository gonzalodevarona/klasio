"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import type { Role } from "@/lib/types/auth";

interface NavItem {
  label: string;
  href: string;
}

const NAV_ITEMS_BY_ROLE: Record<Role, NavItem[]> = {
  SUPERADMIN: [
    { label: "Tenants",    href: "/tenants" },
    { label: "Programs",   href: "/programs" },
    { label: "Professors", href: "/professors" },
    { label: "Students",   href: "/students" },
    { label: "Plans",      href: "/plans" },
    { label: "Classes",    href: "/classes" },
  ],
  ADMIN: [
    { label: "Programs",   href: "/programs" },
    { label: "Professors", href: "/professors" },
    { label: "Students",   href: "/students" },
    { label: "Plans",      href: "/plans" },
    { label: "Classes",    href: "/classes" },
  ],
  MANAGER: [
    { label: "Programs",   href: "/programs" },
    { label: "Professors", href: "/professors" },
    { label: "Students",   href: "/students" },
    { label: "Classes",    href: "/classes" },
  ],
  PROFESSOR: [
    { label: "Classes", href: "/classes" },
  ],
  STUDENT: [],
};

export default function Sidebar() {
  const { user, loading, logout } = useAuth();
  const pathname = usePathname();

  if (loading) {
    return (
      <aside className="w-64 bg-gray-900 min-h-screen flex flex-col">
        <div className="p-6">
          <div className="h-6 w-24 bg-gray-700 rounded animate-pulse" />
          <div className="h-4 w-32 bg-gray-700 rounded animate-pulse mt-2" />
        </div>
      </aside>
    );
  }

  const navItems = user ? (NAV_ITEMS_BY_ROLE[user.role] ?? []) : [];

  return (
    <aside className="w-64 bg-gray-900 text-white min-h-screen flex flex-col">
      <div className="p-6">
        <h1 className="text-xl font-bold">Klasio</h1>
        <p className="text-sm text-gray-400 mt-1">League Management</p>
      </div>

      <nav className="flex-1 px-4">
        <ul className="space-y-1">
          {navItems.map(({ label, href }) => {
            const isActive = pathname === href || pathname.startsWith(href + "/");
            return (
              <li key={href}>
                <Link
                  href={href}
                  className={`flex items-center px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                    isActive
                      ? "bg-gray-700 text-white"
                      : "text-gray-300 hover:bg-gray-800 hover:text-white"
                  }`}
                >
                  {label}
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>

      {user && (
        <div className="p-4 border-t border-gray-700">
          <p className="text-xs text-gray-400 mb-2 truncate">{user.role}</p>
          <button
            onClick={logout}
            className="w-full text-left text-sm text-gray-300 hover:text-white px-3 py-2 rounded-md hover:bg-gray-800 transition-colors"
          >
            Sign out
          </button>
        </div>
      )}
    </aside>
  );
}
