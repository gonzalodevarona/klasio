import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import ProfessorList from "@/components/professors/ProfessorList";
import { api } from "@/lib/api";

jest.mock("@/lib/api", () => ({
  api: {
    get: jest.fn(),
  },
}));

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

const mockProfessors = {
  content: [
    {
      id: "11111111-1111-1111-1111-111111111111",
      firstName: "Carlos",
      lastName: "Martinez",
      email: "carlos@example.com",
      phoneNumber: "+573001234567",
      status: "ACTIVE",
      createdAt: "2025-01-15T10:00:00Z",
    },
    {
      id: "33333333-3333-3333-3333-333333333333",
      firstName: "Ana",
      lastName: "Lopez",
      email: "ana@example.com",
      phoneNumber: null,
      status: "INVITED",
      createdAt: "2025-02-20T14:30:00Z",
    },
  ],
  number: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
};

beforeEach(() => {
  jest.clearAllMocks();
  (api.get as jest.Mock).mockResolvedValue(mockProfessors);
});

describe("ProfessorList", () => {
  it("renders professor data in table", async () => {
    render(<ProfessorList />);

    await waitFor(() => {
      expect(screen.getByText("Carlos Martinez")).toBeInTheDocument();
    });

    expect(screen.getByText("Ana Lopez")).toBeInTheDocument();
    expect(screen.getByText("carlos@example.com")).toBeInTheDocument();
    expect(screen.getByText("ana@example.com")).toBeInTheDocument();
    expect(screen.getByText("ACTIVE")).toBeInTheDocument();
    expect(screen.getByText("INVITED")).toBeInTheDocument();
  });

  it("links professor names to detail pages", async () => {
    render(<ProfessorList />);

    await waitFor(() => {
      expect(screen.getByText("Carlos Martinez")).toBeInTheDocument();
    });

    const link = screen.getByText("Carlos Martinez").closest("a");
    expect(link).toHaveAttribute(
      "href",
      "/professors/11111111-1111-1111-1111-111111111111"
    );
  });

  it("shows loading state initially", () => {
    (api.get as jest.Mock).mockReturnValue(new Promise(() => {}));
    render(<ProfessorList />);

    expect(screen.getByText("Loading professors...")).toBeInTheDocument();
  });

  it("shows error message on fetch failure", async () => {
    (api.get as jest.Mock).mockRejectedValue(new Error("Network error"));
    render(<ProfessorList />);

    await waitFor(() => {
      expect(screen.getByText("Network error")).toBeInTheDocument();
    });
  });

  it("shows empty state when no professors exist", async () => {
    (api.get as jest.Mock).mockResolvedValue({
      content: [],
      number: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });
    render(<ProfessorList />);

    await waitFor(() => {
      expect(screen.getByText("No professors found")).toBeInTheDocument();
    });
  });

  it("renders pagination controls", async () => {
    (api.get as jest.Mock).mockResolvedValue({
      ...mockProfessors,
      totalElements: 25,
      totalPages: 2,
    });
    render(<ProfessorList />);

    await waitFor(() => {
      expect(screen.getByText(/Page 1 of 2/)).toBeInTheDocument();
    });

    expect(screen.getByText("Previous")).toBeDisabled();
    expect(screen.getByText("Next")).toBeEnabled();
  });

  it("changes page on Next click", async () => {
    (api.get as jest.Mock).mockResolvedValue({
      ...mockProfessors,
      totalElements: 25,
      totalPages: 2,
    });
    render(<ProfessorList />);

    await waitFor(() => {
      expect(screen.getByText("Next")).toBeEnabled();
    });

    fireEvent.click(screen.getByText("Next"));

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith("/professors?page=1&size=20");
    });
  });

  it("filters by status", async () => {
    render(<ProfessorList />);

    await waitFor(() => {
      expect(screen.getByText("Carlos Martinez")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText("Status:"), {
      target: { value: "ACTIVE" },
    });

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith(
        "/professors?page=0&size=20&status=ACTIVE"
      );
    });
  });
});
