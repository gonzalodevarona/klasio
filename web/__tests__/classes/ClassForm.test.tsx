import { screen, fireEvent, waitFor } from "@testing-library/react";
import { renderWithIntl as render } from "../../__test-support__/renderWithIntl";
import ClassForm from "@/components/classes/ClassForm";
import { api, ApiError } from "@/lib/api";

const mockPush = jest.fn();

jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush }),
}));

jest.mock("@/lib/api", () => ({
  api: {
    post: jest.fn(),
    put: jest.fn(),
  },
  ApiError: class extends Error {
    status: number;
    code: string;
    details?: Array<{ field: string; message: string }>;
    constructor(
      status: number,
      code: string,
      message: string,
      details?: Array<{ field: string; message: string }>
    ) {
      super(message);
      this.status = status;
      this.code = code;
      this.details = details;
    }
  },
}));

const PROGRAM_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

beforeEach(() => {
  jest.clearAllMocks();
});

describe("ClassForm (create mode)", () => {
  it("renders all required fields", () => {
    render(<ClassForm programId={PROGRAM_ID} />);

    expect(screen.getByLabelText(/Class Name/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Level/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Max Students/)).toBeInTheDocument();
    expect(screen.getByText("Create Class")).toBeInTheDocument();
    expect(screen.getByText("Recurring")).toBeInTheDocument();
    expect(screen.getByText("One-Time")).toBeInTheDocument();
  });

  it("renders level dropdown with three options", () => {
    render(<ClassForm programId={PROGRAM_ID} />);

    const select = screen.getByLabelText(/Level/) as HTMLSelectElement;
    const options = Array.from(select.querySelectorAll("option"));
    const optionValues = options.map((o) => o.value);

    expect(optionValues).toContain("BEGINNER");
    expect(optionValues).toContain("INTERMEDIATE");
    expect(optionValues).toContain("ADVANCED");
  });

  it("renders recurring schedule fields by default (day of week selector)", () => {
    render(<ClassForm programId={PROGRAM_ID} />);

    expect(screen.getByText("Day of Week")).toBeInTheDocument();
    expect(screen.getByText("Start Time")).toBeInTheDocument();
    expect(screen.getByText("End Time")).toBeInTheDocument();
  });

  it("validates name is required on submit", async () => {
    render(<ClassForm programId={PROGRAM_ID} />);

    fireEvent.change(screen.getByLabelText(/Level/), {
      target: { value: "BEGINNER" },
    });
    fireEvent.change(screen.getByLabelText(/Max Students/), {
      target: { value: "20" },
    });
    fireEvent.click(screen.getByText("Create Class"));

    await waitFor(() => {
      expect(screen.getByText("Name is required.")).toBeInTheDocument();
    });

    expect(api.post).not.toHaveBeenCalled();
  });

  it("validates level is required on submit", async () => {
    render(<ClassForm programId={PROGRAM_ID} />);

    fireEvent.change(screen.getByLabelText(/Class Name/), {
      target: { value: "Test Class" },
    });
    fireEvent.change(screen.getByLabelText(/Max Students/), {
      target: { value: "20" },
    });
    fireEvent.click(screen.getByText("Create Class"));

    await waitFor(() => {
      expect(screen.getByText("Level is required.")).toBeInTheDocument();
    });

    expect(api.post).not.toHaveBeenCalled();
  });

  it("validates max students is required on submit", async () => {
    render(<ClassForm programId={PROGRAM_ID} />);

    fireEvent.change(screen.getByLabelText(/Class Name/), {
      target: { value: "Test Class" },
    });
    fireEvent.change(screen.getByLabelText(/Level/), {
      target: { value: "BEGINNER" },
    });
    fireEvent.click(screen.getByText("Create Class"));

    await waitFor(() => {
      expect(
        screen.getByText("Max students is required.")
      ).toBeInTheDocument();
    });

    expect(api.post).not.toHaveBeenCalled();
  });

  it("submits correct payload for recurring class and navigates on success", async () => {
    const createdClass = {
      id: "cccccccc-cccc-cccc-cccc-cccccccccccc",
    };
    (api.post as jest.Mock).mockResolvedValueOnce(createdClass);

    render(<ClassForm programId={PROGRAM_ID} />);

    fireEvent.change(screen.getByLabelText(/Class Name/), {
      target: { value: " Kids Beginner " },
    });
    fireEvent.change(screen.getByLabelText(/Level/), {
      target: { value: "BEGINNER" },
    });
    fireEvent.change(screen.getByLabelText(/Max Students/), {
      target: { value: "20" },
    });

    // Fill schedule entry
    const daySelect = screen.getByText("Select day").closest("select")!;
    fireEvent.change(daySelect, { target: { value: "MONDAY" } });

    const startTimeInputs = screen.getAllByDisplayValue("");
    // The time inputs - find by type
    const timeInputs = screen
      .getAllByRole("textbox", { hidden: true })
      .concat(
        Array.from(document.querySelectorAll('input[type="time"]')) as HTMLInputElement[]
      );

    const startInput = document.querySelector(
      'input[type="time"]'
    ) as HTMLInputElement;
    const endInput = document.querySelectorAll(
      'input[type="time"]'
    )[1] as HTMLInputElement;

    fireEvent.change(startInput, { target: { value: "18:00" } });
    fireEvent.change(endInput, { target: { value: "20:00" } });

    fireEvent.click(screen.getByText("Create Class"));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith(
        `/programs/${PROGRAM_ID}/classes`,
        expect.objectContaining({
          name: "Kids Beginner",
          level: "BEGINNER",
          type: "RECURRING",
          maxStudents: 20,
        })
      );
    });

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith(
        `/programs/${PROGRAM_ID}/classes/${createdClass.id}`
      );
    });
  });

  it("displays API error message on failure", async () => {
    const MockApiError = (api as unknown as { ApiError: typeof ApiError }).ApiError || ApiError;
    (api.post as jest.Mock).mockRejectedValueOnce(
      new (ApiError as unknown as new (...args: unknown[]) => Error)(
        409,
        "CLASS_NAME_ALREADY_EXISTS",
        "A class with this name already exists"
      )
    );

    render(<ClassForm programId={PROGRAM_ID} />);

    fireEvent.change(screen.getByLabelText(/Class Name/), {
      target: { value: "Kids Beginner" },
    });
    fireEvent.change(screen.getByLabelText(/Level/), {
      target: { value: "BEGINNER" },
    });
    fireEvent.change(screen.getByLabelText(/Max Students/), {
      target: { value: "20" },
    });

    const daySelect = screen.getByText("Select day").closest("select")!;
    fireEvent.change(daySelect, { target: { value: "MONDAY" } });

    const startInput = document.querySelector(
      'input[type="time"]'
    ) as HTMLInputElement;
    const endInput = document.querySelectorAll(
      'input[type="time"]'
    )[1] as HTMLInputElement;
    fireEvent.change(startInput, { target: { value: "18:00" } });
    fireEvent.change(endInput, { target: { value: "20:00" } });

    fireEvent.click(screen.getByText("Create Class"));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toBeInTheDocument();
    });
  });

  // T053: ONE_TIME tests
  it("switches to date picker when type=ONE_TIME", () => {
    render(<ClassForm programId={PROGRAM_ID} />);

    fireEvent.click(screen.getByLabelText("One-Time"));

    expect(screen.getByText("Date")).toBeInTheDocument();
    expect(screen.queryByText("Day of Week")).not.toBeInTheDocument();
  });

  it("renders level dropdown with OPEN option", () => {
    render(<ClassForm programId={PROGRAM_ID} />);

    const select = screen.getByLabelText(/Level/) as HTMLSelectElement;
    const optionValues = Array.from(select.querySelectorAll("option")).map((o) => o.value);

    expect(optionValues).toContain("OPEN");
    expect(select.querySelector('option[value="OPEN"]')?.textContent).toBe("Open (any level)");
  });

  it("validates schedule entry for one-time class requires date", async () => {
    render(<ClassForm programId={PROGRAM_ID} />);

    fireEvent.click(screen.getByLabelText("One-Time"));
    fireEvent.change(screen.getByLabelText(/Class Name/), {
      target: { value: "Workshop" },
    });
    fireEvent.change(screen.getByLabelText(/Level/), {
      target: { value: "ADVANCED" },
    });
    fireEvent.change(screen.getByLabelText(/Max Students/), {
      target: { value: "10" },
    });

    const startInput = document.querySelector(
      'input[type="time"]'
    ) as HTMLInputElement;
    const endInput = document.querySelectorAll(
      'input[type="time"]'
    )[1] as HTMLInputElement;
    fireEvent.change(startInput, { target: { value: "10:00" } });
    fireEvent.change(endInput, { target: { value: "12:00" } });

    fireEvent.click(screen.getByText("Create Class"));

    await waitFor(() => {
      expect(
        screen.getByText("One-time classes require a specific date.")
      ).toBeInTheDocument();
    });

    expect(api.post).not.toHaveBeenCalled();
  });
});

// Fixture for an existing OPEN class (edit mode)
const PROFESSOR_ID = "pppppppp-pppp-pppp-pppp-pppppppppppp";
const OPEN_CLASS: import("@/lib/types/programClass").ProgramClassDetail = {
  id: "cccccccc-cccc-cccc-cccc-cccccccccccc",
  programId: PROGRAM_ID,
  tenantId: "tttttttt-tttt-tttt-tttt-tttttttttttt",
  name: "Open Fitness",
  level: "OPEN",
  type: "RECURRING",
  maxStudents: 20,
  status: "ACTIVE",
  professorId: PROFESSOR_ID,
  scheduleEntries: [{ dayOfWeek: "MONDAY", startTime: "10:00", endTime: "11:00" }],
  createdAt: "2026-01-01T00:00:00Z",
  createdBy: "admin",
};

describe("ClassForm (edit mode – OPEN cascade confirmation)", () => {
  it("shows confirmation modal when changing OPEN → BEGINNER and clicking Save", async () => {
    render(<ClassForm programId={PROGRAM_ID} programClass={OPEN_CLASS} />);

    // Change level from OPEN to BEGINNER
    fireEvent.change(screen.getByLabelText(/Level/), { target: { value: "BEGINNER" } });

    // Click Save
    fireEvent.click(screen.getByText("Save Changes"));

    // Confirmation modal must appear
    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });

    // API must NOT have been called yet
    expect(api.put).not.toHaveBeenCalled();
  });

  it("proceeds with save after confirming the cascade modal", async () => {
    (api.put as jest.Mock).mockResolvedValue(OPEN_CLASS);

    render(<ClassForm programId={PROGRAM_ID} programClass={OPEN_CLASS} />);

    fireEvent.change(screen.getByLabelText(/Level/), { target: { value: "INTERMEDIATE" } });
    fireEvent.click(screen.getByText("Save Changes"));

    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });

    // Click Confirm in the modal
    fireEvent.click(screen.getByText("Confirm"));

    await waitFor(() => {
      expect(api.put).toHaveBeenCalledWith(
        `/programs/${PROGRAM_ID}/classes/${OPEN_CLASS.id}`,
        expect.objectContaining({ level: "INTERMEDIATE" })
      );
    });
  });

  it("cancels the save when dismissing the cascade modal", async () => {
    render(<ClassForm programId={PROGRAM_ID} programClass={OPEN_CLASS} />);

    fireEvent.change(screen.getByLabelText(/Level/), { target: { value: "ADVANCED" } });
    fireEvent.click(screen.getByText("Save Changes"));

    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });

    // Close the modal without confirming
    fireEvent.click(screen.getByText("Cancel"));

    expect(api.put).not.toHaveBeenCalled();
  });

  it("does NOT show confirmation modal when changing BEGINNER → OPEN (reverse direction)", async () => {
    const beginnerClass: import("@/lib/types/programClass").ProgramClassDetail = {
      ...OPEN_CLASS,
      level: "BEGINNER",
    };
    (api.put as jest.Mock).mockResolvedValue(beginnerClass);

    render(<ClassForm programId={PROGRAM_ID} programClass={beginnerClass} />);

    fireEvent.change(screen.getByLabelText(/Level/), { target: { value: "OPEN" } });
    fireEvent.click(screen.getByText("Save Changes"));

    // No dialog should appear
    await waitFor(() => {
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });
  });
});
