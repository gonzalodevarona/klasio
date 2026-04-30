import { renderHook, act } from "@testing-library/react";
import { useWalkInBulkRegistration } from "../useWalkInBulkRegistration";

global.fetch = jest.fn();

describe("useWalkInBulkRegistration", () => {
  beforeEach(() => (global.fetch as jest.Mock).mockReset());

  it("posts payload and returns parsed result", async () => {
    const fakeResult = {
      results: [
        { studentId: "s1", outcome: "SUCCESS", registrationId: "r1", status: "PRESENT", intendedHours: 2 },
        { studentId: "s2", outcome: "FAILED", errorCode: "INSUFFICIENT_HOURS", errorMessage: "..." },
      ],
      summary: { total: 2, succeeded: 1, failed: 1 },
    };
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true, status: 200, json: async () => fakeResult,
    });

    const { result } = renderHook(() => useWalkInBulkRegistration("c1", "2026-04-27"));
    let response: unknown;
    await act(async () => {
      response = await result.current.mutate({
        startTime: "18:00:00",
        studentIds: ["s1", "s2"],
        hoursToCharge: 2,
      });
    });

    expect(response).toEqual(fakeResult);
    const url = (global.fetch as jest.Mock).mock.calls[0][0] as string;
    expect(url).toContain("walk-in/bulk");
    const body = JSON.parse((global.fetch as jest.Mock).mock.calls[0][1].body);
    expect(body).toEqual({ startTime: "18:00:00", studentIds: ["s1", "s2"], hoursToCharge: 2 });
  });

  it("throws on whole-request 4xx", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: false, status: 409,
      json: async () => ({ code: "MARKING_WINDOW", message: "closed" }),
    });
    const { result } = renderHook(() => useWalkInBulkRegistration("c1", "2026-04-27"));
    await expect(result.current.mutate({
      startTime: "18:00:00", studentIds: ["s1"], hoursToCharge: 1,
    })).rejects.toMatchObject({ code: "MARKING_WINDOW" });
  });
});
