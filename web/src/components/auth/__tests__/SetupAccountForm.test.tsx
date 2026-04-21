import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import SetupAccountForm from "../SetupAccountForm";

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function buildProps(overrides: Partial<React.ComponentProps<typeof SetupAccountForm>> = {}) {
  return {
    token: "valid-token-123",
    ...overrides,
  };
}

afterEach(() => {
  jest.resetAllMocks();
});

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe("SetupAccountForm", () => {
  describe("when token is missing", () => {
    it("shows the resend email form directly", () => {
      render(<SetupAccountForm token={null} />);

      expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
      expect(screen.getByRole("button", { name: /send new link/i })).toBeInTheDocument();
    });

    it("does not render the password form", () => {
      render(<SetupAccountForm token={null} />);

      expect(screen.queryByLabelText(/new password/i)).not.toBeInTheDocument();
    });
  });

  describe("with a valid token", () => {
    it("renders the password form", () => {
      render(<SetupAccountForm {...buildProps()} />);

      expect(screen.getByLabelText("New Password")).toBeInTheDocument();
      expect(screen.getByLabelText("Confirm New Password")).toBeInTheDocument();
    });

    it("shows password policy checker below the password field", () => {
      render(<SetupAccountForm {...buildProps()} />);

      expect(screen.getByText(/at least 8 characters/i)).toBeInTheDocument();
    });

    it("shows error when passwords do not match", async () => {
      render(<SetupAccountForm {...buildProps()} />);

      fireEvent.change(screen.getByLabelText(/^new password/i), {
        target: { value: "Password1!" },
      });
      fireEvent.change(screen.getByLabelText(/confirm new password/i), {
        target: { value: "Different1!" },
      });

      fireEvent.click(screen.getByRole("button", { name: /set password/i }));

      expect(await screen.findByText(/passwords do not match/i)).toBeInTheDocument();
    });

    it("shows error when password does not meet policy requirements", async () => {
      render(<SetupAccountForm {...buildProps()} />);

      // Weak password: matches confirmPassword but fails policy
      fireEvent.change(screen.getByLabelText(/^new password/i), {
        target: { value: "weakpass" },
      });
      fireEvent.change(screen.getByLabelText(/confirm new password/i), {
        target: { value: "weakpass" },
      });

      fireEvent.click(screen.getByRole("button", { name: /set password/i }));

      expect(await screen.findByText(/password does not meet the requirements/i)).toBeInTheDocument();
    });

    it("does not call the API when passwords do not match", async () => {
      global.fetch = jest.fn();

      render(<SetupAccountForm {...buildProps()} />);

      fireEvent.change(screen.getByLabelText(/^new password/i), {
        target: { value: "Password1!" },
      });
      fireEvent.change(screen.getByLabelText(/confirm new password/i), {
        target: { value: "Different1!" },
      });

      fireEvent.click(screen.getByRole("button", { name: /set password/i }));

      await screen.findByText(/passwords do not match/i);
      expect(global.fetch).not.toHaveBeenCalled();
    });

    it("does not call the API when password fails policy", async () => {
      global.fetch = jest.fn();

      render(<SetupAccountForm {...buildProps()} />);

      fireEvent.change(screen.getByLabelText(/^new password/i), {
        target: { value: "weakpass" },
      });
      fireEvent.change(screen.getByLabelText(/confirm new password/i), {
        target: { value: "weakpass" },
      });

      fireEvent.click(screen.getByRole("button", { name: /set password/i }));

      await screen.findByText(/password does not meet the requirements/i);
      expect(global.fetch).not.toHaveBeenCalled();
    });
  });

  describe("successful submission (200)", () => {
    it("shows success message and login link", async () => {
      global.fetch = jest.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => ({}),
      });

      render(<SetupAccountForm {...buildProps()} />);

      fireEvent.change(screen.getByLabelText(/^new password/i), {
        target: { value: "SecurePass1!" },
      });
      fireEvent.change(screen.getByLabelText(/confirm new password/i), {
        target: { value: "SecurePass1!" },
      });
      fireEvent.click(screen.getByRole("button", { name: /set password/i }));

      await waitFor(() => {
        expect(
          screen.getByText(/your account is ready/i)
        ).toBeInTheDocument();
      });

      expect(screen.getByRole("link", { name: /log in/i })).toBeInTheDocument();
    });
  });

  describe("expired / invalid token (410)", () => {
    it("shows expired link message with 'request a new link' button", async () => {
      global.fetch = jest.fn().mockResolvedValue({
        ok: false,
        status: 410,
        json: async () => ({}),
      });

      render(<SetupAccountForm {...buildProps()} />);

      fireEvent.change(screen.getByLabelText(/^new password/i), {
        target: { value: "SecurePass1!" },
      });
      fireEvent.change(screen.getByLabelText(/confirm new password/i), {
        target: { value: "SecurePass1!" },
      });
      fireEvent.click(screen.getByRole("button", { name: /set password/i }));

      await waitFor(() => {
        expect(
          screen.getByText(/setup link has expired/i)
        ).toBeInTheDocument();
      });

      expect(
        screen.getByRole("button", { name: /request a new link/i })
      ).toBeInTheDocument();
    });

    it("reveals resend email form when 'request a new link' is clicked", async () => {
      global.fetch = jest.fn().mockResolvedValue({
        ok: false,
        status: 410,
        json: async () => ({}),
      });

      render(<SetupAccountForm {...buildProps()} />);

      fireEvent.change(screen.getByLabelText(/^new password/i), {
        target: { value: "SecurePass1!" },
      });
      fireEvent.change(screen.getByLabelText(/confirm new password/i), {
        target: { value: "SecurePass1!" },
      });
      fireEvent.click(screen.getByRole("button", { name: /set password/i }));

      await waitFor(() => {
        screen.getByRole("button", { name: /request a new link/i });
      });

      fireEvent.click(screen.getByRole("button", { name: /request a new link/i }));

      expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: /send new link/i })
      ).toBeInTheDocument();
    });

    it("shows confirmation after resend is submitted", async () => {
      // First call: setup-account 410; second call: resend-setup 200
      global.fetch = jest
        .fn()
        .mockResolvedValueOnce({
          ok: false,
          status: 410,
          json: async () => ({}),
        })
        .mockResolvedValueOnce({
          ok: true,
          status: 200,
          json: async () => ({}),
        });

      render(<SetupAccountForm {...buildProps()} />);

      // Submit the setup form to get the 410
      fireEvent.change(screen.getByLabelText(/^new password/i), {
        target: { value: "SecurePass1!" },
      });
      fireEvent.change(screen.getByLabelText(/confirm new password/i), {
        target: { value: "SecurePass1!" },
      });
      fireEvent.click(screen.getByRole("button", { name: /set password/i }));

      await waitFor(() => {
        screen.getByRole("button", { name: /request a new link/i });
      });

      // Open the resend form
      fireEvent.click(screen.getByRole("button", { name: /request a new link/i }));

      // Fill in email and submit
      fireEvent.change(screen.getByLabelText(/email address/i), {
        target: { value: "user@example.com" },
      });
      fireEvent.click(screen.getByRole("button", { name: /send new link/i }));

      await waitFor(() => {
        expect(
          screen.getByText(/a new setup link has been sent/i)
        ).toBeInTheDocument();
      });
    });
  });

  describe("unexpected errors", () => {
    it("shows generic error message on 400", async () => {
      global.fetch = jest.fn().mockResolvedValue({
        ok: false,
        status: 400,
        json: async () => ({}),
      });

      render(<SetupAccountForm {...buildProps()} />);

      fireEvent.change(screen.getByLabelText(/^new password/i), {
        target: { value: "SecurePass1!" },
      });
      fireEvent.change(screen.getByLabelText(/confirm new password/i), {
        target: { value: "SecurePass1!" },
      });
      fireEvent.click(screen.getByRole("button", { name: /set password/i }));

      await waitFor(() => {
        expect(
          screen.getByText(/something went wrong/i)
        ).toBeInTheDocument();
      });
    });

    it("shows generic error message on network failure", async () => {
      global.fetch = jest.fn().mockRejectedValue(new Error("Network error"));

      render(<SetupAccountForm {...buildProps()} />);

      fireEvent.change(screen.getByLabelText(/^new password/i), {
        target: { value: "SecurePass1!" },
      });
      fireEvent.change(screen.getByLabelText(/confirm new password/i), {
        target: { value: "SecurePass1!" },
      });
      fireEvent.click(screen.getByRole("button", { name: /set password/i }));

      await waitFor(() => {
        expect(
          screen.getByText(/something went wrong/i)
        ).toBeInTheDocument();
      });
    });
  });
});
