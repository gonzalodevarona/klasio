import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import TenantForm from "@/components/tenants/TenantForm";

// Mock next/navigation
const pushMock = jest.fn();
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: pushMock }),
}));

// Mock API
const postFormMock = jest.fn();
jest.mock("@/lib/api", () => {
  const { ApiError } = jest.requireActual("@/lib/api");
  return {
    api: {
      postForm: (...args: unknown[]) => postFormMock(...args),
    },
    ApiError,
  };
});

// Import ApiError after mock setup
import { ApiError } from "@/lib/api";

beforeEach(() => {
  jest.clearAllMocks();
});

describe("TenantForm", () => {
  it("renders all form fields", () => {
    render(<TenantForm />);

    expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/sport discipline/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/slug/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/contact email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/contact phone/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/contact address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/logo/i)).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /create league/i })
    ).toBeInTheDocument();
  });

  it("shows validation errors for empty required fields", async () => {
    const user = userEvent.setup();
    render(<TenantForm />);

    await user.click(screen.getByRole("button", { name: /create league/i }));

    expect(screen.getByText("Name is required.")).toBeInTheDocument();
    expect(
      screen.getByText("Sport discipline is required.")
    ).toBeInTheDocument();
    expect(
      screen.getByText("Contact email is required.")
    ).toBeInTheDocument();

    expect(postFormMock).not.toHaveBeenCalled();
  });

  it("shows validation error for invalid email", async () => {
    const user = userEvent.setup();
    render(<TenantForm />);

    await user.type(screen.getByLabelText(/name/i), "Test League");
    await user.type(screen.getByLabelText(/sport discipline/i), "Soccer");
    await user.type(screen.getByLabelText(/contact email/i), "not-an-email");

    await user.click(screen.getByRole("button", { name: /create league/i }));

    expect(
      screen.getByText("Enter a valid email address.")
    ).toBeInTheDocument();

    expect(postFormMock).not.toHaveBeenCalled();
  });

  it("submits form data correctly", async () => {
    const user = userEvent.setup();
    postFormMock.mockResolvedValue({
      id: "1",
      slug: "test-league",
      name: "Test League",
      sportDiscipline: "Soccer",
      contactEmail: "test@league.com",
      status: "ACTIVE",
      createdAt: "2026-03-15T00:00:00Z",
      logoUrl: null,
      contactPhone: null,
      contactAddress: null,
      createdBy: "admin",
      deactivatedAt: null,
      deactivatedBy: null,
    });

    render(<TenantForm />);

    await user.type(screen.getByLabelText(/name/i), "Test League");
    await user.type(screen.getByLabelText(/sport discipline/i), "Soccer");
    await user.type(
      screen.getByLabelText(/contact email/i),
      "test@league.com"
    );
    await user.type(screen.getByLabelText(/contact phone/i), "+573001234567");
    await user.type(
      screen.getByLabelText(/contact address/i),
      "Calle 50, Medellin"
    );

    await user.click(screen.getByRole("button", { name: /create league/i }));

    await waitFor(() => {
      expect(postFormMock).toHaveBeenCalledTimes(1);
    });

    const [path, formData] = postFormMock.mock.calls[0];
    expect(path).toBe("/tenants");
    expect(formData).toBeInstanceOf(FormData);
    expect(formData.get("name")).toBe("Test League");
    expect(formData.get("sportDiscipline")).toBe("Soccer");
    expect(formData.get("contactEmail")).toBe("test@league.com");
    expect(formData.get("contactPhone")).toBe("+573001234567");
    expect(formData.get("contactAddress")).toBe("Calle 50, Medellin");

    await waitFor(() => {
      expect(pushMock).toHaveBeenCalledWith("/tenants/test-league");
    });
  });

  it("shows API error messages", async () => {
    const user = userEvent.setup();
    postFormMock.mockRejectedValue(
      new ApiError(409, "SLUG_ALREADY_EXISTS", "A league with this slug already exists.", [
        { field: "slug", message: "Slug is already taken." },
      ])
    );

    render(<TenantForm />);

    await user.type(screen.getByLabelText(/name/i), "Test League");
    await user.type(screen.getByLabelText(/sport discipline/i), "Soccer");
    await user.type(
      screen.getByLabelText(/contact email/i),
      "test@league.com"
    );

    await user.click(screen.getByRole("button", { name: /create league/i }));

    await waitFor(() => {
      expect(
        screen.getByText("A league with this slug already exists.")
      ).toBeInTheDocument();
    });
  });

  it("shows slug preview as user types name", async () => {
    const user = userEvent.setup();
    render(<TenantForm />);

    await user.type(screen.getByLabelText(/name/i), "Liga de Futbol");

    const preview = screen.getByTestId("slug-preview");
    expect(preview).toHaveTextContent("liga-de-futbol");
  });

  it("shows slug preview with accent stripping", async () => {
    const user = userEvent.setup();
    render(<TenantForm />);

    await user.type(screen.getByLabelText(/name/i), "Liga Antioquena");

    const preview = screen.getByTestId("slug-preview");
    expect(preview).toHaveTextContent("liga-antioquena");
  });

  it("disables submit button while submitting", async () => {
    const user = userEvent.setup();

    let resolveSubmit: (value: unknown) => void;
    postFormMock.mockReturnValue(
      new Promise((resolve) => {
        resolveSubmit = resolve;
      })
    );

    render(<TenantForm />);

    await user.type(screen.getByLabelText(/name/i), "Test League");
    await user.type(screen.getByLabelText(/sport discipline/i), "Soccer");
    await user.type(
      screen.getByLabelText(/contact email/i),
      "test@league.com"
    );

    await user.click(screen.getByRole("button", { name: /create league/i }));

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /creating/i })).toBeDisabled();
    });

    // Resolve the promise to clean up
    resolveSubmit!({
      slug: "test-league",
    });

    await waitFor(() => {
      expect(
        screen.getByRole("button", { name: /create league/i })
      ).toBeEnabled();
    });
  });
});
