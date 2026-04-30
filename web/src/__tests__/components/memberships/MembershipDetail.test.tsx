import { render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import { MembershipDetail as MembershipDetailType } from "@/lib/types/membership";
import MembershipDetail from "@/components/memberships/MembershipDetail";

// Mock hooks that make network calls
jest.mock("@/hooks/useMemberships", () => ({
  useMembershipActions: () => ({
    activateMembership: jest.fn(),
    validatePayment: jest.fn(),
    adjustHours: jest.fn(),
    loading: false,
    error: null,
  }),
}));

// Mock HourTransactionList — it makes its own API calls
jest.mock("@/components/memberships/HourTransactionList", () => ({
  __esModule: true,
  default: () => <div data-testid="hour-transaction-list" />,
}));

// Mock HourAdjustmentForm — modal, not relevant to these tests
jest.mock("@/components/memberships/HourAdjustmentForm", () => ({
  __esModule: true,
  default: () => <div data-testid="hour-adjustment-form" />,
}));

const messages = {
  memberships: {
    detailTitle: "Membership",
    detailSectionDetails: "Details",
    detailSectionTxHistory: "Hour Transaction History",
    detailLabelPlan: "Plan",
    detailLabelProgram: "Program",
    detailLabelStudent: "Student",
    detailLabelPurchasedHours: "Purchased hours",
    detailLabelAvailableHours: "Available hours",
    detailLabelStartDate: "Start date",
    detailLabelExpirationDate: "Expiration date",
    detailLabelPaymentValidated: "Payment validated",
    detailLabelActivatedAt: "Activated at",
    detailLabelCreatedAt: "Created at",
    detailLabelUpdatedAt: "Updated at",
    btnValidateActivate: "Validate & Activate",
    btnValidateDelegate: "Validate (delegate)",
    btnActivate: "Activate",
    btnAdjustHours: "Adjust Hours",
  },
  badges: {
    membershipStatus: {
      ACTIVE: "Active",
      INACTIVE: "Inactive",
      EXPIRED: "Expired",
      PENDING_PAYMENT: "Pending Payment",
      PENDING_PAYMENT_VALIDATION: "Under Review",
      PENDING_MANAGER_ACTIVATION: "Pending Activation",
    },
  },
  membership: {
    unlimited: {
      badge: "Unlimited",
      daysRemaining: "Expires in {days} days",
    },
  },
  common: {
    yes: "Yes",
    no: "No",
  },
};

function wrap(ui: React.ReactElement) {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      {ui}
    </NextIntlClientProvider>
  );
}

function buildMembership(overrides: Partial<MembershipDetailType> = {}): MembershipDetailType {
  return {
    id: "mem-1",
    studentId: "stu-1",
    programId: "prog-1",
    planId: "plan-1",
    planName: "Basic Plan",
    modality: "HOURS_BASED",
    purchasedHours: 100,
    availableHours: 50,
    startDate: "2026-04-01",
    expirationDate: "2026-04-30",
    status: "ACTIVE",
    paymentValidated: true,
    createdAt: "2026-04-01T00:00:00Z",
    tenantId: "tenant-1",
    studentName: "John Doe",
    programName: "Soccer",
    enrollmentId: "enroll-1",
    paymentValidatedBy: null,
    paymentValidatedAt: null,
    activatedBy: null,
    activatedAt: null,
    createdBy: "admin-1",
    updatedAt: null,
    updatedBy: null,
    ...overrides,
  };
}

describe("MembershipDetail", () => {
  it("renders UnlimitedBadge for UNLIMITED membership", () => {
    const membership = buildMembership({
      modality: "UNLIMITED",
      purchasedHours: null,
      availableHours: null,
    });

    wrap(<MembershipDetail membership={membership} onRefresh={jest.fn()} />);

    expect(screen.getByText("Unlimited")).toBeInTheDocument();
    expect(screen.queryByTestId("hour-balance")).not.toBeInTheDocument();
  });

  it("renders HourBalance for HOURS_BASED membership", () => {
    const membership = buildMembership({
      modality: "HOURS_BASED",
      purchasedHours: 100,
      availableHours: 50,
    });

    wrap(<MembershipDetail membership={membership} onRefresh={jest.fn()} />);

    expect(screen.getByTestId("hour-balance")).toBeInTheDocument();
    expect(screen.queryByText("Unlimited")).not.toBeInTheDocument();
  });
});
