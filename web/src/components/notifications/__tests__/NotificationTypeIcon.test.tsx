import { render, screen } from "@testing-library/react";
import NotificationTypeIcon from "../NotificationTypeIcon";

describe("NotificationTypeIcon", () => {
  it("renders ALERTED with warning emoji", () => {
    render(<NotificationTypeIcon type="CLASS_SESSION_ALERTED" />);
    const icon = screen.getByRole("img", { name: "CLASS_SESSION_ALERTED" });
    expect(icon).toBeInTheDocument();
    expect(icon).toHaveTextContent("⚠️");
  });

  it("renders CANCELLED with prohibited emoji", () => {
    render(<NotificationTypeIcon type="CLASS_SESSION_CANCELLED" />);
    const icon = screen.getByRole("img", { name: "CLASS_SESSION_CANCELLED" });
    expect(icon).toBeInTheDocument();
    expect(icon).toHaveTextContent("🚫");
  });

  it("renders default (unknown type) with bell emoji", () => {
    render(<NotificationTypeIcon type="SOME_OTHER_TYPE" />);
    const icon = screen.getByRole("img", { name: "SOME_OTHER_TYPE" });
    expect(icon).toBeInTheDocument();
    expect(icon).toHaveTextContent("🔔");
  });
});
