import { Suspense } from "react";
import Sidebar from "@/components/layout/Sidebar";

function SidebarSkeleton() {
  return (
    <aside className="w-64 bg-gray-900 min-h-screen flex flex-col">
      <div className="p-6 space-y-2">
        <div className="h-6 w-24 bg-gray-700 rounded animate-pulse" />
        <div className="h-4 w-32 bg-gray-700 rounded animate-pulse" />
      </div>
    </aside>
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
      <main className="flex-1 p-8">{children}</main>
    </div>
  );
}
