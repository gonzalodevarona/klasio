import { render, screen } from "@testing-library/react";
import TenantList from "@/components/tenants/TenantList";
import { TenantSummary } from "@/lib/types/tenant";

// Mock next/link
jest.mock("next/link", () => {
  return function MockLink({
    children,
    href,
  }: {
    children: React.ReactNode;
    href: string;
  }) {
    return <a href={href}>{children}</a>;
  };
});

// Mock useTenants hook
const mockUseTenants = jest.fn();
jest.mock("@/hooks/useTenants", () => ({
  useTenants: (...args: unknown[]) => mockUseTenants(...args),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

function createTenant(overrides: Partial<TenantSummary> = {}): TenantSummary {
  return {
    id: "1",
    slug: "test-league",
    name: "Test League",
    sportDiscipline: "Soccer",
    status: "ACTIVE",
    createdAt: "2026-03-15T00:00:00Z",
    ...overrides,
  };
}

describe("TenantList", () => {
  it("renders table with tenant names, slugs, and status badges", () => {
    const tenants = [
      createTenant({ id: "1", name: "Liga de Futbol", slug: "liga-de-futbol" }),
      createTenant({
        id: "2",
        name: "Tennis Club",
        slug: "tennis-club",
        sportDiscipline: "Tennis",
        status: "INACTIVE",
      }),
    ];

    mockUseTenants.mockReturnValue({
      tenants,
      totalPages: 1,
      totalElements: 2,
      loading: false,
      error: null,
      refetch: jest.fn(),
    });

    render(<TenantList />);

    expect(screen.getByText("Liga de Futbol")).toBeInTheDocument();
    expect(screen.getByText("liga-de-futbol")).toBeInTheDocument();
    expect(screen.getByText("Tennis Club")).toBeInTheDocument();
    expect(screen.getByText("tennis-club")).toBeInTheDocument();
    expect(screen.getByText("ACTIVE")).toBeInTheDocument();
    expect(screen.getByText("INACTIVE")).toBeInTheDocument();
  });

  it("shows 'No tenants found' for empty list", () => {
    mockUseTenants.mockReturnValue({
      tenants: [],
      totalPages: 0,
      totalElements: 0,
      loading: false,
      error: null,
      refetch: jest.fn(),
    });

    render(<TenantList />);

    expect(screen.getByText("No tenants found")).toBeInTheDocument();
  });

  it("status badges show correct styling", () => {
    const tenants = [
      createTenant({ id: "1", name: "Active League", status: "ACTIVE" }),
      createTenant({ id: "2", name: "Inactive League", status: "INACTIVE" }),
    ];

    mockUseTenants.mockReturnValue({
      tenants,
      totalPages: 1,
      totalElements: 2,
      loading: false,
      error: null,
      refetch: jest.fn(),
    });

    render(<TenantList />);

    const activeBadge = screen.getByText("ACTIVE");
    expect(activeBadge).toHaveClass("bg-green-100", "text-green-800");

    const inactiveBadge = screen.getByText("INACTIVE");
    expect(inactiveBadge).toHaveClass("bg-red-100", "text-red-800");
  });
});
