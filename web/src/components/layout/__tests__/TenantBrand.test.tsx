import { render, screen } from "@testing-library/react";
import TenantBrand from "../TenantBrand";

describe("TenantBrand", () => {
  it("renders skeleton when loading", () => {
    const { container } = render(
      <TenantBrand tenantName={null} tenantLogoUrl={null} loading />
    );
    const skeleton = container.querySelector(".animate-pulse");
    expect(skeleton).not.toBeNull();
    expect(skeleton).toHaveClass("h-4", "w-28");
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
    expect(img).toHaveAttribute("alt", "Acme League");
  });

  it("renders name only when logoUrl is null", () => {
    render(
      <TenantBrand tenantName="Acme League" tenantLogoUrl={null} loading={false} />
    );
    expect(screen.getByText("Acme League")).toBeInTheDocument();
    expect(screen.queryByRole("img")).toBeNull();
  });
});
