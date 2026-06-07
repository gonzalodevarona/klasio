import React from "react";
import { render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import messages from "../../../../messages/en.json";
import TenantForm from "../TenantForm";
import { TenantDetail } from "@/lib/types/tenant";

jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn() }),
}));

jest.mock("@/lib/api", () => ({
  api: { post: jest.fn(), postForm: jest.fn(), patch: jest.fn() },
  ApiError: class ApiError extends Error {
    details: unknown[];
    constructor(message: string, details: unknown[] = []) {
      super(message);
      this.details = details;
    }
  },
}));

// LogoUpload uses file APIs not available in jsdom — replace with a stub
jest.mock("../LogoUpload", () => {
  const React = require("react");
  return function LogoUploadMock() {
    return <div data-testid="logo-upload-stub" />;
  };
});

// CountrySelect is a complex component — replace with a stub
jest.mock("../CountrySelect", () => {
  const React = require("react");
  return function CountrySelectMock() {
    return <div data-testid="country-select-stub" />;
  };
});

function wrap(ui: React.ReactNode) {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      {ui}
    </NextIntlClientProvider>
  );
}

const baseTenant: TenantDetail = {
  id: "t-1",
  slug: "acme",
  name: "Acme League",
  discipline: "Tennis",
  status: "ACTIVE",
  createdAt: "2024-01-01T00:00:00Z",
  language: "en",
  timezone: "America/Bogota",
  logoUrl: null,
  contactEmail: "contact@acme.com",
  contactPhone: "3001234567",
  contactPhoneIndicator: "57",
  contactStreet: "Calle 50 #45-12",
  contactCity: "Medellín",
  contactState: "Antioquia",
  contactCountry: "Colombia",
  createdBy: "admin@acme.com",
  deactivatedAt: null,
  deactivatedBy: null,
  selfRegistrationEnabled: true,
};

describe("TenantForm — self-registration toggle", () => {
  it("renders self-registration toggle defaulting to enabled in create mode", () => {
    wrap(<TenantForm />);

    const checkbox = screen.getByRole("checkbox", { name: /allow self-registration/i });
    expect(checkbox).toBeInTheDocument();
    expect(checkbox).toBeChecked();
  });

  it("renders self-registration toggle with label", () => {
    wrap(<TenantForm />);

    expect(screen.getByLabelText(/allow self-registration/i)).toBeInTheDocument();
  });

  it("reflects selfRegistrationEnabled=false when tenant has it disabled", () => {
    const tenant: TenantDetail = { ...baseTenant, selfRegistrationEnabled: false };
    wrap(<TenantForm tenant={tenant} />);

    const checkbox = screen.getByRole("checkbox", { name: /allow self-registration/i });
    expect(checkbox).not.toBeChecked();
  });
});

describe("TenantForm — invite link in edit mode", () => {
  it("shows invite link when in edit mode with a slug", () => {
    wrap(<TenantForm tenant={baseTenant} />);

    const input = screen.getByTestId("invite-link-input") as HTMLInputElement;
    expect(input).toBeInTheDocument();
    expect(input.value).toContain("acme.");
    expect(input.value).toContain("/register");
  });

  it("invite link value includes the tenant slug and /register path", () => {
    wrap(<TenantForm tenant={baseTenant} />);

    const input = screen.getByTestId("invite-link-input") as HTMLInputElement;
    expect(input.value).toBe(
      `https://acme.${process.env.NEXT_PUBLIC_ROOT_DOMAIN ?? "localhost"}/register`
    );
  });

  it("shows a Copy button in edit mode", () => {
    wrap(<TenantForm tenant={baseTenant} />);

    expect(screen.getByRole("button", { name: /^copy$/i })).toBeInTheDocument();
  });

  it("does not show invite link section in create mode", () => {
    wrap(<TenantForm />);

    expect(screen.queryByTestId("invite-link-input")).toBeNull();
  });
});
