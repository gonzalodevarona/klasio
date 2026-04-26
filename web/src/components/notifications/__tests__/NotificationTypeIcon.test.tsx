import { render } from "@testing-library/react";
import NotificationTypeIcon from "../NotificationTypeIcon";

describe("NotificationTypeIcon", () => {
  it("renders ALERTED with k-warn-text token", () => {
    const { container } = render(<NotificationTypeIcon type="CLASS_SESSION_ALERTED" />);
    const svg = container.querySelector("svg");
    expect(svg).not.toBeNull();
    expect(svg).toHaveClass("text-k-warn-text");
  });

  it("renders CANCELLED with k-danger-text token", () => {
    const { container } = render(<NotificationTypeIcon type="CLASS_SESSION_CANCELLED" />);
    expect(container.querySelector("svg")).toHaveClass("text-k-danger-text");
  });

  it("renders default (unknown type) with k-muted token", () => {
    const { container } = render(<NotificationTypeIcon type="SOME_OTHER_TYPE" />);
    expect(container.querySelector("svg")).toHaveClass("text-k-muted");
  });
});
