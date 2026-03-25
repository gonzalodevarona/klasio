import { renderHook, waitFor } from "@testing-library/react";
import { useProfessors, useProfessorDetail } from "@/hooks/useProfessors";
import { api } from "@/lib/api";

jest.mock("@/lib/api", () => ({
  api: {
    get: jest.fn(),
  },
}));

const mockListResponse = {
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
  ],
  number: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
};

const mockDetail = {
  id: "11111111-1111-1111-1111-111111111111",
  tenantId: "22222222-2222-2222-2222-222222222222",
  firstName: "Carlos",
  lastName: "Martinez",
  email: "carlos@example.com",
  phoneNumber: "+573001234567",
  status: "ACTIVE",
  createdAt: "2025-01-15T10:00:00Z",
  createdBy: "44444444-4444-4444-4444-444444444444",
  updatedAt: null,
  updatedBy: null,
};

beforeEach(() => {
  jest.clearAllMocks();
});

describe("useProfessors", () => {
  it("fetches professors on mount", async () => {
    (api.get as jest.Mock).mockResolvedValue(mockListResponse);

    const { result } = renderHook(() => useProfessors());

    expect(result.current.loading).toBe(true);

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.professors).toEqual(mockListResponse.content);
    expect(result.current.totalPages).toBe(1);
    expect(result.current.totalElements).toBe(1);
    expect(result.current.error).toBeNull();
    expect(api.get).toHaveBeenCalledWith("/professors?page=0&size=20");
  });

  it("passes pagination parameters", async () => {
    (api.get as jest.Mock).mockResolvedValue(mockListResponse);

    renderHook(() => useProfessors(2, 10));

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith("/professors?page=2&size=10");
    });
  });

  it("passes status filter parameter", async () => {
    (api.get as jest.Mock).mockResolvedValue(mockListResponse);

    renderHook(() => useProfessors(0, 20, "ACTIVE"));

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith(
        "/professors?page=0&size=20&status=ACTIVE"
      );
    });
  });

  it("handles fetch error", async () => {
    (api.get as jest.Mock).mockRejectedValue(new Error("Network error"));

    const { result } = renderHook(() => useProfessors());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBe("Network error");
    expect(result.current.professors).toEqual([]);
  });
});

describe("useProfessorDetail", () => {
  it("fetches professor detail by id", async () => {
    (api.get as jest.Mock).mockResolvedValue(mockDetail);

    const { result } = renderHook(() =>
      useProfessorDetail("11111111-1111-1111-1111-111111111111")
    );

    expect(result.current.loading).toBe(true);

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.professor).toEqual(mockDetail);
    expect(result.current.error).toBeNull();
    expect(api.get).toHaveBeenCalledWith(
      "/professors/11111111-1111-1111-1111-111111111111"
    );
  });

  it("handles fetch error", async () => {
    (api.get as jest.Mock).mockRejectedValue(new Error("Not found"));

    const { result } = renderHook(() => useProfessorDetail("nonexistent-id"));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBe("Not found");
    expect(result.current.professor).toBeNull();
  });
});
