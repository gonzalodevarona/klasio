import { renderHook, act, waitFor } from "@testing-library/react";
import { useRaiseSessionAlert } from "../useSessionActions";
import { api } from "@/lib/api";

// ---------------------------------------------------------------------------
// Module mocks
// ---------------------------------------------------------------------------

jest.mock("@/lib/api", () => ({
  api: {
    post: jest.fn(),
    patch: jest.fn(),
  },
}));

afterEach(() => {
  jest.clearAllMocks();
});

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe("useRaiseSessionAlert", () => {
  it("calls POST /classes/{classId}/sessions/{date}/alert with the reason", async () => {
    (api.post as jest.Mock).mockResolvedValue({});

    const { result } = renderHook(() => useRaiseSessionAlert());

    await act(async () => {
      await result.current.raiseAlert({
        classId: "cls-1",
        sessionDate: "2026-04-20",
        reason: "Professor will be 15 minutes late today",
      });
    });

    expect(api.post).toHaveBeenCalledWith(
      "/classes/cls-1/sessions/2026-04-20/alert",
      { reason: "Professor will be 15 minutes late today" }
    );
  });

  it("surfaces the backend message on failure", async () => {
    (api.post as jest.Mock).mockRejectedValue(
      new Error("Reason must be at least 20 characters.")
    );

    const { result } = renderHook(() => useRaiseSessionAlert());

    await act(async () => {
      await expect(
        result.current.raiseAlert({
          classId: "cls-1",
          sessionDate: "2026-04-20",
          reason: "too short",
        })
      ).rejects.toThrow("Reason must be at least 20 characters.");
    });

    await waitFor(() => {
      expect(result.current.error).toBe("Reason must be at least 20 characters.");
    });
  });
});
