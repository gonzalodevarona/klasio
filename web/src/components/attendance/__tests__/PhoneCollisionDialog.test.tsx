import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { PhoneCollisionDialog } from "../PhoneCollisionDialog";

jest.mock("next-intl", () => ({
  useTranslations: () => (key: string, params?: Record<string, unknown>) => {
    if (params) return `${key}(${JSON.stringify(params)})`;
    return key;
  },
}));

const baseProps = {
  open: true,
  phone: "3001234567",
  fullName: "Ana García",
  totalVisits: 5,
  existingAttendeeId: "existing-uuid",
  onConfirm: jest.fn(),
  onCancel: jest.fn(),
};

describe("PhoneCollisionDialog", () => {
  beforeEach(() => jest.clearAllMocks());

  it("renders title, phone, fullName, and totalVisits when open=true", () => {
    render(<PhoneCollisionDialog {...baseProps} />);
    expect(screen.getByText("title")).toBeInTheDocument();
    // body and question are rendered
    expect(screen.getByText(/body/)).toBeInTheDocument();
    expect(screen.getByText("question")).toBeInTheDocument();
    // buttons
    expect(screen.getByText("yes")).toBeInTheDocument();
    expect(screen.getByText("cancel")).toBeInTheDocument();
  });

  it("calls onConfirm with existingAttendeeId when 'yes' button is clicked", async () => {
    const onConfirm = jest.fn();
    render(<PhoneCollisionDialog {...baseProps} onConfirm={onConfirm} />);
    await userEvent.click(screen.getByText("yes"));
    expect(onConfirm).toHaveBeenCalledWith("existing-uuid");
  });

  it("calls onCancel when 'cancel' button is clicked", async () => {
    const onCancel = jest.fn();
    render(<PhoneCollisionDialog {...baseProps} onCancel={onCancel} />);
    await userEvent.click(screen.getByText("cancel"));
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it("renders nothing when open=false", () => {
    const { container } = render(<PhoneCollisionDialog {...baseProps} open={false} />);
    expect(container.firstChild).toBeNull();
  });
});
