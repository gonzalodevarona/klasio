import { renderHook, waitFor } from "@testing-library/react";
import { useUsersByIds } from "../useUsersByIds";

global.fetch = jest.fn();

describe("useUsersByIds", () => {
  beforeEach(() => (global.fetch as jest.Mock).mockReset());

  it("fetches /api/v1/users/by-ids?ids=... and returns a map", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => [
        { id: "u-1", fullName: "Juan Perez", role: "PROFESSOR" },
        { id: "u-2", fullName: "Maria Lopez", role: "ADMIN" },
      ],
    });
    const { result } = renderHook(() => useUsersByIds(["u-1", "u-2"]));
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.users).toEqual({
      "u-1": { id: "u-1", fullName: "Juan Perez", role: "PROFESSOR" },
      "u-2": { id: "u-2", fullName: "Maria Lopez", role: "ADMIN" },
    });
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining("by-ids"),
      expect.any(Object)
    );
  });

  it("returns empty map and skips fetch when ids list is empty", async () => {
    const { result } = renderHook(() => useUsersByIds([]));
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.users).toEqual({});
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it("dedupes ids before issuing a request", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({ ok: true, json: async () => [] });
    renderHook(() => useUsersByIds(["u-1", "u-1", "u-2"]));
    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(1));
    const url = (global.fetch as jest.Mock).mock.calls[0][0] as string;
    // u-1 must not appear twice in the URL
    expect(url.split("u-1").length - 1).toBe(1);
  });
});
