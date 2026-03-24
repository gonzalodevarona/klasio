import { renderHook, waitFor } from "@testing-library/react";
import { usePrograms, useProgramDetail } from "@/hooks/usePrograms";
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
      name: "Kids Football",
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
  name: "Kids Football",
  status: "ACTIVE",
  createdAt: "2025-01-15T10:00:00Z",
  createdBy: "44444444-4444-4444-4444-444444444444",
  updatedAt: "2025-01-15T10:00:00Z",
  updatedBy: "44444444-4444-4444-4444-444444444444",
};

beforeEach(() => {
  jest.clearAllMocks();
});

describe("usePrograms", () => {
  it("fetches programs on mount", async () => {
    (api.get as jest.Mock).mockResolvedValue(mockListResponse);

    const { result } = renderHook(() => usePrograms());

    expect(result.current.loading).toBe(true);

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.programs).toEqual(mockListResponse.content);
    expect(result.current.totalPages).toBe(1);
    expect(result.current.totalElements).toBe(1);
    expect(result.current.error).toBeNull();
    expect(api.get).toHaveBeenCalledWith("/programs?page=0&size=20");
  });

  it("passes pagination parameters", async () => {
    (api.get as jest.Mock).mockResolvedValue(mockListResponse);

    renderHook(() => usePrograms(2, 10));

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith("/programs?page=2&size=10");
    });
  });

  it("passes status filter parameter", async () => {
    (api.get as jest.Mock).mockResolvedValue(mockListResponse);

    renderHook(() => usePrograms(0, 20, "ACTIVE"));

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith(
        "/programs?page=0&size=20&status=ACTIVE"
      );
    });
  });

  it("handles fetch error", async () => {
    (api.get as jest.Mock).mockRejectedValue(new Error("Network error"));

    const { result } = renderHook(() => usePrograms());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBe("Network error");
    expect(result.current.programs).toEqual([]);
  });
});

describe("useProgramDetail", () => {
  it("fetches program detail by id", async () => {
    (api.get as jest.Mock).mockResolvedValue(mockDetail);

    const { result } = renderHook(() =>
      useProgramDetail("11111111-1111-1111-1111-111111111111")
    );

    expect(result.current.loading).toBe(true);

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.program).toEqual(mockDetail);
    expect(result.current.error).toBeNull();
    expect(api.get).toHaveBeenCalledWith(
      "/programs/11111111-1111-1111-1111-111111111111"
    );
  });

  it("handles fetch error", async () => {
    (api.get as jest.Mock).mockRejectedValue(new Error("Not found"));

    const { result } = renderHook(() => useProgramDetail("nonexistent-id"));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBe("Not found");
    expect(result.current.program).toBeNull();
  });
});
