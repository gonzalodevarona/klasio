import { renderHook, waitFor } from "@testing-library/react";
import { useProgramPlans, useProgramPlanDetail } from "@/hooks/useProgramPlans";

const mockPlans = [
  {
    id: "plan-1",
    name: "4 Hours",
    modality: "HOURS_BASED" as const,
    cost: 90000,
    hours: 4,
    managerId: "manager-1",
    status: "ACTIVE" as const,
  },
  {
    id: "plan-2",
    name: "8 Hours",
    modality: "HOURS_BASED" as const,
    cost: 160000,
    hours: 8,
    managerId: "manager-2",
    status: "ACTIVE" as const,
  },
];

const mockPlanDetail = {
  ...mockPlans[0],
  programId: "program-1",
  tenantId: "tenant-1",
  scheduleEntries: [],
  createdAt: "2024-01-01T00:00:00Z",
  createdBy: "user-1",
  updatedAt: null,
  updatedBy: null,
};

global.fetch = jest.fn();

beforeEach(() => {
  jest.clearAllMocks();
  localStorage.setItem("auth_token", "test-token");
});

describe("useProgramPlans", () => {
  it("should fetch plans on mount", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockPlans,
    });

    const { result } = renderHook(() => useProgramPlans("program-1"));

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.plans).toHaveLength(2);
    expect(result.current.plans[0].name).toBe("4 Hours");
    expect(result.current.error).toBeNull();
  });

  it("should handle errors", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: false,
      status: 500,
      statusText: "Internal Server Error",
      json: async () => ({ error: { code: "INTERNAL_ERROR", message: "Server error" } }),
    });

    const { result } = renderHook(() => useProgramPlans("program-1"));

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.error).toBe("Server error");
    expect(result.current.plans).toHaveLength(0);
  });

  it("should call correct URL", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => [],
    });

    renderHook(() => useProgramPlans("abc-123"));

    await waitFor(() =>
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/programs/abc-123/plans"),
        expect.any(Object)
      )
    );
  });
});

describe("useProgramPlanDetail", () => {
  it("should fetch plan detail on mount", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockPlanDetail,
    });

    const { result } = renderHook(() =>
      useProgramPlanDetail("program-1", "plan-1")
    );

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.plan).not.toBeNull();
    expect(result.current.plan?.name).toBe("4 Hours");
    expect(result.current.error).toBeNull();
  });

  it("should handle errors", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: false,
      status: 404,
      statusText: "Not Found",
      json: async () => ({ error: { code: "PLAN_NOT_FOUND", message: "Plan not found" } }),
    });

    const { result } = renderHook(() =>
      useProgramPlanDetail("program-1", "plan-1")
    );

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.error).toBe("Plan not found");
    expect(result.current.plan).toBeNull();
  });
});
