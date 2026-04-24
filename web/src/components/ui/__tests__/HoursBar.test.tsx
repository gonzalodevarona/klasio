import React from "react";
import { render, screen } from "@testing-library/react";
import { HoursBar } from "../HoursBar";

describe("HoursBar", () => {
  it("renders the used/total label", () => {
    render(<HoursBar used={3} total={10} />);
    expect(screen.getByText("3/10h")).toBeInTheDocument();
  });

  it("fill width is computed as remaining / total * 100", () => {
    render(<HoursBar used={3} total={10} />);
    const fill = screen.getByTestId("hours-bar-fill");
    expect(fill).toHaveStyle({ width: "70%" });
  });

  it("clamps width at 100% when bonus hours pushed remaining over total", () => {
    render(<HoursBar used={0} total={10} />);
    const fill = screen.getByTestId("hours-bar-fill");
    expect(fill).toHaveStyle({ width: "100%" });
  });

  it("clamps remaining at 0 when used > total (width 0%)", () => {
    render(<HoursBar used={15} total={10} />);
    const fill = screen.getByTestId("hours-bar-fill");
    expect(fill).toHaveStyle({ width: "0%" });
  });

  it("when total=0 width is 0%", () => {
    render(<HoursBar used={0} total={0} />);
    const fill = screen.getByTestId("hours-bar-fill");
    expect(fill).toHaveStyle({ width: "0%" });
  });

  it("color = bg-k-volt when remaining >= 100% (used=0)", () => {
    render(<HoursBar used={0} total={10} />);
    expect(screen.getByTestId("hours-bar-fill")).toHaveClass("bg-k-volt");
  });

  it("color = bg-[#8AE800] when 66 <= pct < 100 (used=3, total=10 → 70%)", () => {
    render(<HoursBar used={3} total={10} />);
    expect(screen.getByTestId("hours-bar-fill")).toHaveClass("bg-[#8AE800]");
  });

  it("color = bg-[#FFC107] when 33 <= pct < 66 (used=6, total=10 → 40%)", () => {
    render(<HoursBar used={6} total={10} />);
    expect(screen.getByTestId("hours-bar-fill")).toHaveClass("bg-[#FFC107]");
  });

  it("color = bg-k-border when pct < 33 (used=8, total=10 → 20%)", () => {
    render(<HoursBar used={8} total={10} />);
    expect(screen.getByTestId("hours-bar-fill")).toHaveClass("bg-k-border");
  });

  it("color = bg-k-border when total=0 (pct=0)", () => {
    render(<HoursBar used={0} total={0} />);
    expect(screen.getByTestId("hours-bar-fill")).toHaveClass("bg-k-border");
  });

  it("renders bar outer track with fixed dimensions and bg", () => {
    render(<HoursBar used={3} total={10} />);
    const track = screen.getByTestId("hours-bar-track");
    expect(track).toHaveClass("w-20", "h-1", "bg-k-line", "rounded-full", "overflow-hidden");
  });

  it("label uses mono font + subtle color", () => {
    render(<HoursBar used={3} total={10} />);
    const label = screen.getByText("3/10h");
    expect(label).toHaveClass("font-[var(--font-mono)]", "text-[11px]", "text-k-subtle");
  });
});
