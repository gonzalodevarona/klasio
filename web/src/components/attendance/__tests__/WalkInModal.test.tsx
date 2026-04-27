import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { WalkInModal } from "../WalkInModal";
import * as eligibleHook from "@/hooks/useWalkInEligibleStudents";
import * as regHook from "@/hooks/useWalkInRegistration";
import { NextIntlClientProvider } from "next-intl";
import messages from "../../../../messages/en.json";

jest.mock("@/hooks/useWalkInEligibleStudents");
jest.mock("@/hooks/useWalkInRegistration");

function wrap(ui: React.ReactNode) {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      {ui}
    </NextIntlClientProvider>
  );
}

const defaultProps = {
  classId: "c1",
  sessionDate: "2026-04-27",
  startTime: "18:00:00",
  durationMinutes: 120,
  onClose: jest.fn(),
  onSuccess: jest.fn(),
};

describe("WalkInModal", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (eligibleHook.useWalkInEligibleStudents as jest.Mock).mockReturnValue({
      students: [], isLoading: false, error: null,
    });
    (regHook.useWalkInRegistration as jest.Mock).mockReturnValue({
      mutate: jest.fn(), isPending: false, error: null,
    });
  });

  it("renders the modal title", () => {
    wrap(<WalkInModal {...defaultProps} />);
    expect(screen.getByText("Register walk-in")).toBeInTheDocument();
  });

  it("renders eligible students list", async () => {
    (eligibleHook.useWalkInEligibleStudents as jest.Mock).mockReturnValue({
      students: [{ studentId: "s1", fullName: "Juan Perez", idDocument: "1004",
                   enrollmentId: "e1", membershipId: "m1", availableHours: 3 }],
      isLoading: false, error: null,
    });
    wrap(<WalkInModal {...defaultProps} />);
    expect(await screen.findByText("Juan Perez")).toBeInTheDocument();
  });

  it("submit button is disabled until a student is selected", () => {
    wrap(<WalkInModal {...defaultProps} />);
    const btn = screen.getByRole("button", { name: /Register and mark present/i });
    expect(btn).toBeDisabled();
  });

  it("hours dropdown has options 1..floor(duration/60) and defaults to max", () => {
    wrap(<WalkInModal {...defaultProps} durationMinutes={120} />);
    const select = screen.getByLabelText(/Hours to charge/i) as HTMLSelectElement;
    expect(select.value).toBe("2");
    expect([...select.options].map(o => o.value)).toEqual(["1", "2"]);
  });

  it("calls mutate with correct payload on submit and triggers onSuccess", async () => {
    const mutate = jest.fn().mockResolvedValue({
      registrationId: "r1", status: "PRESENT", intendedHours: 1,
    });
    (regHook.useWalkInRegistration as jest.Mock).mockReturnValue({
      mutate, isPending: false, error: null,
    });
    (eligibleHook.useWalkInEligibleStudents as jest.Mock).mockReturnValue({
      students: [{ studentId: "s1", fullName: "Juan Perez", idDocument: "1004",
                   enrollmentId: "e1", membershipId: "m1", availableHours: 3 }],
      isLoading: false, error: null,
    });
    const onSuccess = jest.fn();
    wrap(<WalkInModal {...defaultProps} durationMinutes={60} onSuccess={onSuccess} />);

    await userEvent.click(screen.getByText("Juan Perez"));
    await userEvent.click(screen.getByRole("button", { name: /Register and mark present/i }));

    await waitFor(() => expect(mutate).toHaveBeenCalledWith({
      startTime: "18:00:00", studentId: "s1", hoursToCharge: 1,
    }));
    expect(onSuccess).toHaveBeenCalled();
  });
});
