import { render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import StudentDashboard from "@/app/(dashboard)/student/dashboard/page";
import { MembershipSummary } from "@/lib/types/membership";

// --- Mock hooks that make network calls ---

jest.mock("@/hooks/useMemberships", () => ({
  useMyMemberships: jest.fn(),
}));

jest.mock("@/hooks/useMyEnrollments", () => ({
  useMyEnrollments: jest.fn(),
}));

jest.mock("@/hooks/useMyRegistrations", () => ({
  useMyRegistrations: jest.fn(),
}));

// Mock next/link — not relevant to the behavior under test
jest.mock("next/link", () => ({
  __esModule: true,
  default: ({ children, href }: { children: React.ReactNode; href: string }) => (
    <a href={href}>{children}</a>
  ),
}));

import { useMyMemberships } from "@/hooks/useMemberships";
import { useMyEnrollments } from "@/hooks/useMyEnrollments";
import { useMyRegistrations } from "@/hooks/useMyRegistrations";

const mockUseMyMemberships = useMyMemberships as jest.MockedFunction<typeof useMyMemberships>;
const mockUseMyEnrollments = useMyEnrollments as jest.MockedFunction<typeof useMyEnrollments>;
const mockUseMyRegistrations = useMyRegistrations as jest.MockedFunction<typeof useMyRegistrations>;

// --- i18n messages ---

const messages = {
  studentDashboard: {
    title: "My Dashboard",
    subtitle: "Overview of your activity",
    activeMembership: "Active Membership",
    loading: "Loading…",
    noMembership: "No active membership",
    membershipPeriod: "Period:",
    viewDetails: "View details",
    enrollmentsTitle: "My Enrollments",
    viewAll: "View all",
    noEnrollments: "No enrollments",
    upcomingRegistrations: "Upcoming Classes",
    noRegistrations: "No upcoming classes",
    quickLinksMemberships: "Memberships",
    quickLinksEnrollments: "Enrollments",
    quickLinksClasses: "Classes",
    alertTooltip: "Session alert",
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
};

// --- Helpers ---

function buildMembership(overrides: Partial<MembershipSummary> = {}): MembershipSummary {
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
    ...overrides,
  };
}

function setupDefaultMocks(memberships: MembershipSummary[]) {
  mockUseMyMemberships.mockReturnValue({
    memberships,
    loading: false,
    error: null,
    refetch: jest.fn(),
  });
  mockUseMyEnrollments.mockReturnValue({
    enrollments: [],
    loading: false,
    error: null,
    refetch: jest.fn(),
  });
  mockUseMyRegistrations.mockReturnValue({
    registrations: [],
    loading: false,
    error: null,
    refetch: jest.fn(),
  });
}

function wrap(ui: React.ReactElement) {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      {ui}
    </NextIntlClientProvider>
  );
}

// --- Tests ---

describe("StudentDashboard — UNLIMITED wiring", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders UnlimitedBadge when active membership is UNLIMITED", () => {
    const unlimitedMembership = buildMembership({
      modality: "UNLIMITED",
      purchasedHours: null,
      availableHours: null,
    });
    setupDefaultMocks([unlimitedMembership]);

    wrap(<StudentDashboard />);

    // "Unlimited" badge text must appear
    expect(screen.getByText("Unlimited")).toBeInTheDocument();

    // HourBalance must NOT be rendered
    expect(screen.queryByTestId("hour-balance")).not.toBeInTheDocument();
  });

  it("renders HourBalance when active membership is HOURS_BASED", () => {
    const hoursBasedMembership = buildMembership({
      modality: "HOURS_BASED",
      purchasedHours: 100,
      availableHours: 50,
    });
    setupDefaultMocks([hoursBasedMembership]);

    wrap(<StudentDashboard />);

    // HourBalance must be rendered with testid
    expect(screen.getByTestId("hour-balance")).toBeInTheDocument();

    // "Unlimited" badge must NOT appear
    expect(screen.queryByText("Unlimited")).not.toBeInTheDocument();
  });
});
