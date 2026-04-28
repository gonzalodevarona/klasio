import { render, screen, fireEvent } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import MembershipForm from "@/components/memberships/MembershipForm";
import { ProgramPlanSummary } from "@/lib/types/program";

// --- i18n messages ---

const messages = {
  memberships: {
    formPlanLabel: "Plan",
    formPlanSelectPlaceholder: "— Select a plan —",
    formModalityLabel: "Modality: ",
    formModalityHoursBased: "Hours-based",
    formModalityClassesPerWeek: "Classes per week",
    formHoursLabel: "Hours: ",
    formCostLabel: "Cost: ",
    formStartDateLabel: "Start date",
    formExpiresHint: "Membership expires on the last day of the calendar month.",
    formPaymentValidated: "Payment validated",
    formActivateDirectly: "Activate directly",
    formCreateBtn: "Create Membership",
    formCreatingBtn: "Creating…",
  },
  membership: {
    unlimited: {
      label: "Unlimited hours",
      badge: "Unlimited",
      daysRemaining: "Expires in {days} days",
    },
    modality: {
      unlimited: "Unlimited",
    },
  },
  common: {
    cancel: "Cancel",
    unexpectedError: "An unexpected error occurred",
  },
};

// --- Plan fixtures ---

const hoursBasedPlan: ProgramPlanSummary = {
  id: "plan-hours-1",
  name: "Basic 10h",
  modality: "HOURS_BASED",
  cost: 100000,
  hours: 10,
  status: "ACTIVE",
  programId: "prog-1",
};

const unlimitedPlan: ProgramPlanSummary = {
  id: "plan-unlimited-1",
  name: "Unlimited Monthly",
  modality: "UNLIMITED",
  cost: 200000,
  hours: null,
  status: "ACTIVE",
  programId: "prog-1",
};

// --- Helpers ---

function wrap(ui: React.ReactElement) {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      {ui}
    </NextIntlClientProvider>
  );
}

// --- Tests ---

describe("MembershipForm — UNLIMITED wiring", () => {
  it("hides hours detail and shows 'Unlimited hours' label when UNLIMITED plan is selected", () => {
    wrap(
      <MembershipForm
        studentId="stu-1"
        plans={[unlimitedPlan]}
        onSubmit={jest.fn()}
        onCancel={jest.fn()}
      />
    );

    // Select the unlimited plan
    const planSelect = screen.getByRole("combobox");
    fireEvent.change(planSelect, { target: { value: "plan-unlimited-1" } });

    // "Unlimited hours" label must appear (from membership.unlimited.label)
    expect(screen.getByText("Unlimited hours")).toBeInTheDocument();

    // Hours-specific detail must NOT appear
    expect(screen.queryByText(/Hours:/)).not.toBeInTheDocument();
  });

  it("shows hours detail and hides 'Unlimited hours' when HOURS_BASED plan is selected", () => {
    wrap(
      <MembershipForm
        studentId="stu-1"
        plans={[hoursBasedPlan]}
        onSubmit={jest.fn()}
        onCancel={jest.fn()}
      />
    );

    // Select the hours-based plan
    const planSelect = screen.getByRole("combobox");
    fireEvent.change(planSelect, { target: { value: "plan-hours-1" } });

    // "Hours:" label must appear
    expect(screen.getByText(/Hours:/)).toBeInTheDocument();

    // "Unlimited hours" must NOT appear
    expect(screen.queryByText("Unlimited hours")).not.toBeInTheDocument();
  });
});
