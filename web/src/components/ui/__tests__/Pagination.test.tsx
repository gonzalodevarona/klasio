import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { Pagination } from "../Pagination";

function buildProps(overrides: Partial<React.ComponentProps<typeof Pagination>> = {}) {
  return {
    page: 0,
    totalPages: 5,
    total: 42,
    onPrev: jest.fn(),
    onNext: jest.fn(),
    labelPrev: "Previous",
    labelNext: "Next",
    labelFormat: (p: number, tp: number, t: number) => `Page ${p + 1} of ${tp} (${t} items)`,
    ...overrides,
  };
}

describe("Pagination", () => {
  it("renders the formatted label", () => {
    render(<Pagination {...buildProps()} />);
    expect(screen.getByText("Page 1 of 5 (42 items)")).toBeInTheDocument();
  });

  it("renders prev and next buttons with provided labels", () => {
    render(<Pagination {...buildProps()} />);
    expect(screen.getByRole("button", { name: "Previous" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Next" })).toBeInTheDocument();
  });

  it("disables prev when page === 0", () => {
    render(<Pagination {...buildProps({ page: 0 })} />);
    expect(screen.getByRole("button", { name: "Previous" })).toBeDisabled();
  });

  it("enables prev when page > 0", () => {
    render(<Pagination {...buildProps({ page: 1 })} />);
    expect(screen.getByRole("button", { name: "Previous" })).not.toBeDisabled();
  });

  it("disables next when page === totalPages - 1", () => {
    render(<Pagination {...buildProps({ page: 4, totalPages: 5 })} />);
    expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();
  });

  it("enables next when page < totalPages - 1", () => {
    render(<Pagination {...buildProps({ page: 2, totalPages: 5 })} />);
    expect(screen.getByRole("button", { name: "Next" })).not.toBeDisabled();
  });

  it("calls onPrev when prev clicked", () => {
    const onPrev = jest.fn();
    render(<Pagination {...buildProps({ page: 1, onPrev })} />);
    fireEvent.click(screen.getByRole("button", { name: "Previous" }));
    expect(onPrev).toHaveBeenCalledTimes(1);
  });

  it("calls onNext when next clicked", () => {
    const onNext = jest.fn();
    render(<Pagination {...buildProps({ page: 1, onNext })} />);
    fireEvent.click(screen.getByRole("button", { name: "Next" }));
    expect(onNext).toHaveBeenCalledTimes(1);
  });

  it("disables both when totalPages === 1", () => {
    render(<Pagination {...buildProps({ page: 0, totalPages: 1 })} />);
    expect(screen.getByRole("button", { name: "Previous" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();
  });
});
