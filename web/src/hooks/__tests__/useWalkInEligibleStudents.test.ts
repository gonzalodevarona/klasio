import { renderHook, waitFor, act } from "@testing-library/react";
import { useWalkInEligibleStudents } from "../useWalkInEligibleStudents";

global.fetch = jest.fn();

describe("useWalkInEligibleStudents", () => {
  beforeEach(() => {
    (global.fetch as jest.Mock).mockReset();
    jest.useRealTimers();
  });

  it("fetches without q on initial render (no debounce)", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({ ok: true, json: async () => [] });
    renderHook(() => useWalkInEligibleStudents("c1", "2026-04-27", "18:00:00", ""));
    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(1));
    const url = (global.fetch as jest.Mock).mock.calls[0][0] as string;
    expect(url).toContain("eligible-students");
    expect(url).toContain("startTime=18%3A00%3A00");
    expect(url).not.toContain("q=");
  });

  it("debounces q changes by 300ms and appends q param", async () => {
    jest.useFakeTimers();
    (global.fetch as jest.Mock).mockResolvedValue({ ok: true, json: async () => [] });

    const { rerender } = renderHook(
      ({ q }) => useWalkInEligibleStudents("c1", "2026-04-27", "18:00:00", q),
      { initialProps: { q: "" } }
    );

    // initial fetch fires immediately (delay=0 for empty q)
    act(() => { jest.advanceTimersByTime(0); });
    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(1));

    rerender({ q: "jua" });
    // not fired yet (debounce 300ms)
    expect(global.fetch).toHaveBeenCalledTimes(1);

    act(() => { jest.advanceTimersByTime(300); });
    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(2));

    const url = (global.fetch as jest.Mock).mock.calls[1][0] as string;
    expect(url).toContain("q=jua");
    jest.useRealTimers();
  });
});
