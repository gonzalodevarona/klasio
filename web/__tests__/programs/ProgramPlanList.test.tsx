import { render, screen } from "@testing-library/react";
import ProgramPlanList from "@/components/programs/ProgramPlanList";

jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn() }),
}));

const mockPlans = [
  {
    id: "plan-1",
    name: "4 Hours",
    modality: "HOURS_BASED" as const,
    cost: 90000,
    hours: 4,
    managerId: "11111111-1111-1111-1111-111111111111",
    status: "ACTIVE" as const,
  },
  {
    id: "plan-2",
    name: "Mon/Wed Evening",
    modality: "CLASSES_PER_WEEK" as const,
    cost: 120000,
    hours: null,
    managerId: "22222222-2222-2222-2222-222222222222",
    status: "INACTIVE" as const,
  },
];

describe("ProgramPlanList", () => {
  it("should render plan data in table", () => {
    render(
      <ProgramPlanList
        programId="prog-1"
        plans={mockPlans}
        loading={false}
        error={null}
      />
    );

    expect(screen.getByText("4 Hours")).toBeInTheDocument();
    expect(screen.getByText("Mon/Wed Evening")).toBeInTheDocument();
    expect(screen.getByText("4h")).toBeInTheDocument();
    expect(screen.getByText("Hours Based")).toBeInTheDocument();
    expect(screen.getByText("Classes per Week")).toBeInTheDocument();
    expect(screen.getByText("11111111-1111-1111-1111-111111111111")).toBeInTheDocument();
  });

  it("should show loading skeleton", () => {
    const { container } = render(
      <ProgramPlanList
        programId="prog-1"
        plans={[]}
        loading={true}
        error={null}
      />
    );

    expect(container.querySelector(".animate-pulse")).toBeInTheDocument();
  });

  it("should show error message", () => {
    render(
      <ProgramPlanList
        programId="prog-1"
        plans={[]}
        loading={false}
        error="Failed to load plans."
      />
    );

    expect(screen.getByText("Failed to load plans.")).toBeInTheDocument();
  });

  it("should show empty state with add button", () => {
    render(
      <ProgramPlanList
        programId="prog-1"
        plans={[]}
        loading={false}
        error={null}
      />
    );

    expect(screen.getByText(/no plans configured/i)).toBeInTheDocument();
    expect(screen.getByText("Add First Plan")).toBeInTheDocument();
  });

  it("should link plan names to detail pages", () => {
    render(
      <ProgramPlanList
        programId="prog-1"
        plans={mockPlans}
        loading={false}
        error={null}
      />
    );

    const link = screen.getByText("4 Hours").closest("a");
    expect(link).toHaveAttribute("href", "/programs/prog-1/plans/plan-1");
  });

  it("should show dash for null hours", () => {
    render(
      <ProgramPlanList
        programId="prog-1"
        plans={mockPlans}
        loading={false}
        error={null}
      />
    );

    const cells = screen.getAllByText("-");
    expect(cells.length).toBeGreaterThanOrEqual(1);
  });
});
