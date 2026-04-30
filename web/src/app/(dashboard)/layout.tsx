import { Suspense } from "react";
import Sidebar from "@/components/layout/Sidebar";

function SidebarSkeleton() {
  return (
    <>
      {/* Mobile topbar skeleton */}
      <div className="lg:hidden fixed top-0 inset-x-0 z-40 h-14 bg-k-dark border-b border-k-sidebar-active animate-pulse" />
      {/* Desktop sidebar skeleton */}
      <aside className="hidden lg:flex w-[220px] bg-k-dark h-screen sticky top-0 flex-col shrink-0">
        <div className="p-6 space-y-2">
          <div className="h-6 w-24 bg-k-sidebar-active rounded animate-pulse" />
          <div className="h-4 w-32 bg-k-sidebar-active rounded animate-pulse" />
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
    <div className="flex min-h-screen bg-k-bg">
      <Suspense fallback={<SidebarSkeleton />}>
        <Sidebar />
      </Suspense>
      {/*
        pt-20: on mobile, clears the fixed topbar (h-14 = 56px) with 24px breathing room (matches p-6 elsewhere).
        lg:p-9: on desktop, the sidebar is sticky in flow so no topbar — uniform 36px padding.
      */}
      <main className="flex-1 overflow-y-auto pt-20 px-6 pb-6 lg:p-9">
        {children}
      </main>
    </div>
  );
}
