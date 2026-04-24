import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { Table, Thead, Th, Tr, Td } from "../Table";

describe("Table", () => {
  it("renders a wrapper div with overflow + border classes and an inner table", () => {
    render(
      <Table data-testid="wrap">
        <thead>
          <tr>
            <th>H</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>D</td>
          </tr>
        </tbody>
      </Table>,
    );
    const wrap = screen.getByTestId("wrap");
    expect(wrap.tagName).toBe("DIV");
    expect(wrap).toHaveClass("overflow-x-auto", "rounded-k-md", "border", "border-k-border", "w-full");
    const table = wrap.querySelector("table");
    expect(table).not.toBeNull();
    expect(table).toHaveClass("w-full");
  });

  it("merges className on the wrapper", () => {
    render(
      <Table className="custom-class" data-testid="wrap">
        <thead />
      </Table>,
    );
    expect(screen.getByTestId("wrap")).toHaveClass("custom-class");
  });
});

describe("Thead", () => {
  it("applies bg + border classes", () => {
    render(
      <table>
        <Thead data-testid="thead">
          <tr>
            <th>H</th>
          </tr>
        </Thead>
      </table>,
    );
    expect(screen.getByTestId("thead")).toHaveClass("bg-k-bg", "border-b", "border-k-border");
  });
});

describe("Th", () => {
  it("applies typography classes", () => {
    render(
      <table>
        <thead>
          <tr>
            <Th>Name</Th>
          </tr>
        </thead>
      </table>,
    );
    const el = screen.getByText("Name");
    expect(el.tagName).toBe("TH");
    expect(el).toHaveClass(
      "font-[var(--font-mono)]",
      "text-[10px]",
      "uppercase",
      "tracking-[0.1em]",
      "text-k-muted",
      "px-4",
      "py-2.5",
    );
  });

  it("right=true adds text-right", () => {
    render(
      <table>
        <thead>
          <tr>
            <Th right>Hours</Th>
          </tr>
        </thead>
      </table>,
    );
    expect(screen.getByText("Hours")).toHaveClass("text-right");
  });

  it("right=false omits text-right", () => {
    render(
      <table>
        <thead>
          <tr>
            <Th>Name</Th>
          </tr>
        </thead>
      </table>,
    );
    expect(screen.getByText("Name")).not.toHaveClass("text-right");
  });
});

describe("Tr", () => {
  it("applies base border class", () => {
    render(
      <table>
        <tbody>
          <Tr>
            <td>X</td>
          </Tr>
        </tbody>
      </table>,
    );
    expect(screen.getByText("X").parentElement).toHaveClass("border-b", "border-k-line");
  });

  it("onClick adds hover + cursor classes and fires", () => {
    const onClick = jest.fn();
    render(
      <table>
        <tbody>
          <Tr onClick={onClick}>
            <td>X</td>
          </Tr>
        </tbody>
      </table>,
    );
    const tr = screen.getByText("X").parentElement!;
    expect(tr).toHaveClass("hover:bg-k-surface", "cursor-pointer");
    fireEvent.click(tr);
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it("active=true adds active bg class", () => {
    render(
      <table>
        <tbody>
          <Tr active>
            <td>X</td>
          </Tr>
        </tbody>
      </table>,
    );
    expect(screen.getByText("X").parentElement).toHaveClass("bg-[#F9FFEA]");
  });
});

describe("Td", () => {
  it("applies base classes", () => {
    render(
      <table>
        <tbody>
          <tr>
            <Td>X</Td>
          </tr>
        </tbody>
      </table>,
    );
    const el = screen.getByText("X");
    expect(el.tagName).toBe("TD");
    expect(el).toHaveClass("px-4", "py-3", "text-sm", "whitespace-nowrap");
  });

  it("mono adds mono font class", () => {
    render(
      <table>
        <tbody>
          <tr>
            <Td mono>X</Td>
          </tr>
        </tbody>
      </table>,
    );
    expect(screen.getByText("X")).toHaveClass("font-[var(--font-mono)]");
  });

  it("muted adds text-k-muted", () => {
    render(
      <table>
        <tbody>
          <tr>
            <Td muted>X</Td>
          </tr>
        </tbody>
      </table>,
    );
    expect(screen.getByText("X")).toHaveClass("text-k-muted");
  });

  it("bold adds font-semibold", () => {
    render(
      <table>
        <tbody>
          <tr>
            <Td bold>X</Td>
          </tr>
        </tbody>
      </table>,
    );
    expect(screen.getByText("X")).toHaveClass("font-semibold");
  });

  it("right adds text-right", () => {
    render(
      <table>
        <tbody>
          <tr>
            <Td right>X</Td>
          </tr>
        </tbody>
      </table>,
    );
    expect(screen.getByText("X")).toHaveClass("text-right");
  });
});
