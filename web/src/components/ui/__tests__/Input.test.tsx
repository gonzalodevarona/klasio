import React from "react";
import { render, screen } from "@testing-library/react";
import { Input } from "../Input";

describe("Input", () => {
  it("renders an <input> element", () => {
    render(<Input placeholder="Name" />);
    expect(screen.getByPlaceholderText("Name").tagName).toBe("INPUT");
  });

  it("applies base classes to the input", () => {
    render(<Input placeholder="X" />);
    const input = screen.getByPlaceholderText("X");
    expect(input).toHaveClass(
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
    render(<Input label="Email" placeholder="X" />);
    expect(screen.getByText("Email")).toBeInTheDocument();
  });

  it("does not render a label element when label prop is omitted", () => {
    const { container } = render(<Input placeholder="X" />);
    expect(container.querySelector("label")).toBeNull();
  });

  it("renders the hint when provided and no error", () => {
    render(<Input placeholder="X" hint="Helpful text" />);
    expect(screen.getByText("Helpful text")).toBeInTheDocument();
  });

  it("renders the error and adds danger border class", () => {
    render(<Input placeholder="X" error="Required" />);
    expect(screen.getByText("Required")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("X")).toHaveClass("border-k-danger-text");
  });

  it("renders error instead of hint when both are provided", () => {
    render(<Input placeholder="X" hint="Hint" error="Err" />);
    expect(screen.getByText("Err")).toBeInTheDocument();
    expect(screen.queryByText("Hint")).toBeNull();
  });

  it("renders the leftIcon and adds pl-9 to the input", () => {
    render(<Input placeholder="X" leftIcon={<svg data-testid="icon" />} />);
    expect(screen.getByTestId("icon")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("X")).toHaveClass("pl-9");
  });

  it("does not add pl-9 when no leftIcon", () => {
    render(<Input placeholder="X" />);
    expect(screen.getByPlaceholderText("X")).not.toHaveClass("pl-9");
  });

  it("forwards ref to the underlying input", () => {
    const ref = React.createRef<HTMLInputElement>();
    render(<Input ref={ref} placeholder="X" />);
    expect(ref.current).toBeInstanceOf(HTMLInputElement);
  });

  it("forwards arbitrary input props (type, value, onChange)", () => {
    const onChange = jest.fn();
    render(
      <Input
        type="email"
        value="a@b.c"
        onChange={onChange}
        placeholder="X"
      />
    );
    const input = screen.getByPlaceholderText("X") as HTMLInputElement;
    expect(input.type).toBe("email");
    expect(input.value).toBe("a@b.c");
  });
});
