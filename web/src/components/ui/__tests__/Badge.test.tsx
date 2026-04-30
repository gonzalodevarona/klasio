import React from "react";
import { render, screen } from "@testing-library/react";
import { Badge, type BadgeVariant } from "../Badge";

describe("Badge", () => {
  it("renders the label as text", () => {
    render(<Badge variant="active" label="Active" />);
    expect(screen.getByText("Active")).toBeInTheDocument();
  });

  it("applies base pill classes", () => {
    render(<Badge variant="active" label="Active" />);
    const el = screen.getByText("Active");
    expect(el).toHaveClass("rounded-full");
    expect(el).toHaveClass("font-semibold");
    expect(el).toHaveClass("inline-flex");
    expect(el).toHaveClass("items-center");
    expect(el).toHaveClass("gap-1");
  });

  it("applies default size classes", () => {
    render(<Badge variant="active" label="Active" />);
    const el = screen.getByText("Active");
    expect(el).toHaveClass("text-[11px]");
    expect(el).toHaveClass("px-2.5");
    expect(el).toHaveClass("py-0.5");
  });

  it("applies small size classes when small=true", () => {
    render(<Badge variant="active" label="Active" small />);
    const el = screen.getByText("Active");
    expect(el).toHaveClass("text-[10px]");
    expect(el).toHaveClass("px-2");
    expect(el).toHaveClass("py-px");
  });

  const cases: Array<{
    variant: BadgeVariant;
    expected: string[];
  }> = [
    { variant: "active",       expected: ["bg-k-volt", "text-k-volt-text"] },
    { variant: "expiring",     expected: ["bg-k-warn-bg", "text-k-warn-text"] },
    { variant: "inactive",     expected: ["bg-k-bg", "text-k-subtle", "border", "border-k-border"] },
    { variant: "pending",      expected: ["bg-k-warn-bg", "text-k-warn-text"] },
    { variant: "approved",     expected: ["bg-k-volt", "text-k-volt-text"] },
    { variant: "rejected",     expected: ["bg-k-danger-bg", "text-k-danger-text"] },
    { variant: "beginner",     expected: ["bg-k-info-bg", "text-k-info-text"] },
    { variant: "intermediate", expected: ["bg-k-warn-bg", "text-k-warn-text"] },
    { variant: "advanced",     expected: ["bg-k-volt", "text-k-volt-text"] },
    { variant: "info",         expected: ["bg-k-info-bg", "text-k-info-text"] },
  ];

  cases.forEach(({ variant, expected }) => {
    it(`variant="${variant}" applies correct color classes`, () => {
      render(<Badge variant={variant} label="X" />);
      const el = screen.getByText("X");
      expected.forEach((cls) => expect(el).toHaveClass(cls));
    });
  });

  it("merges caller className", () => {
    render(<Badge variant="active" label="X" className="custom-class" />);
    expect(screen.getByText("X")).toHaveClass("custom-class");
  });

  it("renders an icon node passed via the icon prop", () => {
    render(
      <Badge
        variant="active"
        label="WithIcon"
        icon={<svg data-testid="badge-icon" />}
      />,
    );
    expect(screen.getByTestId("badge-icon")).toBeInTheDocument();
  });

  it("forwards the title prop to the underlying span", () => {
    render(<Badge variant="active" label="X" title="tooltip text" />);
    expect(screen.getByText("X")).toHaveAttribute("title", "tooltip text");
  });
});
