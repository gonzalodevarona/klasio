import { Suspense } from "react";
import Sidebar from "@/components/layout/Sidebar";

function SidebarSkeleton() {
  return (
    <>
      {/* Mobile topbar skeleton */}
      <div className="lg:hidden fixed top-0 inset-x-0 z-40 h-14 bg-gray-900 border-b border-gray-700 animate-pulse" />
      {/* Desktop sidebar skeleton */}
      <aside className="hidden lg:flex w-64 bg-gray-900 h-screen sticky top-0 flex-col shrink-0">
        <div className="p-6 space-y-2">
          <div className="h-6 w-24 bg-gray-700 rounded animate-pulse" />
          <div className="h-4 w-32 bg-gray-700 rounded animate-pulse" />
        </div>
      </aside>
    </>
  );
}

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen bg-gray-50">
      <Suspense fallback={<SidebarSkeleton />}>
        <Sidebar />
      </Suspense>
      {/*
        pt-[4.5rem]: on mobile, clears the fixed topbar (3.5rem = 56px) + 1rem breathing room.
        lg:pt-8:     on desktop, the sidebar is sticky in flow so no topbar — use standard padding.
      */}
      <main className="flex-1 overflow-y-auto px-4 pb-4 pt-[4.5rem] lg:p-8">
        {children}
      </main>
    </div>
  );
}
