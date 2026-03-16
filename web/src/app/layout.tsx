import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Klasio - Sports League Management",
  description: "Multitenant platform for managing sports leagues",
};

function Sidebar() {
  return (
    <aside className="w-64 bg-gray-900 text-white min-h-screen flex flex-col">
      <div className="p-6">
        <h1 className="text-xl font-bold">Klasio</h1>
        <p className="text-sm text-gray-400 mt-1">League Management</p>
      </div>
      <nav className="flex-1 px-4">
        <ul className="space-y-1">
          <li>
            <a
              href="/tenants"
              className="flex items-center px-3 py-2 rounded-md text-sm font-medium text-gray-300 hover:bg-gray-800 hover:text-white"
            >
              Tenants
            </a>
          </li>
        </ul>
      </nav>
    </aside>
  );
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className="flex min-h-screen bg-gray-50">
        <Sidebar />
        <main className="flex-1 p-8">{children}</main>
      </body>
    </html>
  );
}
