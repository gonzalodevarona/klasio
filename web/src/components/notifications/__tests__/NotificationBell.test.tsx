import React from "react";
import { render, screen } from "@testing-library/react";
import NotificationBell from "../NotificationBell";

// ---------------------------------------------------------------------------
// Module mocks
// ---------------------------------------------------------------------------

jest.mock("@/hooks/useNotifications", () => ({
  useUnreadCount: jest.fn(),
  useNotifications: jest.fn(() => ({
    notifications: [],
    totalPages: 0,
    isLoading: false,
    error: null,
    refresh: jest.fn(),
  })),
  useMarkNotificationRead: jest.fn(() => ({ markRead: jest.fn() })),
  useMarkAllNotificationsRead: jest.fn(() => ({ markAllRead: jest.fn() })),
}));

import { useUnreadCount } from "@/hooks/useNotifications";

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe("NotificationBell", () => {
  it('renders "10+" badge when unread count is 11', () => {
    (useUnreadCount as jest.Mock).mockReturnValue({ count: 11 });

    render(<NotificationBell />);

    expect(screen.getByText("10+")).toBeInTheDocument();
  });
});
