import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import SessionReasonModal from "../SessionReasonModal";

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const noop = jest.fn();

function buildProps(overrides: Partial<React.ComponentProps<typeof SessionReasonModal>> = {}) {
  return {
    open: true,
    title: "Raise Alert",
    submitLabel: "Submit",
    submitVariant: "amber" as const,
    onClose: noop,
    onSubmit: jest.fn().mockResolvedValue(undefined),
    submitting: false,
    error: null,
    ...overrides,
  };
}

afterEach(() => {
  jest.clearAllMocks();
});

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe("SessionReasonModal", () => {
  it("disables submit when reason is shorter than 20 characters", () => {
    render(<SessionReasonModal {...buildProps()} />);

    const textarea = screen.getByRole("textbox");
    fireEvent.change(textarea, { target: { value: "too short" } });

    const submitButton = screen.getByRole("button", { name: "Submit" });
    expect(submitButton).toBeDisabled();
  });

  it("enables submit at 20 characters", () => {
    render(<SessionReasonModal {...buildProps()} />);

    const textarea = screen.getByRole("textbox");
    // Exactly 20 characters
    fireEvent.change(textarea, { target: { value: "12345678901234567890" } });

    const submitButton = screen.getByRole("button", { name: "Submit" });
    expect(submitButton).not.toBeDisabled();
  });

  it("shows live character count", () => {
    render(<SessionReasonModal {...buildProps()} />);

    const textarea = screen.getByRole("textbox");
    fireEvent.change(textarea, { target: { value: "twelve chars" } });

    expect(screen.getByText(/12 \/ 20 minimum/)).toBeInTheDocument();
  });

  it("calls onSubmit with the trimmed reason", async () => {
    const onSubmit = jest.fn().mockResolvedValue(undefined);
    render(<SessionReasonModal {...buildProps({ onSubmit })} />);

    const textarea = screen.getByRole("textbox");
    // 20 chars with surrounding whitespace that should be trimmed
    fireEvent.change(textarea, { target: { value: "  12345678901234567890  " } });

    const submitButton = screen.getByRole("button", { name: "Submit" });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith("12345678901234567890");
    });
  });
});
