import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import messages from "../../../../messages/en.json";
import StudentForm from "../StudentForm";

// Suppress next/navigation in test environment
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn() }),
}));

// Mock the api module — not needed for self mode but avoids import-time errors
jest.mock("@/lib/api", () => ({
  api: { post: jest.fn(), put: jest.fn() },
  ApiError: class ApiError extends Error {
    details: unknown[];
    constructor(message: string, details: unknown[] = []) {
      super(message);
      this.details = details;
    }
  },
}));

// DocumentFields calls useTranslations internally — mock it to a simple pair of inputs
jest.mock("@/components/common/DocumentFields", () => {
  const React = require("react");
  return function DocumentFieldsMock({
    documentType,
    documentNumber,
    onDocumentTypeChange,
    onDocumentNumberChange,
  }: {
    documentType: string;
    documentNumber: string;
    onDocumentTypeChange: (v: string) => void;
    onDocumentNumberChange: (v: string) => void;
  }) {
    return (
      <>
        <select
          aria-label="documentType"
          value={documentType}
          onChange={(e) => onDocumentTypeChange(e.target.value)}
        >
          <option value="CC">CC</option>
          <option value="TI">TI</option>
        </select>
        <input
          aria-label="identityNumber"
          value={documentNumber}
          onChange={(e) => onDocumentNumberChange(e.target.value)}
        />
      </>
    );
  };
});

function wrap(ui: React.ReactNode) {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      {ui}
    </NextIntlClientProvider>
  );
}

describe("StudentForm — self mode", () => {
  it("renders the canonical field set in self mode", () => {
    wrap(<StudentForm mode="self" onSubmit={jest.fn()} />);

    // firstName — use exact placeholder to avoid matching email placeholder
    expect(screen.getByPlaceholderText("e.g. Maria")).toBeInTheDocument();
    // phone
    expect(screen.getByPlaceholderText("e.g. 3001234567")).toBeInTheDocument();
    // blood type label (optional field)
    expect(screen.getByText(/blood type/i)).toBeInTheDocument();
  });

  it("reveals tutor block for a minor (age < 18)", async () => {
    wrap(<StudentForm mode="self" onSubmit={jest.fn()} />);

    // tutor section should NOT be visible initially
    expect(screen.queryByLabelText(/tutor first name/i)).toBeNull();

    // Enter a DOB for a 10-year-old via fireEvent.change (reliable for date inputs)
    const tenYearsAgo = new Date();
    tenYearsAgo.setFullYear(tenYearsAgo.getFullYear() - 10);
    const dobValue = tenYearsAgo.toISOString().split("T")[0];

    fireEvent.change(screen.getByLabelText(/date of birth/i), {
      target: { value: dobValue },
    });

    // Tutor first name field should now be visible
    await waitFor(() =>
      expect(screen.getByLabelText(/tutor first name/i)).toBeInTheDocument()
    );
  });

  it("calls onSubmit in self mode without router redirect", async () => {
    const onSubmit = jest.fn().mockResolvedValue(undefined);

    wrap(<StudentForm mode="self" onSubmit={onSubmit} />);

    // Fill required fields — use exact placeholder strings
    await userEvent.type(screen.getByPlaceholderText("e.g. Maria"), "Ana");
    await userEvent.type(screen.getByPlaceholderText("e.g. Rodriguez"), "García");
    await userEvent.type(screen.getByPlaceholderText("e.g. maria@example.com"), "ana@example.com");
    await userEvent.type(screen.getByPlaceholderText("e.g. 3001234567"), "3001234567");

    // dateOfBirth — use fireEvent.change for reliable date input handling
    fireEvent.change(screen.getByLabelText(/date of birth/i), {
      target: { value: "1995-06-15" },
    });

    await userEvent.type(screen.getByPlaceholderText("e.g. Sura, Nueva EPS"), "Sura");

    // identityNumber via mocked DocumentFields
    await userEvent.type(screen.getByLabelText("identityNumber"), "123456789");

    // Submit
    const submitBtn = screen.getByRole("button", { name: /create student/i });
    await userEvent.click(submitBtn);

    // onSubmit callback must have been called once
    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));

    // Success screen replaces the form — proves the self-mode path ran (no router redirect)
    await waitFor(() =>
      expect(screen.getByRole("status")).toBeInTheDocument()
    );
  });
});
