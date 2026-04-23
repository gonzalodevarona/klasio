import { screen, fireEvent, waitFor } from "@testing-library/react";
import { renderWithIntl as render } from "../../__test-support__/renderWithIntl";
import ProfessorDetail from "@/components/professors/ProfessorDetail";
import { api, ApiError } from "@/lib/api";
import { ProfessorDetail as ProfessorDetailType } from "@/lib/types/professor";

jest.mock("@/lib/api", () => ({
  api: {
    post: jest.fn(),
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

const activeProfessor: ProfessorDetailType = {
  id: "11111111-1111-1111-1111-111111111111",
  tenantId: "22222222-2222-2222-2222-222222222222",
  firstName: "Carlos",
  lastName: "Martinez",
  email: "carlos@example.com",
  phoneNumber: "+573001234567",
  status: "ACTIVE",
  createdAt: "2025-01-15T10:00:00Z",
  createdBy: "44444444-4444-4444-4444-444444444444",
  updatedAt: "2025-01-20T08:00:00Z",
  updatedBy: "44444444-4444-4444-4444-444444444444",
};

const invitedProfessor: ProfessorDetailType = {
  ...activeProfessor,
  status: "INVITED",
};

const deactivatedProfessor: ProfessorDetailType = {
  ...activeProfessor,
  status: "DEACTIVATED",
};

beforeEach(() => {
  jest.clearAllMocks();
});

describe("ProfessorDetail", () => {
  it("renders professor detail information", () => {
    render(<ProfessorDetail professor={activeProfessor} />);

    expect(screen.getAllByText("carlos@example.com").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Active").length).toBeGreaterThanOrEqual(1);
  });

  it("shows Edit link when professor is not deactivated", () => {
    render(<ProfessorDetail professor={activeProfessor} />);

    const editLink = screen.getByText("Edit");
    expect(editLink).toBeInTheDocument();
    expect(editLink.closest("a")).toHaveAttribute(
      "href",
      `/professors/${activeProfessor.id}/edit`
    );
  });

  it("shows Deactivate button when status is ACTIVE", () => {
    render(<ProfessorDetail professor={activeProfessor} />);

    expect(screen.getByText("Deactivate Professor")).toBeInTheDocument();
    expect(screen.queryByText("Reactivate Professor")).not.toBeInTheDocument();
  });

  it("shows Deactivate button when status is INVITED", () => {
    render(<ProfessorDetail professor={invitedProfessor} />);

    expect(screen.getByText("Deactivate Professor")).toBeInTheDocument();
    expect(screen.queryByText("Reactivate Professor")).not.toBeInTheDocument();
  });

  it("shows Reactivate button when status is DEACTIVATED", () => {
    render(<ProfessorDetail professor={deactivatedProfessor} />);

    expect(screen.getByText("Reactivate Professor")).toBeInTheDocument();
    expect(screen.queryByText("Deactivate Professor")).not.toBeInTheDocument();
  });

  it("does not show Edit link when professor is deactivated", () => {
    render(<ProfessorDetail professor={deactivatedProfessor} />);

    expect(screen.queryByText("Edit")).not.toBeInTheDocument();
  });

  it("shows confirm dialog and calls deactivate API on confirm", async () => {
    (api.post as jest.Mock).mockResolvedValue(undefined);

    render(<ProfessorDetail professor={activeProfessor} />);

    fireEvent.click(screen.getByText("Deactivate Professor"));

    expect(
      screen.getByText("Are you sure you want to deactivate this professor?")
    ).toBeInTheDocument();

    fireEvent.click(screen.getByText("Confirm Deactivation"));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith(
        `/professors/${activeProfessor.id}/deactivate`
      );
    });
  });

  it("cancels deactivation when Cancel is clicked", () => {
    render(<ProfessorDetail professor={activeProfessor} />);

    fireEvent.click(screen.getByText("Deactivate Professor"));
    fireEvent.click(screen.getByText("Cancel"));

    expect(api.post).not.toHaveBeenCalled();
    expect(screen.getByText("Deactivate Professor")).toBeInTheDocument();
  });

  it("calls reactivate API on button click", async () => {
    (api.post as jest.Mock).mockResolvedValue(undefined);

    render(<ProfessorDetail professor={deactivatedProfessor} />);

    fireEvent.click(screen.getByText("Reactivate Professor"));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith(
        `/professors/${deactivatedProfessor.id}/reactivate`
      );
    });
  });

  it("shows success alert after deactivate action", async () => {
    (api.post as jest.Mock).mockResolvedValue(undefined);

    render(<ProfessorDetail professor={activeProfessor} />);

    fireEvent.click(screen.getByText("Deactivate Professor"));
    fireEvent.click(screen.getByText("Confirm Deactivation"));

    await waitFor(() => {
      expect(
        screen.getByText(/professor has been deactivated/i)
      ).toBeInTheDocument();
    });
  });

  it("shows success alert after reactivate action", async () => {
    (api.post as jest.Mock).mockResolvedValue(undefined);

    render(<ProfessorDetail professor={deactivatedProfessor} />);

    fireEvent.click(screen.getByText("Reactivate Professor"));

    await waitFor(() => {
      expect(
        screen.getByText(/professor has been reactivated/i)
      ).toBeInTheDocument();
    });
  });

  it("shows error alert on API failure", async () => {
    const err = new (ApiError as unknown as new (...args: unknown[]) => ApiError)(
      400,
      "PROFESSOR_ALREADY_DEACTIVATED",
      "Professor is already deactivated"
    );
    (api.post as jest.Mock).mockRejectedValue(err);

    render(<ProfessorDetail professor={activeProfessor} />);

    fireEvent.click(screen.getByText("Deactivate Professor"));
    fireEvent.click(screen.getByText("Confirm Deactivation"));

    await waitFor(() => {
      expect(
        screen.getByText("Professor is already deactivated")
      ).toBeInTheDocument();
    });
  });
});
