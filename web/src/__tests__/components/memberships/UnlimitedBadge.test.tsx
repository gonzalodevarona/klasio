import { render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import { UnlimitedBadge } from "@/components/memberships/UnlimitedBadge";

const messages = {
  membership: {
    unlimited: {
      badge: "Unlimited",
      daysRemaining: "Expires in {days} days",
    },
  },
};

function wrap(ui: React.ReactElement) {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      {ui}
    </NextIntlClientProvider>
  );
}

describe("UnlimitedBadge", () => {
  it("renders unlimited badge with expiration date", () => {
    const expirationDate = new Date("2026-04-30");
    wrap(<UnlimitedBadge expiresAt={expirationDate} />);

    expect(screen.getByText("Unlimited")).toBeInTheDocument();
    expect(screen.getByText(/expires in/i)).toBeInTheDocument();
  });

  it("calculates days remaining correctly", () => {
    const today = new Date("2026-04-27");
    const expirationDate = new Date("2026-04-30"); // 3 days

    jest.useFakeTimers();
    jest.setSystemTime(today);

    wrap(<UnlimitedBadge expiresAt={expirationDate} />);
    expect(screen.getByText(/3 days/)).toBeInTheDocument();

    jest.useRealTimers();
  });
});
