import { getTranslations } from "next-intl/server";
import NotificationList from "@/components/notifications/NotificationList";

export default async function NotificationsPage() {
  const t = await getTranslations("notifications");

  return (
    <main className="p-6">
      <h1 className="text-2xl font-semibold mb-4">{t("pageTitle")}</h1>
      <NotificationList />
    </main>
  );
}
