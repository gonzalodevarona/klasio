import React from "react";
import { screen } from "@testing-library/react";
import { renderWithIntl as render } from "../../../../__test-support__/renderWithIntl";
import NotificationBell from "../NotificationBell";

// ---------------------------------------------------------------------------
// Module mocks
// ---------------------------------------------------------------------------

jest.mock("@/context/NotificationCountContext", () => ({
  useNotificationCount: jest.fn(),
  NotificationCountProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

jest.mock("@/hooks/useNotifications", () => ({
  useNotifications: jest.fn(() => ({
    notifications: [],
    totalPages: 0,
    isLoading: false,
    error: null,
    refresh: jest.fn(),
    markReadOptimistic: jest.fn(),
  })),
  useMarkNotificationRead: jest.fn(() => ({ markRead: jest.fn() })),
  useMarkAllNotificationsRead: jest.fn(() => ({ markAllRead: jest.fn() })),
}));

import { useNotificationCount } from "@/context/NotificationCountContext";

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe("NotificationBell", () => {
  it('renders "10+" badge when unread count is 11', () => {
    (useNotificationCount as jest.Mock).mockReturnValue({ count: 11, refreshCount: jest.fn() });

    render(<NotificationBell />);

    expect(screen.getByText("10+")).toBeInTheDocument();
  });
});
