import React from "react";
import { render, screen } from "@testing-library/react";
import { Select } from "../Select";

describe("Select", () => {
  it("renders a <select> element", () => {
    render(
      <Select aria-label="opt">
        <option value="a">A</option>
      </Select>
    );
    expect(screen.getByRole("combobox").tagName).toBe("SELECT");
  });

  it("renders option children", () => {
    render(
      <Select aria-label="opt">
        <option value="a">A</option>
        <option value="b">B</option>
      </Select>
    );
    expect(screen.getByRole("option", { name: "A" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "B" })).toBeInTheDocument();
  });

  it("applies base classes", () => {
    render(
      <Select aria-label="opt">
        <option value="a">A</option>
      </Select>
    );
    const el = screen.getByRole("combobox");
    expect(el).toHaveClass(
      "bg-k-surface",
      "border",
      "border-k-border",
      "rounded-k-sm",
      "px-3",
      "py-2",
      "text-sm",
      "w-full",
      "focus:border-k-volt",
      "focus:outline-none"
    );
  });

  it("renders the label when provided", () => {
    render(
      <Select label="Country" aria-label="opt">
        <option value="a">A</option>
      </Select>
    );
    expect(screen.getByText("Country")).toBeInTheDocument();
  });

  it("associates label with select via htmlFor/id", () => {
    render(
      <Select label="Country" aria-label="opt">
        <option value="a">A</option>
      </Select>
    );
    const label = screen.getByText("Country");
    const select = screen.getByRole("combobox");
    expect(label).toHaveAttribute("for", select.id);
    expect(select.id).toBeTruthy();
  });

  it("does not render a label element when label prop is omitted", () => {
    const { container } = render(
      <Select aria-label="opt">
        <option value="a">A</option>
      </Select>
    );
    expect(container.querySelector("label")).toBeNull();
  });

  it("renders hint when no error", () => {
    render(
      <Select aria-label="opt" hint="Pick one">
        <option value="a">A</option>
      </Select>
    );
    expect(screen.getByText("Pick one")).toBeInTheDocument();
  });

  it("renders error and applies danger border", () => {
    render(
      <Select aria-label="opt" error="Required">
        <option value="a">A</option>
      </Select>
    );
    expect(screen.getByText("Required")).toBeInTheDocument();
    expect(screen.getByRole("combobox")).toHaveClass("border-k-danger-text");
  });

  it("renders error and suppresses hint when both provided", () => {
    render(
      <Select aria-label="opt" hint="Hint" error="Err">
        <option value="a">A</option>
      </Select>
    );
    expect(screen.getByText("Err")).toBeInTheDocument();
    expect(screen.queryByText("Hint")).toBeNull();
  });

  it("forwards ref to the underlying select", () => {
    const ref = React.createRef<HTMLSelectElement>();
    render(
      <Select ref={ref} aria-label="opt">
        <option value="a">A</option>
      </Select>
    );
    expect(ref.current).toBeInstanceOf(HTMLSelectElement);
  });
});
