import React from "react";
import { screen } from "@testing-library/react";
import { renderWithIntl as render } from "../../../../__test-support__/renderWithIntl";
import NotificationList from "../NotificationList";

jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn() }),
}));

jest.mock("@/context/NotificationCountContext", () => ({
  useNotificationCount: () => ({ count: 0, refreshCount: jest.fn() }),
  NotificationCountProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

const useNotificationsMock = jest.fn();

jest.mock("@/hooks/useNotifications", () => ({
  useNotifications: (...args: unknown[]) => useNotificationsMock(...args),
  useMarkNotificationRead: () => ({ markRead: jest.fn() }),
  useMarkAllNotificationsRead: () => ({ markAllRead: jest.fn() }),
}));

function setNotifications(state: {
  notifications?: unknown[];
  totalPages?: number;
  isLoading?: boolean;
  error?: string | null;
}) {
  useNotificationsMock.mockReturnValue({
    notifications: state.notifications ?? [],
    totalPages: state.totalPages ?? 0,
    isLoading: state.isLoading ?? false,
    error: state.error ?? null,
    refresh: jest.fn(),
  });
}

describe("NotificationList", () => {
  beforeEach(() => useNotificationsMock.mockReset());

  it("renders the active 'All' tab on the elevated surface token", () => {
    setNotifications({});
    render(<NotificationList />);
    const allTab = screen.getByRole("button", { name: "All" });
    expect(allTab).toHaveClass("bg-k-surface", "shadow-k-card", "text-k-dark");
  });

  it("renders the empty state with the muted token", () => {
    setNotifications({});
    render(<NotificationList />);
    const empty = screen.getByText("No notifications found.");
    expect(empty).toHaveClass("text-k-muted");
  });

  it("renders the error state with the danger token", () => {
    setNotifications({ error: "Boom" });
    render(<NotificationList />);
    expect(screen.getByText("Boom")).toHaveClass("text-k-danger-text");
  });

  it("wraps non-empty notifications in a k-border list container", () => {
    setNotifications({
      notifications: [
        {
          id: "n-1",
          type: "CLASS_SESSION_ALERTED",
          title: "t",
          body: "b",
          read: false,
          readAt: null,
          createdAt: new Date().toISOString(),
          metadata: {},
        },
      ],
      totalPages: 1,
    });
    const { container } = render(<NotificationList />);
    const wrapper = container.querySelector("div.border-k-border.divide-k-line");
    expect(wrapper).not.toBeNull();
  });

  it("renders pagination buttons with the k-border token when multiple pages", () => {
    setNotifications({ notifications: [], totalPages: 3 });
    render(<NotificationList />);
    expect(screen.getByRole("button", { name: "Previous" })).toHaveClass("border-k-border");
    expect(screen.getByRole("button", { name: "Next" })).toHaveClass("border-k-border");
  });
});
