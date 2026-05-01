import { render, screen } from "@testing-library/react";
import TenantBrand from "../TenantBrand";

describe("TenantBrand", () => {
  it("renders skeleton when loading", () => {
    const { container } = render(
      <TenantBrand tenantName={null} tenantLogoUrl={null} loading />
    );
    expect(container.querySelector(".animate-pulse")).not.toBeNull();
    expect(screen.queryByRole("img")).toBeNull();
  });

  it("renders logo and name when both are present", () => {
    render(
      <TenantBrand
        tenantName="Acme League"
        tenantLogoUrl="https://s3/acme.png"
        loading={false}
      />
    );
    expect(screen.getByText("Acme League")).toBeInTheDocument();
    const img = screen.getByRole("img");
    expect(img).toHaveAttribute("src", "https://s3/acme.png");
  });

  it("renders name only when logoUrl is null", () => {
    render(
      <TenantBrand tenantName="Acme League" tenantLogoUrl={null} loading={false} />
    );
    expect(screen.getByText("Acme League")).toBeInTheDocument();
    expect(screen.queryByRole("img")).toBeNull();
  });
});
