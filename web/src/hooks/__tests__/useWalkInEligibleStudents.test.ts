import { renderHook, waitFor } from "@testing-library/react";
import { useWalkInEligibleStudents } from "../useWalkInEligibleStudents";

global.fetch = jest.fn();

describe("useWalkInEligibleStudents", () => {
  beforeEach(() => (global.fetch as jest.Mock).mockReset());

  it("fetches once on mount with no level param when level is null", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({ ok: true, json: async () => [] });
    renderHook(() => useWalkInEligibleStudents("c1", "2026-04-27", "18:00:00", null));
    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(1));
    const url = (global.fetch as jest.Mock).mock.calls[0][0] as string;
    expect(url).toContain("eligible-students");
    expect(url).toContain("startTime=18%3A00%3A00");
    expect(url).not.toContain("level=");
  });

  it("appends level param when provided", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({ ok: true, json: async () => [] });
    renderHook(() => useWalkInEligibleStudents("c1", "2026-04-27", "18:00:00", "BEGINNER"));
    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(1));
    const url = (global.fetch as jest.Mock).mock.calls[0][0] as string;
    expect(url).toContain("level=BEGINNER");
  });

  it("re-fetches when level changes", async () => {
    (global.fetch as jest.Mock).mockResolvedValue({ ok: true, json: async () => [] });
    const { rerender } = renderHook(
      ({ level }) => useWalkInEligibleStudents("c1", "2026-04-27", "18:00:00", level),
      { initialProps: { level: null as string | null } }
    );
    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(1));
    rerender({ level: "ADVANCED" });
    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(2));
    const url = (global.fetch as jest.Mock).mock.calls[1][0] as string;
    expect(url).toContain("level=ADVANCED");
  });
});
