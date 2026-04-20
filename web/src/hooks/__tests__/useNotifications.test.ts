import { renderHook, act, waitFor } from "@testing-library/react";
import { useUnreadCount } from "../useNotifications";

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function mockFetchWith(body: unknown, status = 200) {
  global.fetch = jest.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  } as Response);
}

afterEach(() => {
  jest.restoreAllMocks();
  // Reset hidden to false after each test
  Object.defineProperty(document, "hidden", {
    configurable: true,
    get: () => false,
  });
});

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe("useUnreadCount", () => {
  it("returns count on mount", async () => {
    mockFetchWith({ count: 3 });

    const { result } = renderHook(() => useUnreadCount());

    await waitFor(() => {
      expect(result.current.count).toBe(3);
    });

    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining("/notifications/unread-count"),
      expect.objectContaining({ credentials: "include" })
    );
  });

  it("skips polling when document is hidden", async () => {
    Object.defineProperty(document, "hidden", {
      configurable: true,
      get: () => true,
    });

    global.fetch = jest.fn();

    renderHook(() => useUnreadCount());

    // Wait a tick to confirm fetch was never called
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 50));
    });

    expect(global.fetch).not.toHaveBeenCalled();
  });
});
