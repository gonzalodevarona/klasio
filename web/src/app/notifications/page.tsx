import NotificationList from "@/components/notifications/NotificationList";

export default function NotificationsPage() {
  return (
    <main className="p-6">
      <h1 className="text-2xl font-semibold mb-4">Notifications</h1>
      <NotificationList />
    </main>
  );
}
