import React from "react";
import { render, screen } from "@testing-library/react";
import ClassRosterPanel from "../ClassRosterPanel";
import * as rosterHook from "@/hooks/useClassSessionRoster";
import { NextIntlClientProvider } from "next-intl";
import messages from "../../../../messages/en.json";

jest.mock("@/hooks/useClassSessionRoster");
jest.mock("../WalkInButton", () => ({
  WalkInButton: () => <button>Register walk-in</button>,
}));
jest.mock("../DropInButton", () => ({
  DropInButton: (props: { programDropInPrice: string }) => (
    <button data-testid="drop-in-btn">Register drop-in ({props.programDropInPrice})</button>
  ),
}));
jest.mock("../AttendanceMarkingPanel", () => ({
  __esModule: true,
  default: () => <div data-testid="marking-panel" />,
}));
jest.mock("../SessionActionsPanel", () => ({
  __esModule: true,
  default: () => null,
}));
jest.mock("../SessionStatusBadge", () => ({
  __esModule: true,
  default: () => null,
}));
jest.mock("../RegistrationStatusBadge", () => ({
  __esModule: true,
  default: ({ status }: { status: string }) => <span data-testid="status-badge">{status}</span>,
  DropInTag: () => <span data-testid="dropin-tag">DROP-IN</span>,
}));
jest.mock("@/components/classes/ClassLevelBadge", () => ({
  __esModule: true,
  default: ({ level }: { level: string }) => <span data-testid="level-badge">{level}</span>,
}));

function wrap(ui: React.ReactNode) {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      {ui}
    </NextIntlClientProvider>
  );
}

const baseSession = {
  sessionDate: "2026-05-09",
  startTime: "18:00:00",
  endTime: "20:00:00",
  registrantCount: 2,
  status: "SCHEDULED" as const,
  alertReason: null,
  cancellationReason: null,
  registrants: [
    {
      registrationId: "reg-student",
      studentId: "s1",
      studentName: "Juan Perez",
      level: "BEGINNER",
      intendedHours: 2,
      status: "REGISTERED" as const,
      createdBy: null,
      dropInAttendeeId: null,
    },
    {
      registrationId: "reg-dropin",
      studentId: "",
      studentName: "",
      level: "",
      intendedHours: 0,
      status: "PRESENT" as const,
      createdBy: null,
      dropInAttendeeId: "att-1",
      dropInAttendeeName: "Ana García",
      dropInAttendeePhone: "3001234567",
      dropInPaymentAmount: "25000",
    },
  ],
};

describe("DropInRoster integration", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (rosterHook.useClassSessionRoster as jest.Mock).mockReturnValue({
      sessions: [baseSession],
      loading: false,
      error: null,
      refetch: jest.fn(),
    });
  });

  it("DropInButton is present when dropInPrice is set and session is not CANCELLED", () => {
    wrap(
      <ClassRosterPanel
        classId="c1"
        userRole="ADMIN"
        programId="p1"
        managedProgramIds={["p1"]}
        programDropInPrice="25000"
      />
    );
    expect(screen.getByTestId("drop-in-btn")).toBeInTheDocument();
  });

  it("DropInButton is absent when dropInPrice is null", () => {
    wrap(
      <ClassRosterPanel
        classId="c1"
        userRole="ADMIN"
        programId="p1"
        managedProgramIds={["p1"]}
        programDropInPrice={null}
      />
    );
    expect(screen.queryByTestId("drop-in-btn")).toBeNull();
  });

  it("DropInButton is absent when dropInPrice is not provided", () => {
    wrap(<ClassRosterPanel classId="c1" userRole="ADMIN" />);
    expect(screen.queryByTestId("drop-in-btn")).toBeNull();
  });

  it("drop-in row shows attendee name and DropInTag, no level or hours (read-only view)", () => {
    // Without userRole, ClassRosterPanel renders the read-only table
    wrap(
      <ClassRosterPanel
        classId="c1"
        programDropInPrice="25000"
      />
    );
    // Drop-in attendee name is shown
    expect(screen.getByText("Ana García")).toBeInTheDocument();
    // DropInTag is shown for the drop-in row
    expect(screen.getByTestId("dropin-tag")).toBeInTheDocument();
    // Student row shows level badge; drop-in row does not
    expect(screen.getByTestId("level-badge")).toBeInTheDocument();
    expect(screen.getByTestId("level-badge").textContent).toBe("BEGINNER");
  });

  it("student row is unchanged — shows name, level, status (read-only view)", () => {
    // Without userRole, ClassRosterPanel renders the read-only table
    wrap(<ClassRosterPanel classId="c1" programDropInPrice="25000" />);
    expect(screen.getByText("Juan Perez")).toBeInTheDocument();
    expect(screen.getByTestId("level-badge")).toBeInTheDocument();
  });

  it("DropInButton is absent when session is CANCELLED", () => {
    (rosterHook.useClassSessionRoster as jest.Mock).mockReturnValue({
      sessions: [{ ...baseSession, status: "CANCELLED" }],
      loading: false,
      error: null,
      refetch: jest.fn(),
    });
    wrap(
      <ClassRosterPanel
        classId="c1"
        userRole="ADMIN"
        programId="p1"
        managedProgramIds={["p1"]}
        programDropInPrice="25000"
      />
    );
    expect(screen.queryByTestId("drop-in-btn")).toBeNull();
  });
});
