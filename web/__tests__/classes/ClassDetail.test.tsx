import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import ClassDetail from "@/components/classes/ClassDetail";
import { api, ApiError } from "@/lib/api";
import { ProgramClassDetail } from "@/lib/types/programClass";

jest.mock("@/lib/api", () => ({
  api: {
    post: jest.fn(),
    delete: jest.fn(),
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

const PROGRAM_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

const mockClass: ProgramClassDetail = {
  id: "c1111111-1111-1111-1111-111111111111",
  tenantId: "t1111111-1111-1111-1111-111111111111",
  programId: PROGRAM_ID,
  name: "Kids Beginner Monday",
  level: "BEGINNER" as const,
  type: "RECURRING" as const,
  professorId: "p1111111-1111-1111-1111-111111111111",
  maxStudents: 20,
  status: "ACTIVE" as const,
  scheduleEntries: [
    { dayOfWeek: "MONDAY", startTime: "18:00", endTime: "20:00" },
  ],
  createdAt: "2025-01-15T10:00:00Z",
  createdBy: "u1111111-1111-1111-1111-111111111111",
  updatedAt: undefined,
  updatedBy: undefined,
};

const inactiveClass: ProgramClassDetail = {
  ...mockClass,
  status: "INACTIVE" as const,
};

const mockOnChanged = jest.fn();

beforeEach(() => {
  jest.clearAllMocks();
});

describe("ClassDetail", () => {
  it("renders class details", () => {
    render(
      <ClassDetail
        programId={PROGRAM_ID}
        programClass={mockClass}
        onChanged={mockOnChanged}
      />
    );

    expect(screen.getAllByText("Kids Beginner Monday").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Beginner").length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("20")).toBeInTheDocument();
    expect(
      screen.getByText("p1111111-1111-1111-1111-111111111111")
    ).toBeInTheDocument();
  });

  it("shows Edit link", () => {
    render(
      <ClassDetail
        programId={PROGRAM_ID}
        programClass={mockClass}
        onChanged={mockOnChanged}
      />
    );

    const editLink = screen.getByText("Edit");
    expect(editLink).toBeInTheDocument();
    expect(editLink.closest("a")).toHaveAttribute(
      "href",
      `/programs/${PROGRAM_ID}/classes/${mockClass.id}/edit`
    );
  });

  it("shows Deactivate button when class is ACTIVE", () => {
    render(
      <ClassDetail
        programId={PROGRAM_ID}
        programClass={mockClass}
        onChanged={mockOnChanged}
      />
    );

    expect(screen.getByText("Deactivate Class")).toBeInTheDocument();
    expect(screen.queryByText("Reactivate Class")).not.toBeInTheDocument();
  });

  it("shows Reactivate button when class is INACTIVE", () => {
    render(
      <ClassDetail
        programId={PROGRAM_ID}
        programClass={inactiveClass}
        onChanged={mockOnChanged}
      />
    );

    expect(screen.getByText("Reactivate Class")).toBeInTheDocument();
    expect(screen.queryByText("Deactivate Class")).not.toBeInTheDocument();
  });

  it("deactivates class on confirm", async () => {
    (api.post as jest.Mock).mockResolvedValue(undefined);

    render(
      <ClassDetail
        programId={PROGRAM_ID}
        programClass={mockClass}
        onChanged={mockOnChanged}
      />
    );

    fireEvent.click(screen.getByText("Deactivate Class"));

    expect(
      screen.getByText("Are you sure you want to deactivate this class?")
    ).toBeInTheDocument();

    fireEvent.click(screen.getByText("Confirm Deactivation"));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith(
        `/programs/${PROGRAM_ID}/classes/${mockClass.id}/deactivate`
      );
    });
  });

  it("shows success feedback after deactivation", async () => {
    (api.post as jest.Mock).mockResolvedValue(undefined);

    render(
      <ClassDetail
        programId={PROGRAM_ID}
        programClass={mockClass}
        onChanged={mockOnChanged}
      />
    );

    fireEvent.click(screen.getByText("Deactivate Class"));
    fireEvent.click(screen.getByText("Confirm Deactivation"));

    await waitFor(() => {
      expect(
        screen.getByText(/class has been deactivated/i)
      ).toBeInTheDocument();
    });
  });
});
