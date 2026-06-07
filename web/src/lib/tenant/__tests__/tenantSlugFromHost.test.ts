import { tenantSlugFromHost } from "../tenantSlugFromHost";

describe("tenantSlugFromHost", () => {
  const ROOT = "klasio.app";
  it("extracts subdomain from prod host", () => {
    expect(tenantSlugFromHost("acme.klasio.app", ROOT)).toBe("acme");
  });
  it("extracts subdomain with port (dev *.localhost)", () => {
    expect(tenantSlugFromHost("acme.localhost:3000", "localhost")).toBe("acme");
  });
  it("returns null for apex (no subdomain)", () => {
    expect(tenantSlugFromHost("klasio.app", ROOT)).toBeNull();
  });
  it("ignores reserved labels www/app", () => {
    expect(tenantSlugFromHost("www.klasio.app", ROOT)).toBeNull();
    expect(tenantSlugFromHost("app.klasio.app", ROOT)).toBeNull();
  });
  it("returns null for null/empty host", () => {
    expect(tenantSlugFromHost(null, ROOT)).toBeNull();
    expect(tenantSlugFromHost("", ROOT)).toBeNull();
  });
  it("lowercases the slug", () => {
    expect(tenantSlugFromHost("ACME.klasio.app", ROOT)).toBe("acme");
  });
});
