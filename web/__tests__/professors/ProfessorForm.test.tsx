import { screen, fireEvent, waitFor } from "@testing-library/react";
import { renderWithIntl } from "../../__test-support__/renderWithIntl";
import ProfessorForm from "@/components/professors/ProfessorForm";
import { api, ApiError } from "@/lib/api";
import { ProfessorDetail } from "@/lib/types/professor";

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

const existingProfessor: ProfessorDetail = {
  id: "11111111-1111-1111-1111-111111111111",
  tenantId: "22222222-2222-2222-2222-222222222222",
  firstName: "Carlos",
  lastName: "Martinez",
  email: "carlos@example.com",
  phoneNumber: "+573001234567",
  status: "ACTIVE" as const,
  createdAt: "2025-01-15T10:00:00Z",
  createdBy: "44444444-4444-4444-4444-444444444444",
  updatedAt: "2025-01-15T10:00:00Z",
  updatedBy: "44444444-4444-4444-4444-444444444444",
};

beforeEach(() => {
  jest.clearAllMocks();
});

describe("ProfessorForm (create mode)", () => {
  it("renders firstName, lastName, and email fields", () => {
    renderWithIntl(<ProfessorForm />);

    expect(screen.getByLabelText(/First Name/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Last Name/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Email/)).toBeInTheDocument();
    expect(screen.getByText("Create Professor")).toBeInTheDocument();
  });

  it("validates firstName is required on submit", async () => {
    renderWithIntl(<ProfessorForm />);

    fireEvent.change(screen.getByLabelText(/Last Name/), {
      target: { value: "Martinez" },
    });
    fireEvent.change(screen.getByLabelText(/Email/), {
      target: { value: "carlos@example.com" },
    });
    fireEvent.click(screen.getByText("Create Professor"));

    await waitFor(() => {
      expect(
        screen.getByText("First name is required.")
      ).toBeInTheDocument();
    });

    expect(api.post).not.toHaveBeenCalled();
  });

  it("validates lastName is required on submit", async () => {
    renderWithIntl(<ProfessorForm />);

    fireEvent.change(screen.getByLabelText(/First Name/), {
      target: { value: "Carlos" },
    });
    fireEvent.change(screen.getByLabelText(/Email/), {
      target: { value: "carlos@example.com" },
    });
    fireEvent.click(screen.getByText("Create Professor"));

    await waitFor(() => {
      expect(
        screen.getByText("Last name is required.")
      ).toBeInTheDocument();
    });

    expect(api.post).not.toHaveBeenCalled();
  });

  it("validates email is required on submit", async () => {
    renderWithIntl(<ProfessorForm />);

    fireEvent.change(screen.getByLabelText(/First Name/), {
      target: { value: "Carlos" },
    });
    fireEvent.change(screen.getByLabelText(/Last Name/), {
      target: { value: "Martinez" },
    });
    fireEvent.click(screen.getByText("Create Professor"));

    await waitFor(() => {
      expect(screen.getByText("Email is required.")).toBeInTheDocument();
    });

    expect(api.post).not.toHaveBeenCalled();
  });

  it("validates email format", async () => {
    renderWithIntl(<ProfessorForm />);

    fireEvent.change(screen.getByLabelText(/First Name/), {
      target: { value: "Carlos" },
    });
    fireEvent.change(screen.getByLabelText(/Last Name/), {
      target: { value: "Martinez" },
    });
    fireEvent.change(screen.getByLabelText(/Email/), {
      target: { value: "not-an-email" },
    });
    fireEvent.click(screen.getByText("Create Professor"));

    await waitFor(() => {
      expect(
        screen.getByText("Please enter a valid email address.")
      ).toBeInTheDocument();
    });

    expect(api.post).not.toHaveBeenCalled();
  });

  it("submits create request and redirects on success", async () => {
    const created = {
      ...existingProfessor,
      id: "new-id-1234",
    };
    (api.post as jest.Mock).mockResolvedValue(created);

    renderWithIntl(<ProfessorForm />);

    fireEvent.change(screen.getByLabelText(/First Name/), {
      target: { value: "Carlos" },
    });
    fireEvent.change(screen.getByLabelText(/Last Name/), {
      target: { value: "Martinez" },
    });
    fireEvent.change(screen.getByLabelText(/Email/), {
      target: { value: "carlos@example.com" },
    });
    fireEvent.click(screen.getByText("Create Professor"));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith("/professors", {
        firstName: "Carlos",
        lastName: "Martinez",
        email: "carlos@example.com",
        phoneNumber: undefined,
      });
    });

    expect(mockPush).toHaveBeenCalledWith("/professors/new-id-1234");
  });

  it("displays API error message on 409 conflict", async () => {
    const err = new (ApiError as unknown as new (...args: unknown[]) => ApiError)(
      409,
      "PROFESSOR_EMAIL_ALREADY_EXISTS",
      "A professor with this email already exists"
    );
    (api.post as jest.Mock).mockRejectedValue(err);

    renderWithIntl(<ProfessorForm />);

    fireEvent.change(screen.getByLabelText(/First Name/), {
      target: { value: "Carlos" },
    });
    fireEvent.change(screen.getByLabelText(/Last Name/), {
      target: { value: "Martinez" },
    });
    fireEvent.change(screen.getByLabelText(/Email/), {
      target: { value: "carlos@example.com" },
    });
    fireEvent.click(screen.getByText("Create Professor"));

    await waitFor(() => {
      expect(
        screen.getByText("A professor with this email already exists")
      ).toBeInTheDocument();
    });
  });

  it("maps API field-level error details to form fields", async () => {
    const err = new (ApiError as unknown as new (...args: unknown[]) => ApiError)(
      400,
      "VALIDATION_ERROR",
      "Validation failed",
      [
        { field: "email", message: "Email format is invalid" },
        { field: "firstName", message: "First name is too long" },
      ]
    );
    (api.post as jest.Mock).mockRejectedValue(err);

    renderWithIntl(<ProfessorForm />);

    fireEvent.change(screen.getByLabelText(/First Name/), {
      target: { value: "Carlos" },
    });
    fireEvent.change(screen.getByLabelText(/Last Name/), {
      target: { value: "Martinez" },
    });
    fireEvent.change(screen.getByLabelText(/Email/), {
      target: { value: "carlos@example.com" },
    });
    fireEvent.click(screen.getByText("Create Professor"));

    await waitFor(() => {
      expect(
        screen.getByText("Email format is invalid")
      ).toBeInTheDocument();
    });

    expect(
      screen.getByText("First name is too long")
    ).toBeInTheDocument();
  });
});

describe("ProfessorForm (edit mode)", () => {
  it("pre-fills fields with existing professor data", () => {
    renderWithIntl(<ProfessorForm professor={existingProfessor} />);

    expect(screen.getByLabelText(/First Name/)).toHaveValue("Carlos");
    expect(screen.getByLabelText(/Last Name/)).toHaveValue("Martinez");
    expect(screen.getByLabelText(/Email/)).toHaveValue("carlos@example.com");
    expect(screen.getByText("Save Changes")).toBeInTheDocument();
  });

  it("submits update request via PUT", async () => {
    (api.put as jest.Mock).mockResolvedValue(existingProfessor);

    renderWithIntl(<ProfessorForm professor={existingProfessor} />);

    fireEvent.change(screen.getByLabelText(/First Name/), {
      target: { value: "Carlos Updated" },
    });
    fireEvent.change(screen.getByLabelText(/Last Name/), {
      target: { value: "Martinez Updated" },
    });
    fireEvent.change(screen.getByLabelText(/Email/), {
      target: { value: "carlos.updated@example.com" },
    });
    fireEvent.click(screen.getByText("Save Changes"));

    await waitFor(() => {
      expect(api.put).toHaveBeenCalledWith(
        `/professors/${existingProfessor.id}`,
        {
          firstName: "Carlos Updated",
          lastName: "Martinez Updated",
          email: "carlos.updated@example.com",
          phoneNumber: "+573001234567",
        }
      );
    });

    expect(mockPush).toHaveBeenCalledWith(
      `/professors/${existingProfessor.id}`
    );
  });
});
