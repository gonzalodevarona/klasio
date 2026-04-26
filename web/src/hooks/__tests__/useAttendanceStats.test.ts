import { renderHook, waitFor } from "@testing-library/react";
import { useAttendanceStats } from "../useAttendanceStats";

function mockFetchWith(body: unknown, status = 200) {
  global.fetch = jest.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  } as Response);
}

afterEach(() => {
  jest.restoreAllMocks();
});

describe("useAttendanceStats", () => {
  it("returns stats on mount", async () => {
    const mockStats = {
      attended: 8,
      cancelledByStudent: 2,
      cancelledBySystem: 1,
      absent: 3,
      totalHoursConsumed: 16,
      attendanceRatePercent: 72,
    };
    mockFetchWith(mockStats);

    const { result } = renderHook(() => useAttendanceStats());

    await waitFor(() => {
      expect(result.current.stats).toEqual(mockStats);
    });

    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining("/me/attendance/stats"),
      expect.objectContaining({ credentials: "include" })
    );
  });

  it("returns error when fetch fails", async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: () => Promise.resolve({ message: "Server error" }),
    } as Response);

    const { result } = renderHook(() => useAttendanceStats());

    await waitFor(() => {
      expect(result.current.error).not.toBeNull();
    });

    expect(result.current.stats).toBeNull();
    expect(result.current.loading).toBe(false);
  });
});
