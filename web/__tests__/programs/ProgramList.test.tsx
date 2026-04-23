import { screen, fireEvent, waitFor } from "@testing-library/react";
import { renderWithIntl as render } from "../../__test-support__/renderWithIntl";
import ProgramList from "@/components/programs/ProgramList";
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

const mockPrograms = {
  content: [
    {
      id: "11111111-1111-1111-1111-111111111111",
      name: "Kids Football",
      status: "ACTIVE",
      createdAt: "2025-01-15T10:00:00Z",
    },
    {
      id: "33333333-3333-3333-3333-333333333333",
      name: "Youth Swimming",
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
  (api.get as jest.Mock).mockResolvedValue(mockPrograms);
});

describe("ProgramList", () => {
  it("renders program data in table", async () => {
    render(<ProgramList />);

    await waitFor(() => {
      expect(screen.getByText("Kids Football")).toBeInTheDocument();
    });

    expect(screen.getByText("Youth Swimming")).toBeInTheDocument();
    expect(screen.getByText("ACTIVE")).toBeInTheDocument();
    expect(screen.getByText("INACTIVE")).toBeInTheDocument();
  });

  it("links program names to detail pages", async () => {
    render(<ProgramList />);

    await waitFor(() => {
      expect(screen.getByText("Kids Football")).toBeInTheDocument();
    });

    const link = screen.getByText("Kids Football").closest("a");
    expect(link).toHaveAttribute(
      "href",
      "/programs/11111111-1111-1111-1111-111111111111"
    );
  });

  it("shows loading state initially", () => {
    (api.get as jest.Mock).mockReturnValue(new Promise(() => {}));
    render(<ProgramList />);

    expect(screen.getByText("Loading programs...")).toBeInTheDocument();
  });

  it("shows error message on fetch failure", async () => {
    (api.get as jest.Mock).mockRejectedValue(new Error("Network error"));
    render(<ProgramList />);

    await waitFor(() => {
      expect(screen.getByText("Network error")).toBeInTheDocument();
    });
  });

  it("shows empty state when no programs exist", async () => {
    (api.get as jest.Mock).mockResolvedValue({
      content: [],
      number: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });
    render(<ProgramList />);

    await waitFor(() => {
      expect(screen.getByText("No programs found")).toBeInTheDocument();
    });
  });

  it("renders pagination controls", async () => {
    (api.get as jest.Mock).mockResolvedValue({
      ...mockPrograms,
      totalElements: 25,
      totalPages: 2,
    });
    render(<ProgramList />);

    await waitFor(() => {
      expect(screen.getByText(/Page 1 of 2/)).toBeInTheDocument();
    });

    expect(screen.getByText("Previous")).toBeDisabled();
    expect(screen.getByText("Next")).toBeEnabled();
  });

  it("changes page on Next click", async () => {
    (api.get as jest.Mock).mockResolvedValue({
      ...mockPrograms,
      totalElements: 25,
      totalPages: 2,
    });
    render(<ProgramList />);

    await waitFor(() => {
      expect(screen.getByText("Next")).toBeEnabled();
    });

    fireEvent.click(screen.getByText("Next"));

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith("/programs?page=1&size=20");
    });
  });

  it("filters by status", async () => {
    render(<ProgramList />);

    await waitFor(() => {
      expect(screen.getByText("Kids Football")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText("Status:"), {
      target: { value: "ACTIVE" },
    });

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith(
        "/programs?page=0&size=20&status=ACTIVE"
      );
    });
  });
});
