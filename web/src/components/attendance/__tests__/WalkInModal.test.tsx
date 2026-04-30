import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { WalkInModal } from "../WalkInModal";
import * as eligibleHook from "@/hooks/useWalkInEligibleStudents";
import * as bulkHook from "@/hooks/useWalkInBulkRegistration";
import { NextIntlClientProvider } from "next-intl";
import messages from "../../../../messages/en.json";

jest.mock("@/hooks/useWalkInEligibleStudents");
jest.mock("@/hooks/useWalkInBulkRegistration");

function wrap(ui: React.ReactNode) {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      {ui}
    </NextIntlClientProvider>
  );
}

const baseStudents = [
  { studentId: "s1", fullName: "Juan Perez",   idDocument: "1004", enrollmentId: "e1", membershipId: "m1", availableHours: 3, level: "BEGINNER" },
  { studentId: "s2", fullName: "Ana Gomez",    idDocument: "2005", enrollmentId: "e2", membershipId: "m2", availableHours: -1, level: "BEGINNER" },
  { studentId: "s3", fullName: "Carlos Ruiz",  idDocument: "3006", enrollmentId: "e3", membershipId: "m3", availableHours: 5, level: "ADVANCED" },
];

const defaultProps = {
  classId: "c1",
  sessionDate: "2026-04-27",
  startTime: "18:00:00",
  durationMinutes: 120,
  classLevel: "OPEN" as string,
  onClose: jest.fn(),
  onSuccess: jest.fn(),
};

describe("WalkInModal", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (eligibleHook.useWalkInEligibleStudents as jest.Mock).mockReturnValue({
      students: baseStudents, isLoading: false, error: null,
    });
    (bulkHook.useWalkInBulkRegistration as jest.Mock).mockReturnValue({
      mutate: jest.fn(), isPending: false, error: null,
    });
  });

  it("renders modal title", () => {
    wrap(<WalkInModal {...defaultProps} />);
    expect(screen.getByText("Register walk-in")).toBeInTheDocument();
  });

  it("search bar is always visible", () => {
    wrap(<WalkInModal {...defaultProps} />);
    expect(screen.getByPlaceholderText(/search by name/i)).toBeInTheDocument();
  });

  it("level dropdown is shown only for OPEN classes", () => {
    wrap(<WalkInModal {...defaultProps} classLevel="OPEN" />);
    expect(screen.getByLabelText(/level/i)).toBeInTheDocument();
  });

  it("level dropdown is hidden for non-OPEN classes", () => {
    wrap(<WalkInModal {...defaultProps} classLevel="BEGINNER" />);
    expect(screen.queryByLabelText(/level/i)).toBeNull();
  });

  it("filters students by search query in memory", async () => {
    wrap(<WalkInModal {...defaultProps} />);
    await userEvent.type(screen.getByPlaceholderText(/search by name/i), "Juan");
    expect(screen.getByText("Juan Perez")).toBeInTheDocument();
    expect(screen.queryByText("Ana Gomez")).toBeNull();
    expect(screen.queryByText("Carlos Ruiz")).toBeNull();
  });

  it("toggles select-all only over visible filtered rows", async () => {
    wrap(<WalkInModal {...defaultProps} />);
    await userEvent.type(screen.getByPlaceholderText(/search by name/i), "Perez");
    await userEvent.click(screen.getByLabelText(/select all/i));
    // only Juan Perez selected (the only filtered row)
    expect(screen.getByText(/1 selected/i)).toBeInTheDocument();
  });

  it("submit button uses bulk endpoint with selected ids", async () => {
    const mutate = jest.fn().mockResolvedValue({
      results: [{ studentId: "s1", outcome: "SUCCESS", registrationId: "r1", status: "PRESENT", intendedHours: 1 }],
      summary: { total: 1, succeeded: 1, failed: 0 },
    });
    (bulkHook.useWalkInBulkRegistration as jest.Mock).mockReturnValue({ mutate, isPending: false, error: null });
    wrap(<WalkInModal {...defaultProps} durationMinutes={60} />);

    await userEvent.click(screen.getByText("Juan Perez"));
    await userEvent.click(screen.getByRole("button", { name: /register .*walk-in/i }));

    await waitFor(() => expect(mutate).toHaveBeenCalledWith({
      startTime: "18:00:00", studentIds: ["s1"], hoursToCharge: 1,
    }));
  });

  it("renders results panel with success and failure counts after submit", async () => {
    const mutate = jest.fn().mockResolvedValue({
      results: [
        { studentId: "s1", outcome: "SUCCESS", registrationId: "r1", status: "PRESENT", intendedHours: 1 },
        { studentId: "s2", outcome: "FAILED", errorCode: "INSUFFICIENT_HOURS", errorMessage: "no hours" },
      ],
      summary: { total: 2, succeeded: 1, failed: 1 },
    });
    (bulkHook.useWalkInBulkRegistration as jest.Mock).mockReturnValue({ mutate, isPending: false, error: null });
    wrap(<WalkInModal {...defaultProps} durationMinutes={60} />);

    await userEvent.click(screen.getByText("Juan Perez"));
    await userEvent.click(screen.getByText("Ana Gomez"));
    await userEvent.click(screen.getByRole("button", { name: /register .*walk-in/i }));

    await waitFor(() => {
      expect(screen.getByText(/1 registered successfully/i)).toBeInTheDocument();
      expect(screen.getByText(/1 failed/i)).toBeInTheDocument();
    });
  });

  it("retry-failed pre-checks failed students and returns to list view", async () => {
    const mutate = jest.fn().mockResolvedValue({
      results: [
        { studentId: "s1", outcome: "SUCCESS", registrationId: "r1", status: "PRESENT", intendedHours: 1 },
        { studentId: "s2", outcome: "FAILED", errorCode: "INSUFFICIENT_HOURS", errorMessage: "no hours" },
      ],
      summary: { total: 2, succeeded: 1, failed: 1 },
    });
    (bulkHook.useWalkInBulkRegistration as jest.Mock).mockReturnValue({ mutate, isPending: false, error: null });
    wrap(<WalkInModal {...defaultProps} durationMinutes={60} />);

    await userEvent.click(screen.getByText("Juan Perez"));
    await userEvent.click(screen.getByText("Ana Gomez"));
    await userEvent.click(screen.getByRole("button", { name: /register .*walk-in/i }));

    await waitFor(() => screen.getByText(/1 failed/i));
    await userEvent.click(screen.getByRole("button", { name: /retry failed/i }));

    // back to list view, only Ana Gomez selected (the previously-failed one)
    expect(screen.getByText(/1 selected/i)).toBeInTheDocument();
  });
});
