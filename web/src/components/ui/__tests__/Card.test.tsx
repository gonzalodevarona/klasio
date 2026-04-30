import React from "react";
import { render, screen } from "@testing-library/react";
import { Card } from "../Card";

describe("Card", () => {
  it("renders children", () => {
    render(<Card>Hello</Card>);
    expect(screen.getByText("Hello")).toBeInTheDocument();
  });

  it("applies light surface and border classes by default", () => {
    render(<Card data-testid="card">X</Card>);
    const el = screen.getByTestId("card");
    expect(el).toHaveClass(
      "rounded-k-lg",
      "bg-k-surface",
      "border-[1.5px]",
      "border-k-border"
    );
  });

  it("applies dark surface when dark=true", () => {
    render(
      <Card dark data-testid="card">
        X
      </Card>
    );
    const el = screen.getByTestId("card");
    expect(el).toHaveClass("bg-k-dark");
    expect(el).not.toHaveClass("bg-k-surface");
    expect(el).not.toHaveClass("border-[1.5px]");
  });

  it("padding=md (default) applies p-6", () => {
    render(<Card data-testid="card">X</Card>);
    expect(screen.getByTestId("card")).toHaveClass("p-6");
  });

  it("padding=sm applies p-4", () => {
    render(
      <Card padding="sm" data-testid="card">
        X
      </Card>
    );
    expect(screen.getByTestId("card")).toHaveClass("p-4");
  });

  it("padding=lg applies p-8", () => {
    render(
      <Card padding="lg" data-testid="card">
        X
      </Card>
    );
    expect(screen.getByTestId("card")).toHaveClass("p-8");
  });

  it("merges caller className", () => {
    render(
      <Card className="custom-class" data-testid="card">
        X
      </Card>
    );
    expect(screen.getByTestId("card")).toHaveClass("custom-class");
  });
});
