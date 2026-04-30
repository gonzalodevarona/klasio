import React from "react";
import { render, screen } from "@testing-library/react";
import { StatCard } from "../StatCard";

describe("StatCard", () => {
  it("renders label and value", () => {
    render(<StatCard label="ACTIVE" value={42} />);
    expect(screen.getByText("ACTIVE")).toBeInTheDocument();
    expect(screen.getByText("42")).toBeInTheDocument();
  });

  it("renders sub when provided", () => {
    render(<StatCard label="X" value="100" sub="+5 this week" />);
    expect(screen.getByText("+5 this week")).toBeInTheDocument();
  });

  it("does not render sub element when omitted", () => {
    const { container } = render(<StatCard label="X" value="100" />);
    expect(container.textContent).not.toContain("undefined");
  });

  it("light theme: label uses text-k-muted, value uses text-k-dark", () => {
    render(<StatCard label="ACTIVE" value={42} />);
    expect(screen.getByText("ACTIVE")).toHaveClass("text-k-muted");
    expect(screen.getByText("42")).toHaveClass("text-k-dark");
  });

  it("dark theme: value uses text-k-volt and label uses text-k-muted", () => {
    render(<StatCard label="ACTIVE" value={42} dark />);
    expect(screen.getByText("ACTIVE")).toHaveClass("text-k-muted");
    expect(screen.getByText("42")).toHaveClass("text-k-volt");
  });

  it("sub uses text-k-muted by default in light theme", () => {
    render(<StatCard label="X" value="1" sub="hello" />);
    expect(screen.getByText("hello")).toHaveClass("text-k-muted");
  });

  it("sub uses text-k-volt by default in dark theme", () => {
    render(<StatCard label="X" value="1" sub="hello" dark />);
    expect(screen.getByText("hello")).toHaveClass("text-k-volt");
  });

  it("subColor overrides default sub class", () => {
    render(<StatCard label="X" value="1" sub="hello" subColor="text-k-danger-text" />);
    const el = screen.getByText("hello");
    expect(el).toHaveClass("text-k-danger-text");
    expect(el).not.toHaveClass("text-k-muted");
  });

  it("value renders typography classes", () => {
    render(<StatCard label="X" value={1} />);
    const el = screen.getByText("1");
    expect(el).toHaveClass("text-[40px]", "font-extrabold", "tracking-[-0.03em]", "leading-none");
  });
});
