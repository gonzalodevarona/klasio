import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ProgramPlanForm from "@/components/programs/ProgramPlanForm";

const mockPush = jest.fn();
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush }),
}));

global.fetch = jest.fn();

beforeEach(() => {
  jest.clearAllMocks();
  localStorage.setItem("auth_token", "test-token");
});

describe("ProgramPlanForm", () => {
  describe("HOURS_BASED modality (default)", () => {
    it("should render hours input for HOURS_BASED", () => {
      render(<ProgramPlanForm programId="prog-1" />);

      expect(screen.getByLabelText(/plan name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/cost/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/hours/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/manager id/i)).toBeInTheDocument();
      expect(screen.queryByText(/schedule entries/i)).not.toBeInTheDocument();
    });

    it("should show modality select in create mode", () => {
      render(<ProgramPlanForm programId="prog-1" />);

      const modalitySelect = screen.getByLabelText(/modality/i);
      expect(modalitySelect).toBeInTheDocument();
      expect(modalitySelect).not.toBeDisabled();
    });

    it("should submit HOURS_BASED plan", async () => {
      const user = userEvent.setup();
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        json: async () => ({ id: "new-plan" }),
      });

      render(<ProgramPlanForm programId="prog-1" />);

      await user.type(screen.getByLabelText(/plan name/i), "4 Hours");
      await user.clear(screen.getByLabelText(/cost/i));
      await user.type(screen.getByLabelText(/cost/i), "90000");
      await user.clear(screen.getByLabelText(/hours/i));
      await user.type(screen.getByLabelText(/hours/i), "4");
      await user.type(
        screen.getByLabelText(/manager id/i),
        "33333333-3333-3333-3333-333333333333"
      );

      await user.click(screen.getByText("Create Plan"));

      await waitFor(() => {
        expect(global.fetch).toHaveBeenCalledWith(
          expect.stringContaining("/programs/prog-1/plans"),
          expect.objectContaining({ method: "POST" })
        );
      });
    });

    it("should validate positive hours", async () => {
      render(<ProgramPlanForm programId="prog-1" />);

      fireEvent.change(screen.getByLabelText(/plan name/i), {
        target: { value: "Test" },
      });
      fireEvent.change(screen.getByLabelText(/cost/i), {
        target: { value: "100" },
      });
      fireEvent.change(screen.getByLabelText(/hours/i), {
        target: { value: "0" },
      });
      fireEvent.change(screen.getByLabelText(/manager id/i), {
        target: { value: "33333333-3333-3333-3333-333333333333" },
      });
      fireEvent.submit(screen.getByText("Create Plan").closest("form")!);

      await waitFor(() => {
        expect(screen.getByRole("alert")).toHaveTextContent(
          /hours must be a positive integer/i
        );
      });
    });

    it("should validate manager ID is required", async () => {
      render(<ProgramPlanForm programId="prog-1" />);

      fireEvent.change(screen.getByLabelText(/plan name/i), {
        target: { value: "Test" },
      });
      fireEvent.change(screen.getByLabelText(/cost/i), {
        target: { value: "100" },
      });
      fireEvent.change(screen.getByLabelText(/hours/i), {
        target: { value: "4" },
      });
      fireEvent.submit(screen.getByText("Create Plan").closest("form")!);

      await waitFor(() => {
        expect(screen.getByRole("alert")).toHaveTextContent(
          /manager id is required/i
        );
      });
    });

    it("should validate manager ID is a valid UUID", async () => {
      render(<ProgramPlanForm programId="prog-1" />);

      fireEvent.change(screen.getByLabelText(/plan name/i), {
        target: { value: "Test" },
      });
      fireEvent.change(screen.getByLabelText(/cost/i), {
        target: { value: "100" },
      });
      fireEvent.change(screen.getByLabelText(/hours/i), {
        target: { value: "4" },
      });
      fireEvent.change(screen.getByLabelText(/manager id/i), {
        target: { value: "not-a-uuid" },
      });
      fireEvent.submit(screen.getByText("Create Plan").closest("form")!);

      await waitFor(() => {
        expect(screen.getByRole("alert")).toHaveTextContent(
          /manager id must be a valid uuid/i
        );
      });
    });
  });

  describe("CLASSES_PER_WEEK modality", () => {
    it("should render schedule builder when modality is changed to CLASSES_PER_WEEK", async () => {
      const user = userEvent.setup();
      render(<ProgramPlanForm programId="prog-1" />);

      await user.selectOptions(screen.getByLabelText(/modality/i), "CLASSES_PER_WEEK");

      expect(screen.getByText("Schedule Entries")).toBeInTheDocument();
      expect(screen.getByText(/\+ add entry/i)).toBeInTheDocument();
      expect(screen.queryByLabelText(/hours/i)).not.toBeInTheDocument();
    });

    it("should add and remove schedule entries", async () => {
      const user = userEvent.setup();
      render(<ProgramPlanForm programId="prog-1" />);

      await user.selectOptions(screen.getByLabelText(/modality/i), "CLASSES_PER_WEEK");

      await user.click(screen.getByText(/\+ add entry/i));
      expect(screen.getAllByLabelText(/day of week/i)).toHaveLength(1);

      await user.click(screen.getByText(/\+ add entry/i));
      expect(screen.getAllByLabelText(/day of week/i)).toHaveLength(2);

      await user.click(screen.getAllByText("Remove")[0]);
      expect(screen.getAllByLabelText(/day of week/i)).toHaveLength(1);
    });

    it("should validate at least one schedule entry", async () => {
      const user = userEvent.setup();
      render(<ProgramPlanForm programId="prog-1" />);

      await user.selectOptions(screen.getByLabelText(/modality/i), "CLASSES_PER_WEEK");

      await user.type(screen.getByLabelText(/plan name/i), "Test Plan");
      await user.clear(screen.getByLabelText(/cost/i));
      await user.type(screen.getByLabelText(/cost/i), "120000");
      await user.type(
        screen.getByLabelText(/manager id/i),
        "33333333-3333-3333-3333-333333333333"
      );

      await user.click(screen.getByText("Create Plan"));

      await waitFor(() => {
        expect(screen.getByRole("alert")).toHaveTextContent(
          /at least one schedule entry/i
        );
      });
    });
  });

  describe("Edit mode", () => {
    const existingPlan = {
      id: "plan-1",
      programId: "prog-1",
      tenantId: "tenant-1",
      name: "4 Hours",
      modality: "HOURS_BASED" as const,
      cost: 90000,
      hours: 4,
      managerId: "33333333-3333-3333-3333-333333333333",
      scheduleEntries: [],
      status: "ACTIVE" as const,
      createdAt: "2024-01-01T00:00:00Z",
      createdBy: "user-1",
      updatedAt: null,
      updatedBy: null,
    };

    it("should prefill form in edit mode", () => {
      render(
        <ProgramPlanForm
          programId="prog-1"
          plan={existingPlan}
        />
      );

      expect(screen.getByLabelText(/plan name/i)).toHaveValue("4 Hours");
      expect(screen.getByLabelText(/cost/i)).toHaveValue(90000);
      expect(screen.getByLabelText(/hours/i)).toHaveValue(4);
      expect(screen.getByLabelText(/manager id/i)).toHaveValue(
        "33333333-3333-3333-3333-333333333333"
      );
      expect(screen.getByText("Update Plan")).toBeInTheDocument();
    });

    it("should disable modality in edit mode", () => {
      render(
        <ProgramPlanForm
          programId="prog-1"
          plan={existingPlan}
        />
      );

      const modalityInput = screen.getByLabelText(/modality/i);
      expect(modalityInput).toBeDisabled();
      expect(
        screen.getByText("Modality cannot be changed after creation.")
      ).toBeInTheDocument();
    });

    it("should submit via PUT in edit mode", async () => {
      const user = userEvent.setup();
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        json: async () => ({ id: "plan-1" }),
      });

      render(
        <ProgramPlanForm
          programId="prog-1"
          plan={existingPlan}
        />
      );

      await user.click(screen.getByText("Update Plan"));

      await waitFor(() => {
        expect(global.fetch).toHaveBeenCalledWith(
          expect.stringContaining("/programs/prog-1/plans/plan-1"),
          expect.objectContaining({ method: "PUT" })
        );
      });
    });
  });

  it("should navigate back on cancel", async () => {
    const user = userEvent.setup();
    render(<ProgramPlanForm programId="prog-1" />);

    await user.click(screen.getByText("Cancel"));

    expect(mockPush).toHaveBeenCalledWith("/programs/prog-1");
  });
});
