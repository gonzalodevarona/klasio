/// <reference types="@testing-library/jest-dom" />
import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { Button } from "../Button";

describe("Button", () => {
  it("renders a <button> by default", () => {
    render(<Button variant="primary">Click</Button>);
    const el = screen.getByRole("button", { name: "Click" });
    expect(el.tagName).toBe("BUTTON");
  });

  it("applies base classes", () => {
    render(<Button variant="primary">X</Button>);
    const el = screen.getByRole("button");
    expect(el).toHaveClass("inline-flex", "items-center", "justify-center", "transition", "duration-k");
    expect(el).toHaveClass("disabled:opacity-40", "disabled:cursor-not-allowed");
  });

  const variants: Array<{
    variant: "primary" | "volt" | "outline" | "ghost" | "danger";
    expected: string[];
  }> = [
    { variant: "primary", expected: ["bg-k-dark", "text-white"] },
    { variant: "volt",    expected: ["bg-k-volt", "text-k-dark"] },
    { variant: "outline", expected: ["bg-transparent", "text-k-dark", "border", "border-k-border"] },
    { variant: "ghost",   expected: ["bg-k-bg", "text-k-subtle"] },
    { variant: "danger",  expected: ["bg-k-danger-bg", "text-k-danger-text", "border", "border-k-danger-text/30"] },
  ];

  variants.forEach(({ variant, expected }) => {
    it(`variant="${variant}" applies correct classes`, () => {
      render(<Button variant={variant}>X</Button>);
      const el = screen.getByRole("button");
      expected.forEach((cls) => expect(el).toHaveClass(cls));
    });
  });

  it("size=sm applies small classes", () => {
    render(<Button variant="primary" size="sm">X</Button>);
    const el = screen.getByRole("button");
    expect(el).toHaveClass("text-xs", "px-3", "py-1.5", "rounded-k-sm");
  });

  it("size=md (default) applies md classes", () => {
    render(<Button variant="primary">X</Button>);
    const el = screen.getByRole("button");
    expect(el).toHaveClass("text-sm", "font-semibold", "px-4", "py-2", "rounded-k-sm");
  });

  it("size=icon applies square classes", () => {
    render(<Button variant="primary" size="icon">X</Button>);
    const el = screen.getByRole("button");
    expect(el).toHaveClass("h-8", "w-8", "p-0", "rounded-k-sm");
  });

  it("disabled attr is set and onClick does not fire", () => {
    const onClick = jest.fn();
    render(<Button variant="primary" disabled onClick={onClick}>X</Button>);
    const el = screen.getByRole("button");
    expect(el).toBeDisabled();
    fireEvent.click(el);
    expect(onClick).not.toHaveBeenCalled();
  });

  it("onClick fires when not disabled", () => {
    const onClick = jest.fn();
    render(<Button variant="primary" onClick={onClick}>X</Button>);
    fireEvent.click(screen.getByRole("button"));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it("defaults type to button to prevent accidental form submission", () => {
    render(<Button variant="primary">X</Button>);
    expect(screen.getByRole("button")).toHaveAttribute("type", "button");
  });

  it("forwards type attribute", () => {
    render(<Button variant="primary" type="submit">X</Button>);
    expect(screen.getByRole("button")).toHaveAttribute("type", "submit");
  });

  it("merges caller className", () => {
    render(<Button variant="primary" className="custom-class">X</Button>);
    expect(screen.getByRole("button")).toHaveClass("custom-class");
  });

  it("asChild renders the child element instead of <button>", () => {
    render(
      <Button variant="primary" asChild>
        <a href="/foo">Link</a>
      </Button>,
    );
    const link = screen.getByRole("link", { name: "Link" });
    expect(link.tagName).toBe("A");
    expect(link).toHaveAttribute("href", "/foo");
    expect(link).toHaveClass("bg-k-dark");
  });
});
