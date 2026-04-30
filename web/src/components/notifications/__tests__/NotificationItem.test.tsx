import { render, screen } from "@testing-library/react";
import NotificationItem from "../NotificationItem";

jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn() }),
}));

const baseNotification = {
  id: "n-1",
  type: "CLASS_SESSION_ALERTED",
  title: "Class alert",
  body: "Your class has an alert",
  read: false,
  readAt: null,
  createdAt: new Date().toISOString(),
  metadata: { classId: "c-1" },
} as const;

describe("NotificationItem", () => {
  it("renders the volt accent bar and tinted background when unread", () => {
    render(<NotificationItem notification={baseNotification} onRead={jest.fn()} />);
    const button = screen.getByRole("button");

    // outer button is positioned and tinted
    expect(button).toHaveClass("relative");
    expect(button).toHaveClass("bg-[#F9FFEA]");

    // accent bar is the first absolute span carrying the volt token
    const accent = button.querySelector("span.bg-k-volt");
    expect(accent).not.toBeNull();
    expect(accent).toHaveClass("absolute", "w-[3px]");
  });

  it("uses k-surface background and omits the accent bar when read", () => {
    render(
      <NotificationItem
        notification={{ ...baseNotification, read: true }}
        onRead={jest.fn()}
      />,
    );
    const button = screen.getByRole("button");

    expect(button).toHaveClass("bg-k-surface");
    expect(button.querySelector("span.bg-k-volt")).toBeNull();
  });

  it("renders the emoji inside an 8x8 k-bg wrapper", () => {
    const { container } = render(
      <NotificationItem notification={baseNotification} onRead={jest.fn()} />,
    );
    const wrapper = container.querySelector("div.bg-k-bg.w-8.h-8");
    expect(wrapper).not.toBeNull();
    expect(wrapper?.querySelector("[role='img']")).not.toBeNull();
  });

  it("uses the k-volt focus ring token on the outer button", () => {
    render(<NotificationItem notification={baseNotification} onRead={jest.fn()} />);
    expect(screen.getByRole("button")).toHaveClass("focus-visible:ring-k-volt");
  });
});
