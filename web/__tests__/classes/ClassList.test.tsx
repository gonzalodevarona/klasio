import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import ClassList from "@/components/classes/ClassList";
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

const PROGRAM_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

const mockClasses = {
  content: [
    {
      id: "c1111111-1111-1111-1111-111111111111",
      programId: PROGRAM_ID,
      name: "Kids Beginner Monday",
      level: "BEGINNER",
      type: "RECURRING",
      professorId: "p1111111-1111-1111-1111-111111111111",
      maxStudents: 20,
      status: "ACTIVE",
      createdAt: "2025-01-15T10:00:00Z",
    },
    {
      id: "c2222222-2222-2222-2222-222222222222",
      programId: PROGRAM_ID,
      name: "Advanced Workshop",
      level: "ADVANCED",
      type: "ONE_TIME",
      professorId: null,
      maxStudents: 15,
      status: "INACTIVE",
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
  (api.get as jest.Mock).mockResolvedValue(mockClasses);
});

describe("ClassList", () => {
  it("renders class data in table", async () => {
    render(<ClassList programId={PROGRAM_ID} />);

    await waitFor(() => {
      expect(screen.getByText("Kids Beginner Monday")).toBeInTheDocument();
    });

    expect(screen.getByText("Advanced Workshop")).toBeInTheDocument();
    expect(screen.getAllByText("Beginner").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Advanced").length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("Recurring")).toBeInTheDocument();
    expect(screen.getByText("One-Time")).toBeInTheDocument();
    expect(screen.getAllByText("ACTIVE").length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("INACTIVE")).toBeInTheDocument();
  });

  it("links class names to detail pages", async () => {
    render(<ClassList programId={PROGRAM_ID} />);

    await waitFor(() => {
      expect(screen.getByText("Kids Beginner Monday")).toBeInTheDocument();
    });

    const link = screen.getByText("Kids Beginner Monday").closest("a");
    expect(link).toHaveAttribute(
      "href",
      `/programs/${PROGRAM_ID}/classes/c1111111-1111-1111-1111-111111111111`
    );
  });

  it("shows loading state initially", () => {
    (api.get as jest.Mock).mockReturnValue(new Promise(() => {}));
    render(<ClassList programId={PROGRAM_ID} />);

    expect(screen.getByText("Loading classes...")).toBeInTheDocument();
  });

  it("shows error message on fetch failure", async () => {
    (api.get as jest.Mock).mockRejectedValue(new Error("Network error"));
    render(<ClassList programId={PROGRAM_ID} />);

    await waitFor(() => {
      expect(screen.getByText("Network error")).toBeInTheDocument();
    });
  });

  it("shows empty state when no classes exist", async () => {
    (api.get as jest.Mock).mockResolvedValue({
      content: [],
      number: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });
    render(<ClassList programId={PROGRAM_ID} />);

    await waitFor(() => {
      expect(screen.getByText("No classes found")).toBeInTheDocument();
    });
  });

  it("renders pagination controls", async () => {
    (api.get as jest.Mock).mockResolvedValue({
      ...mockClasses,
      totalElements: 25,
      totalPages: 2,
    });
    render(<ClassList programId={PROGRAM_ID} />);

    await waitFor(() => {
      expect(screen.getByText(/Page 1 of 2/)).toBeInTheDocument();
    });

    expect(screen.getByText("Previous")).toBeDisabled();
    expect(screen.getByText("Next")).toBeEnabled();
  });

  it("filters by level", async () => {
    render(<ClassList programId={PROGRAM_ID} />);

    await waitFor(() => {
      expect(screen.getByText("Kids Beginner Monday")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText("Level:"), {
      target: { value: "BEGINNER" },
    });

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith(
        `/programs/${PROGRAM_ID}/classes?page=0&size=20&level=BEGINNER`
      );
    });
  });

  it("filters by status", async () => {
    render(<ClassList programId={PROGRAM_ID} />);

    await waitFor(() => {
      expect(screen.getByText("Kids Beginner Monday")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText("Status:"), {
      target: { value: "ACTIVE" },
    });

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith(
        `/programs/${PROGRAM_ID}/classes?page=0&size=20&status=ACTIVE`
      );
    });
  });
});
