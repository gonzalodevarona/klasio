import type { Metadata } from "next";
import "./globals.css";
import { NotificationCountProvider } from "@/context/NotificationCountContext";

export const metadata: Metadata = {
  title: "Klasio - Sports League Management",
  description: "Multitenant platform for managing sports leagues",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className="min-h-screen bg-gray-50">
        <NotificationCountProvider>{children}</NotificationCountProvider>
      </body>
    </html>
  );
}
