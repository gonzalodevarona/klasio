import { renderHook, act, waitFor } from "@testing-library/react";
import { useWalkInRegistration } from "../useWalkInRegistration";

global.fetch = jest.fn();

describe("useWalkInRegistration", () => {
  beforeEach(() => (global.fetch as jest.Mock).mockReset());

  it("posts the correct body and returns the response", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true, status: 201,
      json: async () => ({ registrationId: "r1", status: "PRESENT", intendedHours: 1 }),
    });
    const { result } = renderHook(() => useWalkInRegistration("c1", "2026-04-27"));
    let response: unknown;
    await act(async () => {
      response = await result.current.mutate({ startTime: "18:00:00", studentId: "s1", hoursToCharge: 1 });
    });
    expect(response).toEqual({ registrationId: "r1", status: "PRESENT", intendedHours: 1 });
    expect((global.fetch as jest.Mock).mock.calls[0][0]).toContain("walk-in");
    expect(JSON.parse((global.fetch as jest.Mock).mock.calls[0][1].body)).toEqual({
      startTime: "18:00:00", studentId: "s1", hoursToCharge: 1,
    });
  });

  it("throws on non-2xx with the server error code", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: false, status: 409,
      json: async () => ({ code: "ALREADY_MARKED", message: "x" }),
    });
    const { result } = renderHook(() => useWalkInRegistration("c1", "2026-04-27"));
    await expect(result.current.mutate({ startTime: "18:00:00", studentId: "s1", hoursToCharge: 1 }))
      .rejects.toMatchObject({ code: "ALREADY_MARKED" });
  });
});
