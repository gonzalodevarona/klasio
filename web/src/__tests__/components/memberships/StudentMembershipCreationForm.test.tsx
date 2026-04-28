import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import StudentMembershipCreationForm from "@/components/memberships/StudentMembershipCreationForm";

// --- Mock hooks that make network calls ---

jest.mock("@/hooks/useMyEnrollments", () => ({
  useMyEnrollments: jest.fn(),
}));

jest.mock("@/hooks/usePrograms", () => ({
  useProgramPlansByProgram: jest.fn(),
}));

import { useMyEnrollments } from "@/hooks/useMyEnrollments";
import { useProgramPlansByProgram } from "@/hooks/usePrograms";

const mockUseMyEnrollments = useMyEnrollments as jest.MockedFunction<typeof useMyEnrollments>;
const mockUseProgramPlansByProgram = useProgramPlansByProgram as jest.MockedFunction<
  typeof useProgramPlansByProgram
>;

// --- Test messages (only what the component needs) ---

const messages = {
  memberships: {
    formProgramLabel: "Program enrollment",
    formSelectProgram: "— Select a program —",
    formPlanLabel: "Plan",
    formPlanSelectPlaceholder: "— Select a plan —",
    formModalityLabel: "Modality: ",
    formModalityHoursBased: "Hours-based",
    formModalityClassesPerWeek: "Classes per week",
    formModalityUnlimited: "Unlimited",
    formHoursLabel: "Hours: ",
    formCostLabel: "Cost: ",
    newLoadingPlans: "Loading plans…",
    newNoPlans: "No active plans found for this program.",
    formCreateBtn: "Create Membership",
    formCreatingBtn: "Creating...",
    newNoEnrollments: "This student has no active program enrollments.",
  },
  membership: {
    unlimited: {
      label: "Unlimited hours",
      badge: "Unlimited",
      daysRemaining: "Expires in {days} days",
    },
  },
  paymentProofs: {
    panelTitle: "Payment Proof",
    fileHint: "PDF, JPG or PNG up to 5 MB",
    fileSizeError: "File is too large",
    fileTypeError: "Invalid file type",
  },
  common: {
    cancel: "Cancel",
    delete: "Delete",
    loading: "Loading…",
    unexpectedError: "An unexpected error occurred",
  },
};

// --- Helpers ---

const activeEnrollment = {
  programId: "prog-1",
  programName: "Soccer",
  level: "BEGINNER",
  status: "ACTIVE" as const,
  enrollmentId: "enroll-1",
  studentId: "stu-1",
};

const hoursBasedPlan = {
  id: "plan-hours-1",
  name: "Basic 10h",
  modality: "HOURS_BASED" as const,
  cost: 100000,
  hours: 10,
  managerId: "mgr-1",
  status: "ACTIVE" as const,
  programId: "prog-1",
};

const unlimitedPlan = {
  id: "plan-unlimited-1",
  name: "Unlimited Monthly",
  modality: "UNLIMITED" as const,
  cost: 200000,
  hours: null,
  managerId: "mgr-1",
  status: "ACTIVE" as const,
  programId: "prog-1",
};

function wrap(ui: React.ReactElement) {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      {ui}
    </NextIntlClientProvider>
  );
}

function setupDefaultMocks({
  plans = [hoursBasedPlan, unlimitedPlan],
}: { plans?: typeof hoursBasedPlan[] } = {}) {
  mockUseMyEnrollments.mockReturnValue({
    enrollments: [activeEnrollment],
    loading: false,
    error: null,
    refetch: jest.fn(),
  });
  mockUseProgramPlansByProgram.mockReturnValue({
    plans,
    loading: false,
    error: null,
    refetch: jest.fn(),
  });
}

// --- Tests ---

describe("StudentMembershipCreationForm", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("hides hours detail and shows 'Unlimited hours' when UNLIMITED plan is selected", async () => {
    setupDefaultMocks({ plans: [unlimitedPlan] });

    wrap(
      <StudentMembershipCreationForm
        initialProgramId="prog-1"
        onSubmit={jest.fn()}
        onCancel={jest.fn()}
      />
    );

    // The plan selector should be visible (program is pre-selected)
    const planSelect = await screen.findByRole("combobox");
    fireEvent.change(planSelect, { target: { value: "plan-unlimited-1" } });

    // "Unlimited hours" label should appear
    expect(await screen.findByText("Unlimited hours")).toBeInTheDocument();

    // Hours-specific label must NOT appear
    expect(screen.queryByText(/Hours:/)).not.toBeInTheDocument();
  });

  it("shows hours detail for HOURS_BASED plan", async () => {
    setupDefaultMocks({ plans: [hoursBasedPlan] });

    wrap(
      <StudentMembershipCreationForm
        initialProgramId="prog-1"
        onSubmit={jest.fn()}
        onCancel={jest.fn()}
      />
    );

    const planSelect = await screen.findByRole("combobox");
    fireEvent.change(planSelect, { target: { value: "plan-hours-1" } });

    // Hours: label should appear for HOURS_BASED
    expect(await screen.findByText(/Hours:/)).toBeInTheDocument();

    // "Unlimited hours" must NOT appear
    expect(screen.queryByText("Unlimited hours")).not.toBeInTheDocument();
  });

  it("shows 'Unlimited' modality label for UNLIMITED plan", async () => {
    setupDefaultMocks({ plans: [unlimitedPlan] });

    wrap(
      <StudentMembershipCreationForm
        initialProgramId="prog-1"
        onSubmit={jest.fn()}
        onCancel={jest.fn()}
      />
    );

    const planSelect = await screen.findByRole("combobox");
    fireEvent.change(planSelect, { target: { value: "plan-unlimited-1" } });

    // Modality label should show "Unlimited" text
    expect(await screen.findByText("Unlimited")).toBeInTheDocument();
  });

  it("does not filter out UNLIMITED plans from the selector", async () => {
    setupDefaultMocks({ plans: [hoursBasedPlan, unlimitedPlan] });

    wrap(
      <StudentMembershipCreationForm
        initialProgramId="prog-1"
        onSubmit={jest.fn()}
        onCancel={jest.fn()}
      />
    );

    // Both plan names should appear as options
    expect(await screen.findByText("Basic 10h")).toBeInTheDocument();
    expect(screen.getByText("Unlimited Monthly")).toBeInTheDocument();
  });

  it("renders no-enrollments message when student has no active enrollments", () => {
    mockUseMyEnrollments.mockReturnValue({
      enrollments: [],
      loading: false,
      error: null,
      refetch: jest.fn(),
    });
    mockUseProgramPlansByProgram.mockReturnValue({
      plans: [],
      loading: false,
      error: null,
      refetch: jest.fn(),
    });

    wrap(
      <StudentMembershipCreationForm onSubmit={jest.fn()} onCancel={jest.fn()} />
    );

    expect(screen.getByText(/no active program enrollments/i)).toBeInTheDocument();
  });
});
