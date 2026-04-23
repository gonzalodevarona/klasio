import type { Metadata } from "next";
import "./globals.css";
import { NextIntlClientProvider } from "next-intl";
import { getLocale, getMessages } from "next-intl/server";
import { NotificationCountProvider } from "@/context/NotificationCountContext";

export const metadata: Metadata = {
  title: "Klasio - Sports League Management",
  description: "Multitenant platform for managing sports leagues",
};

export default async function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const locale = await getLocale();
  const messages = await getMessages();

  return (
    <html lang={locale}>
      <body className="min-h-screen bg-gray-50">
        <NextIntlClientProvider locale={locale} messages={messages}>
          <NotificationCountProvider>{children}</NotificationCountProvider>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
