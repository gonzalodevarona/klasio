import { renderHook, waitFor } from "@testing-library/react";
import { useSidebarIdentity } from "../useSidebarIdentity";

function mockFetchByPath(handlers: Record<string, () => Response | Promise<Response>>) {
  global.fetch = jest.fn((input: RequestInfo | URL) => {
    const url = typeof input === "string" ? input : input.toString();
    for (const [path, handler] of Object.entries(handlers)) {
      if (url.includes(path)) return Promise.resolve(handler());
    }
    return Promise.reject(new Error(`Unmocked URL: ${url}`));
  }) as unknown as typeof fetch;
}

function jsonResponse(body: unknown, status = 200): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  } as Response;
}

afterEach(() => jest.restoreAllMocks());

describe("useSidebarIdentity", () => {
  it("does not fetch /me/tenant for SUPERADMIN", async () => {
    mockFetchByPath({
      "/api/me/user-profile": () =>
        jsonResponse({
          firstName: "Su",
          lastName: "Per",
          identityDocumentType: "CC",
          identityNumber: "1",
        }),
    });

    const { result } = renderHook(() =>
      useSidebarIdentity("SUPERADMIN", "tenant-id-ignored")
    );

    await waitFor(() => expect(result.current.displayName).toBe("Su Per"));

    expect(global.fetch).not.toHaveBeenCalledWith(
      expect.stringContaining("/api/me/tenant"),
      expect.anything()
    );
    expect(result.current.tenantName).toBeNull();
    expect(result.current.tenantLogoUrl).toBeNull();
    expect(result.current.tenantFetchFailed).toBe(false);
  });

  it("populates tenantName and tenantLogoUrl on success", async () => {
    mockFetchByPath({
      "/api/me/tenant": () =>
        jsonResponse({
          id: "t1",
          name: "Acme League",
          discipline: "BJJ",
          language: "en",
          logoUrl: "https://s3/acme.png",
        }),
      "/api/me/user-profile": () =>
        jsonResponse({
          firstName: "A",
          lastName: "B",
          identityDocumentType: "CC",
          identityNumber: "1",
        }),
    });

    const { result } = renderHook(() => useSidebarIdentity("ADMIN", "t1"));

    await waitFor(() => expect(result.current.tenantName).toBe("Acme League"));
    expect(result.current.tenantLogoUrl).toBe("https://s3/acme.png");
    expect(result.current.tenantFetchFailed).toBe(false);
  });

  it("flips tenantFetchFailed when /me/tenant returns non-OK", async () => {
    mockFetchByPath({
      "/api/me/tenant": () => jsonResponse({}, 500),
      "/api/me/user-profile": () =>
        jsonResponse({
          firstName: "A",
          lastName: "B",
          identityDocumentType: "CC",
          identityNumber: "1",
        }),
    });

    const { result } = renderHook(() => useSidebarIdentity("ADMIN", "t1"));

    await waitFor(() => expect(result.current.tenantFetchFailed).toBe(true));
    expect(result.current.tenantName).toBeNull();
    expect(result.current.tenantLogoUrl).toBeNull();
  });

  it("flips tenantFetchFailed when /me/tenant rejects", async () => {
    global.fetch = jest.fn((input: RequestInfo | URL) => {
      const url = typeof input === "string" ? input : input.toString();
      if (url.includes("/api/me/tenant")) return Promise.reject(new Error("net"));
      return Promise.resolve(
        jsonResponse({
          firstName: "A",
          lastName: "B",
          identityDocumentType: "CC",
          identityNumber: "1",
        })
      );
    }) as unknown as typeof fetch;

    const { result } = renderHook(() => useSidebarIdentity("ADMIN", "t1"));

    await waitFor(() => expect(result.current.tenantFetchFailed).toBe(true));
  });

  it("returns null logoUrl when backend omits the field", async () => {
    mockFetchByPath({
      "/api/me/tenant": () =>
        jsonResponse({
          id: "t1",
          name: "Acme League",
          discipline: "BJJ",
          language: "en",
        }),
      "/api/me/user-profile": () =>
        jsonResponse({
          firstName: "A",
          lastName: "B",
          identityDocumentType: "CC",
          identityNumber: "1",
        }),
    });

    const { result } = renderHook(() => useSidebarIdentity("ADMIN", "t1"));

    await waitFor(() => expect(result.current.tenantName).toBe("Acme League"));
    expect(result.current.tenantLogoUrl).toBeNull();
  });
});
