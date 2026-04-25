import { getTranslations } from "next-intl/server";
import NotificationList from "@/components/notifications/NotificationList";

export default async function NotificationsPage() {
  const t = await getTranslations("notifications");

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
      </div>
      <NotificationList />
    </div>
  );
}
