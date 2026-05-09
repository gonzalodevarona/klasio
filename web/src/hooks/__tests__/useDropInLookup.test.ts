import { renderHook, waitFor, act } from "@testing-library/react";
import { useDropInLookup } from "../useDropInLookup";

global.fetch = jest.fn();

describe("useDropInLookup", () => {
  beforeEach(() => (global.fetch as jest.Mock).mockReset());

  it("is idle when phone has fewer than 7 digits", () => {
    const { result } = renderHook(() => useDropInLookup("30012"));
    expect(result.current.status).toBe("idle");
    expect(result.current.data).toBeNull();
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it("sets status to searching immediately when phone has >= 7 digits", () => {
    (global.fetch as jest.Mock).mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ id: "a1", fullName: "Ana", phone: "3001234567", totalVisits: 2, firstVisitAt: null, lastVisitAt: null, converted: false }),
    });
    const { result } = renderHook(() => useDropInLookup("3001234567", 0));
    // Status should transition to searching before the debounce fires
    expect(result.current.status).toBe("searching");
  });

  it("returns found with data on 200 response", async () => {
    const attendee = { id: "a1", fullName: "Ana García", phone: "3001234567", totalVisits: 3, firstVisitAt: null, lastVisitAt: null, converted: false };
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => attendee,
    });

    const { result } = renderHook(() => useDropInLookup("3001234567", 0));
    await waitFor(() => expect(result.current.status).toBe("found"));
    expect(result.current.data).toEqual(attendee);
  });

  it("returns notFound on 404 response", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: false,
      status: 404,
      json: async () => null,
    });

    const { result } = renderHook(() => useDropInLookup("3009999999", 0));
    await waitFor(() => expect(result.current.status).toBe("notFound"));
    expect(result.current.data).toBeNull();
  });

  it("returns error on network failure", async () => {
    (global.fetch as jest.Mock).mockRejectedValueOnce(new Error("Network error"));

    const { result } = renderHook(() => useDropInLookup("3001234567", 0));
    await waitFor(() => expect(result.current.status).toBe("error"));
  });

  it("resets to idle when phone drops below 7 digits", async () => {
    (global.fetch as jest.Mock).mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ id: "a1", fullName: "Ana", phone: "3001234567", totalVisits: 1, firstVisitAt: null, lastVisitAt: null, converted: false }),
    });
    const { result, rerender } = renderHook(({ phone }) => useDropInLookup(phone, 0), {
      initialProps: { phone: "3001234567" },
    });
    await waitFor(() => expect(result.current.status).toBe("found"));
    rerender({ phone: "300" });
    expect(result.current.status).toBe("idle");
    expect(result.current.data).toBeNull();
  });
});
