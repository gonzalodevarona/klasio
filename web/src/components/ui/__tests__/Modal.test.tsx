import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { Modal } from "../Modal";

function buildProps(overrides: Partial<React.ComponentProps<typeof Modal>> = {}) {
  return {
    open: true,
    onClose: jest.fn(),
    title: "My Title",
    children: <div>Body content</div>,
    ...overrides,
  };
}

describe("Modal", () => {
  it("renders nothing when open=false", () => {
    const { container } = render(<Modal {...buildProps({ open: false })} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("renders the title and body when open=true", () => {
    render(<Modal {...buildProps()} />);
    expect(screen.getByText("My Title")).toBeInTheDocument();
    expect(screen.getByText("Body content")).toBeInTheDocument();
  });

  it("renders an overlay with the dark + blur classes", () => {
    render(<Modal {...buildProps()} />);
    const overlay = screen.getByTestId("modal-overlay");
    expect(overlay).toHaveClass(
      "fixed", "inset-0", "z-50", "bg-k-dark/55", "backdrop-blur-sm",
      "flex", "items-center", "justify-center",
    );
  });

  it("calls onClose when overlay clicked", () => {
    const onClose = jest.fn();
    render(<Modal {...buildProps({ onClose })} />);
    fireEvent.click(screen.getByTestId("modal-overlay"));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("does not call onClose when panel clicked", () => {
    const onClose = jest.fn();
    render(<Modal {...buildProps({ onClose })} />);
    fireEvent.click(screen.getByTestId("modal-panel"));
    expect(onClose).not.toHaveBeenCalled();
  });

  it("calls onClose when Escape pressed", () => {
    const onClose = jest.fn();
    render(<Modal {...buildProps({ onClose })} />);
    fireEvent.keyDown(window, { key: "Escape" });
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("does not call onClose for other keys", () => {
    const onClose = jest.fn();
    render(<Modal {...buildProps({ onClose })} />);
    fireEvent.keyDown(window, { key: "Enter" });
    expect(onClose).not.toHaveBeenCalled();
  });

  it("does not listen for Escape when open=false", () => {
    const onClose = jest.fn();
    render(<Modal {...buildProps({ open: false, onClose })} />);
    fireEvent.keyDown(window, { key: "Escape" });
    expect(onClose).not.toHaveBeenCalled();
  });

  const sizes: Array<{ size: "sm" | "md" | "lg" | "xl"; expected: string }> = [
    { size: "sm", expected: "max-w-[400px]" },
    { size: "md", expected: "max-w-[480px]" },
    { size: "lg", expected: "max-w-[600px]" },
    { size: "xl", expected: "max-w-[800px]" },
  ];

  sizes.forEach(({ size, expected }) => {
    it(`size="${size}" applies ${expected}`, () => {
      render(<Modal {...buildProps({ size })} />);
      expect(screen.getByTestId("modal-panel")).toHaveClass(expected);
    });
  });

  it("default size=md applies max-w-[480px]", () => {
    render(<Modal {...buildProps()} />);
    expect(screen.getByTestId("modal-panel")).toHaveClass("max-w-[480px]");
  });

  it("merges caller className on the panel", () => {
    render(<Modal {...buildProps({ className: "custom-class" })} />);
    expect(screen.getByTestId("modal-panel")).toHaveClass("custom-class");
  });

  it("renders close button that calls onClose", () => {
    const onClose = jest.fn();
    render(<Modal {...buildProps({ onClose })} />);
    fireEvent.click(screen.getByTestId("modal-close"));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("removes Escape listener on unmount", () => {
    const onClose = jest.fn();
    const { unmount } = render(<Modal {...buildProps({ onClose })} />);
    unmount();
    fireEvent.keyDown(window, { key: "Escape" });
    expect(onClose).not.toHaveBeenCalled();
  });
});
