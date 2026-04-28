import { screen } from "@testing-library/react";
import { renderWithIntl as render } from "../../__test-support__/renderWithIntl";
import ClassLevelBadge from "@/components/classes/ClassLevelBadge";

describe("ClassLevelBadge", () => {
  it('renders "Beginner" for BEGINNER level', () => {
    render(<ClassLevelBadge level="BEGINNER" />);
    expect(screen.getByText("Beginner")).toBeInTheDocument();
  });

  it('renders "Intermediate" for INTERMEDIATE level', () => {
    render(<ClassLevelBadge level="INTERMEDIATE" />);
    expect(screen.getByText("Intermediate")).toBeInTheDocument();
  });

  it('renders "Advanced" for ADVANCED level', () => {
    render(<ClassLevelBadge level="ADVANCED" />);
    expect(screen.getByText("Advanced")).toBeInTheDocument();
  });

  it('renders "Open" for OPEN level', () => {
    render(<ClassLevelBadge level="OPEN" />);
    expect(screen.getByText("Open")).toBeInTheDocument();
  });
});
