import { render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import { HourTransactionSummary } from "@/lib/types/membership";
import HourTransactionList from "@/components/memberships/HourTransactionList";

// Mock the hook so we control the data without network calls
jest.mock("@/hooks/useHourTransactions", () => ({
  useHourTransactions: jest.fn(),
}));

import { useHourTransactions } from "@/hooks/useHourTransactions";

const mockUseHourTransactions = useHourTransactions as jest.MockedFunction<
  typeof useHourTransactions
>;

const messages = {
  memberships: {
    txLoading: "Loading transactions...",
    txEmpty: "No transactions recorded yet.",
    txColType: "Type",
    txColDelta: "Delta",
    txColReason: "Reason",
    txColActor: "Actor",
    txColDate: "Date",
  },
  badges: {
    hourTransactionType: {
      ATTENDANCE_DEDUCTION: "Attendance",
      MANUAL_ADDITION: "Manual +",
      MANUAL_SUBTRACTION: "Manual −",
    },
  },
};

function wrap(ui: React.ReactElement) {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      {ui}
    </NextIntlClientProvider>
  );
}

function buildTransaction(
  overrides: Partial<HourTransactionSummary> = {}
): HourTransactionSummary {
  return {
    id: "tx-1",
    membershipId: "mem-1",
    type: "ATTENDANCE_DEDUCTION",
    delta: -2,
    reason: null,
    actorId: "12345678-0000-0000-0000-000000000000",
    actorRole: "PROFESSOR",
    createdAt: "2026-04-27T10:00:00Z",
    ...overrides,
  };
}

describe("HourTransactionList", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const baseReturn = {
    totalPages: 1,
    totalElements: 0,
    refetch: jest.fn(),
  };

  it("shows loading state", () => {
    mockUseHourTransactions.mockReturnValue({
      ...baseReturn,
      transactions: [],
      loading: true,
      error: null,
    });

    wrap(<HourTransactionList membershipId="mem-1" />);

    expect(screen.getByText("Loading transactions...")).toBeInTheDocument();
  });

  it("shows error state", () => {
    mockUseHourTransactions.mockReturnValue({
      ...baseReturn,
      transactions: [],
      loading: false,
      error: "Failed to load",
    });

    wrap(<HourTransactionList membershipId="mem-1" />);

    expect(screen.getByText("Failed to load")).toBeInTheDocument();
  });

  it("shows empty state when no transactions", () => {
    mockUseHourTransactions.mockReturnValue({
      ...baseReturn,
      transactions: [],
      loading: false,
      error: null,
    });

    wrap(<HourTransactionList membershipId="mem-1" />);

    expect(
      screen.getByText("No transactions recorded yet.")
    ).toBeInTheDocument();
  });

  it("renders negative delta with minus sign", () => {
    mockUseHourTransactions.mockReturnValue({
      ...baseReturn,
      transactions: [buildTransaction({ delta: -2 })],
      loading: false,
      error: null,
    });

    wrap(<HourTransactionList membershipId="mem-1" />);

    expect(screen.getByText("-2")).toBeInTheDocument();
  });

  it("renders positive delta with plus sign", () => {
    mockUseHourTransactions.mockReturnValue({
      ...baseReturn,
      transactions: [buildTransaction({ type: "MANUAL_ADDITION", delta: 5 })],
      loading: false,
      error: null,
    });

    wrap(<HourTransactionList membershipId="mem-1" />);

    expect(screen.getByText("+5")).toBeInTheDocument();
  });

  it("renders delta=0 rows as dash instead of +0/-0", () => {
    mockUseHourTransactions.mockReturnValue({
      ...baseReturn,
      // reason is a non-null string so "—" can only come from the delta cell
      transactions: [buildTransaction({ delta: 0, reason: "attendance audit" })],
      loading: false,
      error: null,
    });

    wrap(<HourTransactionList membershipId="mem-1" />);

    // The delta cell should render "—", not "+0", "-0", or "0"
    const deltaCell = screen.getByTestId("tx-delta-tx-1");
    expect(deltaCell).toHaveTextContent("—");
    expect(screen.queryByText("+0")).not.toBeInTheDocument();
    expect(screen.queryByText("-0")).not.toBeInTheDocument();
    expect(screen.queryByText(/^0$/)).not.toBeInTheDocument();
  });
});
