import { screen, fireEvent, waitFor } from "@testing-library/react";
import { renderWithIntl as render } from "../../__test-support__/renderWithIntl";
import ProgramForm from "@/components/programs/ProgramForm";
import { api, ApiError } from "@/lib/api";
import { ProgramDetail } from "@/lib/types/program";

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

const existingProgram: ProgramDetail = {
  id: "11111111-1111-1111-1111-111111111111",
  tenantId: "22222222-2222-2222-2222-222222222222",
  name: "Kids Football",
  status: "ACTIVE",
  createdAt: "2025-01-15T10:00:00Z",
  createdBy: "44444444-4444-4444-4444-444444444444",
  updatedAt: "2025-01-15T10:00:00Z",
  updatedBy: "44444444-4444-4444-4444-444444444444",
};

beforeEach(() => {
  jest.clearAllMocks();
});

describe("ProgramForm (create mode)", () => {
  it("renders the name field", () => {
    render(<ProgramForm />);

    expect(screen.getByLabelText(/Name/)).toBeInTheDocument();
    expect(screen.getByText("Create Program")).toBeInTheDocument();
  });

  it("validates name is required on submit", async () => {
    render(<ProgramForm />);

    fireEvent.click(screen.getByText("Create Program"));

    await waitFor(() => {
      expect(screen.getByText("Name is required.")).toBeInTheDocument();
    });

    expect(api.post).not.toHaveBeenCalled();
  });

  it("validates name max length", async () => {
    render(<ProgramForm />);

    fireEvent.change(screen.getByLabelText(/Name/), {
      target: { value: "A".repeat(151) },
    });
    fireEvent.click(screen.getByText("Create Program"));

    await waitFor(() => {
      expect(
        screen.getByText("Name must be at most 150 characters.")
      ).toBeInTheDocument();
    });
  });

  it("submits create request and redirects on success", async () => {
    const created = { ...existingProgram, id: "new-id-1234" };
    (api.post as jest.Mock).mockResolvedValue(created);

    render(<ProgramForm />);

    fireEvent.change(screen.getByLabelText(/Name/), {
      target: { value: "New Program" },
    });
    fireEvent.click(screen.getByText("Create Program"));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith("/programs", {
        name: "New Program",
      });
    });

    expect(mockPush).toHaveBeenCalledWith("/programs/new-id-1234");
  });

  it("displays API error messages", async () => {
    const err = new (ApiError as unknown as new (...args: unknown[]) => ApiError)(
      409,
      "PROGRAM_NAME_ALREADY_EXISTS",
      "A program with this name already exists"
    );
    (api.post as jest.Mock).mockRejectedValue(err);

    render(<ProgramForm />);

    fireEvent.change(screen.getByLabelText(/Name/), {
      target: { value: "Duplicate" },
    });
    fireEvent.click(screen.getByText("Create Program"));

    await waitFor(() => {
      expect(
        screen.getByText("A program with this name already exists")
      ).toBeInTheDocument();
    });
  });
});

describe("ProgramForm (edit mode)", () => {
  it("pre-fills name with existing program data", () => {
    render(<ProgramForm program={existingProgram} />);

    expect(screen.getByLabelText(/Name/)).toHaveValue("Kids Football");
    expect(screen.getByText("Save Changes")).toBeInTheDocument();
  });

  it("submits update request via PUT", async () => {
    (api.put as jest.Mock).mockResolvedValue(existingProgram);

    render(<ProgramForm program={existingProgram} />);

    fireEvent.change(screen.getByLabelText(/Name/), {
      target: { value: "Updated Name" },
    });
    fireEvent.click(screen.getByText("Save Changes"));

    await waitFor(() => {
      expect(api.put).toHaveBeenCalledWith(
        `/programs/${existingProgram.id}`,
        {
          name: "Updated Name",
        }
      );
    });

    expect(mockPush).toHaveBeenCalledWith(
      `/programs/${existingProgram.id}`
    );
  });
});
