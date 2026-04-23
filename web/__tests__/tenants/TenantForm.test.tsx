import { screen, waitFor } from "@testing-library/react";
import { renderWithIntl } from "../../__test-support__/renderWithIntl";
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

// Mock complex sub-components to keep tests focused on TenantForm logic
jest.mock("@/components/tenants/CountrySelect", () => {
  return function MockCountrySelect({
    onChange,
    error,
  }: {
    onChange: (c: { name: string; dialCode: string; flag: string }) => void;
    error?: string;
  }) {
    return (
      <div>
        <button
          type="button"
          onClick={() => onChange({ name: "Colombia", dialCode: "57", flag: "🇨🇴" })}
        >
          Select country
        </button>
        {error && <p>{error}</p>}
      </div>
    );
  };
});

jest.mock("@/components/tenants/LogoUpload", () => {
  return function MockLogoUpload({
    onFileSelect,
    error,
  }: {
    onFileSelect: (f: File | null) => void;
    error?: string;
  }) {
    return (
      <div>
        <label htmlFor="mock-logo">Logo</label>
        <input
          id="mock-logo"
          type="file"
          onChange={(e) => onFileSelect(e.target.files?.[0] ?? null)}
        />
        {error && <p>{error}</p>}
      </div>
    );
  };
});

// Import ApiError after mock setup
import { ApiError } from "@/lib/api";

beforeEach(() => {
  jest.clearAllMocks();
});

async function fillRequiredFields(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText(/^name/i), "Test League");
  await user.type(screen.getByLabelText(/^discipline/i), "Soccer");
  await user.selectOptions(screen.getByLabelText(/language/i), "es");
  await user.type(screen.getByLabelText(/contact email/i), "test@league.com");
  await user.click(screen.getByRole("button", { name: /select country/i }));
  await user.type(screen.getByLabelText(/contact phone/i), "3001234567");
  await user.type(screen.getByLabelText(/street address/i), "Calle 50");
  await user.type(screen.getByLabelText(/city/i), "Medellin");
  await user.type(screen.getByLabelText(/state/i), "Antioquia");
  const logo = new File(["logo"], "logo.png", { type: "image/png" });
  await user.upload(screen.getByLabelText(/logo/i), logo);
}

describe("TenantForm", () => {
  it("renders all form fields", () => {
    renderWithIntl(<TenantForm />);

    expect(screen.getByLabelText(/^name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^discipline/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/language/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^slug/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/contact email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/contact phone/i)).toBeInTheDocument();
    expect(screen.getByText(/contact address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/logo/i)).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /create tenant/i })
    ).toBeInTheDocument();
  });

  it("shows validation errors for empty required fields", async () => {
    const user = userEvent.setup();
    renderWithIntl(<TenantForm />);

    await user.click(screen.getByRole("button", { name: /create tenant/i }));

    expect(screen.getByText("Name is required.")).toBeInTheDocument();
    expect(screen.getByText("Discipline is required.")).toBeInTheDocument();
    expect(screen.getByText("Contact email is required.")).toBeInTheDocument();

    expect(postFormMock).not.toHaveBeenCalled();
  });

  it("shows validation error for invalid email", async () => {
    const user = userEvent.setup();
    renderWithIntl(<TenantForm />);

    await user.type(screen.getByLabelText(/^name/i), "Test League");
    await user.type(screen.getByLabelText(/^discipline/i), "Soccer");
    await user.type(screen.getByLabelText(/contact email/i), "not-an-email");

    await user.click(screen.getByRole("button", { name: /create tenant/i }));

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
      discipline: "Soccer",
      language: "es",
      contactEmail: "test@league.com",
      status: "ACTIVE",
      createdAt: "2026-03-15T00:00:00Z",
      logoUrl: null,
      contactPhone: "3001234567",
      contactPhoneIndicator: "57",
      contactStreet: "Calle 50",
      contactCity: "Medellin",
      contactState: "Antioquia",
      contactCountry: "Colombia",
      createdBy: "admin",
      deactivatedAt: null,
      deactivatedBy: null,
    });

    renderWithIntl(<TenantForm />);

    await fillRequiredFields(user);

    await user.click(screen.getByRole("button", { name: /create tenant/i }));

    await waitFor(() => {
      expect(postFormMock).toHaveBeenCalledTimes(1);
    });

    const [path, formData] = postFormMock.mock.calls[0];
    expect(path).toBe("/tenants");
    expect(formData).toBeInstanceOf(FormData);
    expect(formData.get("name")).toBe("Test League");
    expect(formData.get("discipline")).toBe("Soccer");
    expect(formData.get("language")).toBe("es");
    expect(formData.get("contactEmail")).toBe("test@league.com");
    expect(formData.get("contactPhone")).toBe("3001234567");
    expect(formData.get("contactStreet")).toBe("Calle 50");
    expect(formData.get("contactCity")).toBe("Medellin");
    expect(formData.get("contactState")).toBe("Antioquia");
    expect(formData.get("contactCountry")).toBe("Colombia");
    expect(formData.get("contactPhoneIndicator")).toBe("57");

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

    renderWithIntl(<TenantForm />);

    await fillRequiredFields(user);

    await user.click(screen.getByRole("button", { name: /create tenant/i }));

    await waitFor(() => {
      expect(
        screen.getByText("A league with this slug already exists.")
      ).toBeInTheDocument();
    });
  });

  it("shows slug preview as user types name", async () => {
    const user = userEvent.setup();
    renderWithIntl(<TenantForm />);

    await user.type(screen.getByLabelText(/^name/i), "Liga de Futbol");

    const preview = screen.getByTestId("slug-preview");
    expect(preview).toHaveTextContent("liga-de-futbol");
  });

  it("shows slug preview with accent stripping", async () => {
    const user = userEvent.setup();
    renderWithIntl(<TenantForm />);

    await user.type(screen.getByLabelText(/^name/i), "Liga Antioquena");

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

    renderWithIntl(<TenantForm />);

    await fillRequiredFields(user);

    await user.click(screen.getByRole("button", { name: /create tenant/i }));

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /creating/i })).toBeDisabled();
    });

    // Resolve the promise to clean up
    resolveSubmit!({
      slug: "test-league",
    });

    await waitFor(() => {
      expect(
        screen.getByRole("button", { name: /create tenant/i })
      ).toBeEnabled();
    });
  });
});
