# UI Primitives Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build 10 reusable, text-agnostic UI primitives under `web/src/components/ui/` plus a `cn()` class-merge utility, fully test-driven, with one commit per primitive on the existing `feature/full-redesign` branch.

**Architecture:** Headless-style presentational components that encode the Klasio design tokens (`k-*` colors, `k-*` radii, `k-modal` shadow, `duration-k` transitions) already loaded in `tailwind.config.ts`. All primitives accept text via props (no internal `next-intl` calls). Only `Modal` is a `'use client'` component (Escape-key listener); the rest stay server-component-friendly. `Button` uses `@radix-ui/react-slot` for `asChild` polymorphism. Class composition uses `clsx` + `tailwind-merge` through a single `cn()` helper at `web/src/lib/utils.ts`.

**Tech Stack:** React 19, Next.js 15, TypeScript 5.9, Tailwind 3.4, Jest 29, Testing Library 16, `@radix-ui/react-slot`, `clsx`, `tailwind-merge`.

**Test layout:** Project convention places tests under `__tests__/` directories (per `jest.config.ts` `testMatch: ["**/__tests__/**/*.{ts,tsx}"]`). Tests for primitives go in `web/src/components/ui/__tests__/<Component>.test.tsx`. The `cn` test goes in `web/src/lib/__tests__/utils.test.ts`. The spec's mention of "co-located `<Component>.test.tsx`" is overridden by Jest config — tests must live inside `__tests__/`.

**Commit rules:** Conventional Commits, lowercase imperative description, no Co-Authored-By footer (per global user instructions). Each task ends with one commit. All commands run from `/Users/gonzalodevarona/Documents/klasio/web` unless noted.

**Per-task verification gate (run before every commit):**
```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npx tsc --noEmit
npm test -- <test-file-path>
```
Both must pass. Do not commit if either fails.

---

## File Map

| Path | Created in task |
|---|---|
| `web/src/lib/utils.ts` | 1 |
| `web/src/lib/__tests__/utils.test.ts` | 1 |
| `web/src/components/ui/Badge.tsx` | 2 |
| `web/src/components/ui/__tests__/Badge.test.tsx` | 2 |
| `web/src/components/ui/Button.tsx` | 3 |
| `web/src/components/ui/__tests__/Button.test.tsx` | 3 |
| `web/src/components/ui/Input.tsx` | 4 |
| `web/src/components/ui/__tests__/Input.test.tsx` | 4 |
| `web/src/components/ui/Select.tsx` | 5 |
| `web/src/components/ui/__tests__/Select.test.tsx` | 5 |
| `web/src/components/ui/Card.tsx` | 6 |
| `web/src/components/ui/__tests__/Card.test.tsx` | 6 |
| `web/src/components/ui/StatCard.tsx` | 7 |
| `web/src/components/ui/__tests__/StatCard.test.tsx` | 7 |
| `web/src/components/ui/Table.tsx` | 8 |
| `web/src/components/ui/__tests__/Table.test.tsx` | 8 |
| `web/src/components/ui/Pagination.tsx` | 9 |
| `web/src/components/ui/__tests__/Pagination.test.tsx` | 9 |
| `web/src/components/ui/Modal.tsx` | 10 |
| `web/src/components/ui/__tests__/Modal.test.tsx` | 10 |
| `web/src/components/ui/HoursBar.tsx` | 11 |
| `web/src/components/ui/__tests__/HoursBar.test.tsx` | 11 |
| `web/src/components/ui/index.ts` | 12 |
| `web/package.json` | 1 (deps added) |

No existing files outside `web/package.json` and `web/package-lock.json` are modified.

---

## Task 1: Install dependencies and add `cn()` utility

**Files:**
- Modify: `web/package.json` (deps section)
- Create: `web/src/lib/utils.ts`
- Create: `web/src/lib/__tests__/utils.test.ts`

- [ ] **Step 1: Install runtime deps**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npm install clsx@^2 tailwind-merge@^2 @radix-ui/react-slot@^1
```

Expected: `package.json` `dependencies` gains `clsx`, `tailwind-merge`, `@radix-ui/react-slot`. `package-lock.json` updates. No errors.

- [ ] **Step 2: Write failing test for `cn()`**

Create `web/src/lib/__tests__/utils.test.ts`:

```ts
import { cn } from "../utils";

describe("cn", () => {
  it("joins multiple class strings", () => {
    expect(cn("a", "b", "c")).toBe("a b c");
  });

  it("filters falsy values", () => {
    expect(cn("a", false && "b", null, undefined, "c")).toBe("a c");
  });

  it("merges conflicting Tailwind utilities (last wins)", () => {
    expect(cn("px-2", "px-4")).toBe("px-4");
  });

  it("preserves non-conflicting Tailwind utilities", () => {
    expect(cn("px-2", "py-4")).toBe("px-2 py-4");
  });

  it("supports conditional object syntax via clsx", () => {
    expect(cn({ "text-red-500": true, "text-blue-500": false })).toBe("text-red-500");
  });
});
```

- [ ] **Step 3: Run the test and confirm failure**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npm test -- src/lib/__tests__/utils.test.ts
```

Expected: FAIL with "Cannot find module '../utils'".

- [ ] **Step 4: Implement `cn()`**

Create `web/src/lib/utils.ts`:

```ts
import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}
```

- [ ] **Step 5: Run the test and confirm pass**

```bash
npm test -- src/lib/__tests__/utils.test.ts
```

Expected: 5 tests pass.

- [ ] **Step 6: Run typecheck gate**

```bash
npx tsc --noEmit
```

Expected: zero errors.

- [ ] **Step 7: Commit**

```bash
cd /Users/gonzalodevarona/Documents/klasio
git add web/package.json web/package-lock.json web/src/lib/utils.ts web/src/lib/__tests__/utils.test.ts
git commit -m "chore(design-system): install clsx tailwind-merge radix-slot and add cn utility"
```

---

## Task 2: `Badge` primitive

**Files:**
- Create: `web/src/components/ui/Badge.tsx`
- Create: `web/src/components/ui/__tests__/Badge.test.tsx`

- [ ] **Step 1: Write failing tests**

Create `web/src/components/ui/__tests__/Badge.test.tsx`:

```tsx
import React from "react";
import { render, screen } from "@testing-library/react";
import { Badge } from "../Badge";

describe("Badge", () => {
  it("renders the label as text", () => {
    render(<Badge variant="active" label="Active" />);
    expect(screen.getByText("Active")).toBeInTheDocument();
  });

  it("applies base pill classes", () => {
    render(<Badge variant="active" label="Active" />);
    const el = screen.getByText("Active");
    expect(el).toHaveClass("rounded-full");
    expect(el).toHaveClass("font-semibold");
    expect(el).toHaveClass("inline-flex");
    expect(el).toHaveClass("items-center");
  });

  it("applies default size classes", () => {
    render(<Badge variant="active" label="Active" />);
    const el = screen.getByText("Active");
    expect(el).toHaveClass("text-[11px]");
    expect(el).toHaveClass("px-2.5");
    expect(el).toHaveClass("py-0.5");
  });

  it("applies small size classes when small=true", () => {
    render(<Badge variant="active" label="Active" small />);
    const el = screen.getByText("Active");
    expect(el).toHaveClass("text-[10px]");
    expect(el).toHaveClass("px-2");
    expect(el).toHaveClass("py-px");
  });

  const cases: Array<{
    variant:
      | "active" | "expiring" | "inactive"
      | "pending" | "approved" | "rejected"
      | "beginner" | "intermediate" | "advanced";
    expected: string[];
  }> = [
    { variant: "active",       expected: ["bg-k-volt", "text-k-volt-text"] },
    { variant: "expiring",     expected: ["bg-k-warn-bg", "text-k-warn-text"] },
    { variant: "inactive",     expected: ["bg-k-bg", "text-k-subtle", "border", "border-k-border"] },
    { variant: "pending",      expected: ["bg-k-warn-bg", "text-k-warn-text"] },
    { variant: "approved",     expected: ["bg-k-volt", "text-k-volt-text"] },
    { variant: "rejected",     expected: ["bg-k-danger-bg", "text-k-danger-text"] },
    { variant: "beginner",     expected: ["bg-k-info-bg", "text-k-info-text"] },
    { variant: "intermediate", expected: ["bg-k-warn-bg", "text-k-warn-text"] },
    { variant: "advanced",     expected: ["bg-k-volt", "text-k-volt-text"] },
  ];

  cases.forEach(({ variant, expected }) => {
    it(`variant="${variant}" applies correct color classes`, () => {
      render(<Badge variant={variant} label="X" />);
      const el = screen.getByText("X");
      expected.forEach((cls) => expect(el).toHaveClass(cls));
    });
  });

  it("merges caller className", () => {
    render(<Badge variant="active" label="X" className="custom-class" />);
    expect(screen.getByText("X")).toHaveClass("custom-class");
  });
});
```

- [ ] **Step 2: Run tests and confirm failure**

```bash
npm test -- src/components/ui/__tests__/Badge.test.tsx
```

Expected: FAIL with "Cannot find module '../Badge'".

- [ ] **Step 3: Implement `Badge`**

Create `web/src/components/ui/Badge.tsx`:

```tsx
import React from "react";
import { cn } from "@/lib/utils";

export type BadgeVariant =
  | "active" | "expiring" | "inactive"
  | "pending" | "approved" | "rejected"
  | "beginner" | "intermediate" | "advanced";

export interface BadgeProps {
  variant: BadgeVariant;
  label: string;
  small?: boolean;
  className?: string;
}

const VARIANT_CLASSES: Record<BadgeVariant, string> = {
  active:       "bg-k-volt text-k-volt-text",
  expiring:     "bg-k-warn-bg text-k-warn-text",
  inactive:     "bg-k-bg text-k-subtle border border-k-border",
  pending:      "bg-k-warn-bg text-k-warn-text",
  approved:     "bg-k-volt text-k-volt-text",
  rejected:     "bg-k-danger-bg text-k-danger-text",
  beginner:     "bg-k-info-bg text-k-info-text",
  intermediate: "bg-k-warn-bg text-k-warn-text",
  advanced:     "bg-k-volt text-k-volt-text",
};

export function Badge({ variant, label, small, className }: BadgeProps) {
  return (
    <span
      className={cn(
        "rounded-full font-semibold inline-flex items-center",
        small ? "text-[10px] px-2 py-px" : "text-[11px] px-2.5 py-0.5",
        VARIANT_CLASSES[variant],
        className,
      )}
    >
      {label}
    </span>
  );
}
```

- [ ] **Step 4: Run tests and confirm pass**

```bash
npm test -- src/components/ui/__tests__/Badge.test.tsx
```

Expected: all tests pass (15 cases).

- [ ] **Step 5: Typecheck gate**

```bash
npx tsc --noEmit
```

Expected: zero errors.

- [ ] **Step 6: Commit**

```bash
cd /Users/gonzalodevarona/Documents/klasio
git add web/src/components/ui/Badge.tsx web/src/components/ui/__tests__/Badge.test.tsx
git commit -m "feat(ui): add Badge primitive"
```

---

## Task 3: `Button` primitive (with `asChild`)

**Files:**
- Create: `web/src/components/ui/Button.tsx`
- Create: `web/src/components/ui/__tests__/Button.test.tsx`

- [ ] **Step 1: Write failing tests**

Create `web/src/components/ui/__tests__/Button.test.tsx`:

```tsx
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
```

- [ ] **Step 2: Run tests and confirm failure**

```bash
npm test -- src/components/ui/__tests__/Button.test.tsx
```

Expected: FAIL with "Cannot find module '../Button'".

- [ ] **Step 3: Implement `Button`**

Create `web/src/components/ui/Button.tsx`:

```tsx
import React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cn } from "@/lib/utils";

export type ButtonVariant = "primary" | "volt" | "outline" | "ghost" | "danger";
export type ButtonSize = "sm" | "md" | "icon";

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant: ButtonVariant;
  size?: ButtonSize;
  asChild?: boolean;
}

const VARIANT_CLASSES: Record<ButtonVariant, string> = {
  primary: "bg-k-dark text-white hover:bg-[#1A1A1A]",
  volt:    "bg-k-volt text-k-dark hover:bg-[#B8EE3A]",
  outline: "bg-transparent text-k-dark border border-k-border hover:bg-k-surface",
  ghost:   "bg-k-bg text-k-subtle hover:bg-k-border",
  danger:  "bg-k-danger-bg text-k-danger-text border border-k-danger-text/30",
};

const SIZE_CLASSES: Record<ButtonSize, string> = {
  sm:   "text-xs px-3 py-1.5 rounded-k-sm",
  md:   "text-sm font-semibold px-4 py-2 rounded-k-sm",
  icon: "h-8 w-8 p-0 rounded-k-sm inline-flex items-center justify-center",
};

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ variant, size = "md", asChild = false, className, children, ...rest }, ref) => {
    const classes = cn(
      "inline-flex items-center justify-center transition duration-k disabled:opacity-40 disabled:cursor-not-allowed",
      SIZE_CLASSES[size],
      VARIANT_CLASSES[variant],
      className,
    );

    if (asChild) {
      return (
        <Slot className={classes} {...rest}>
          {children}
        </Slot>
      );
    }

    return (
      <button ref={ref} className={classes} {...rest}>
        {children}
      </button>
    );
  },
);

Button.displayName = "Button";
```

- [ ] **Step 4: Run tests and confirm pass**

```bash
npm test -- src/components/ui/__tests__/Button.test.tsx
```

Expected: all tests pass.

- [ ] **Step 5: Typecheck gate**

```bash
npx tsc --noEmit
```

Expected: zero errors.

- [ ] **Step 6: Commit**

```bash
cd /Users/gonzalodevarona/Documents/klasio
git add web/src/components/ui/Button.tsx web/src/components/ui/__tests__/Button.test.tsx
git commit -m "feat(ui): add Button primitive with asChild support"
```

---

## Task 4: `Input` primitive

**Files:**
- Create: `web/src/components/ui/Input.tsx`
- Create: `web/src/components/ui/__tests__/Input.test.tsx`

- [ ] **Step 1: Write failing tests**

Create `web/src/components/ui/__tests__/Input.test.tsx`:

```tsx
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
      "bg-k-surface", "border", "border-k-border", "rounded-k-sm",
      "px-3", "py-2", "text-sm", "w-full", "focus:border-k-volt", "focus:outline-none",
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
    render(<Input type="email" value="a@b.c" onChange={onChange} placeholder="X" />);
    const input = screen.getByPlaceholderText("X") as HTMLInputElement;
    expect(input.type).toBe("email");
    expect(input.value).toBe("a@b.c");
  });
});
```

- [ ] **Step 2: Run tests and confirm failure**

```bash
npm test -- src/components/ui/__tests__/Input.test.tsx
```

Expected: FAIL with "Cannot find module '../Input'".

- [ ] **Step 3: Implement `Input`**

Create `web/src/components/ui/Input.tsx`:

```tsx
import React from "react";
import { cn } from "@/lib/utils";

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
  leftIcon?: React.ReactNode;
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, hint, leftIcon, className, ...rest }, ref) => {
    return (
      <div>
        {label && (
          <label className="text-xs font-medium text-k-subtle mb-1 font-[var(--font-mono)] uppercase tracking-wide block">
            {label}
          </label>
        )}
        <div className="relative">
          {leftIcon && (
            <span className="absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none flex items-center">
              {leftIcon}
            </span>
          )}
          <input
            ref={ref}
            className={cn(
              "bg-k-surface border border-k-border rounded-k-sm px-3 py-2 text-sm font-[var(--font-main)] w-full focus:border-k-volt focus:outline-none",
              error && "border-k-danger-text",
              leftIcon && "pl-9",
              className,
            )}
            {...rest}
          />
        </div>
        {error ? (
          <p className="text-xs text-k-danger-text mt-1">{error}</p>
        ) : hint ? (
          <p className="text-xs text-k-muted mt-1">{hint}</p>
        ) : null}
      </div>
    );
  },
);

Input.displayName = "Input";
```

- [ ] **Step 4: Run tests and confirm pass**

```bash
npm test -- src/components/ui/__tests__/Input.test.tsx
```

Expected: all tests pass.

- [ ] **Step 5: Typecheck gate**

```bash
npx tsc --noEmit
```

Expected: zero errors.

- [ ] **Step 6: Commit**

```bash
cd /Users/gonzalodevarona/Documents/klasio
git add web/src/components/ui/Input.tsx web/src/components/ui/__tests__/Input.test.tsx
git commit -m "feat(ui): add Input primitive"
```

---

## Task 5: `Select` primitive

**Files:**
- Create: `web/src/components/ui/Select.tsx`
- Create: `web/src/components/ui/__tests__/Select.test.tsx`

- [ ] **Step 1: Write failing tests**

Create `web/src/components/ui/__tests__/Select.test.tsx`:

```tsx
import React from "react";
import { render, screen } from "@testing-library/react";
import { Select } from "../Select";

describe("Select", () => {
  it("renders a <select> element", () => {
    render(
      <Select aria-label="opt">
        <option value="a">A</option>
      </Select>,
    );
    expect(screen.getByRole("combobox").tagName).toBe("SELECT");
  });

  it("renders option children", () => {
    render(
      <Select aria-label="opt">
        <option value="a">A</option>
        <option value="b">B</option>
      </Select>,
    );
    expect(screen.getByRole("option", { name: "A" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "B" })).toBeInTheDocument();
  });

  it("applies base classes", () => {
    render(<Select aria-label="opt"><option value="a">A</option></Select>);
    const el = screen.getByRole("combobox");
    expect(el).toHaveClass(
      "bg-k-surface", "border", "border-k-border", "rounded-k-sm",
      "px-3", "py-2", "text-sm", "w-full", "focus:border-k-volt", "focus:outline-none",
    );
  });

  it("renders the label when provided", () => {
    render(<Select label="Country" aria-label="opt"><option value="a">A</option></Select>);
    expect(screen.getByText("Country")).toBeInTheDocument();
  });

  it("renders hint when no error", () => {
    render(<Select aria-label="opt" hint="Pick one"><option value="a">A</option></Select>);
    expect(screen.getByText("Pick one")).toBeInTheDocument();
  });

  it("renders error and applies danger border", () => {
    render(<Select aria-label="opt" error="Required"><option value="a">A</option></Select>);
    expect(screen.getByText("Required")).toBeInTheDocument();
    expect(screen.getByRole("combobox")).toHaveClass("border-k-danger-text");
  });

  it("renders error and suppresses hint when both provided", () => {
    render(
      <Select aria-label="opt" hint="Hint" error="Err"><option value="a">A</option></Select>,
    );
    expect(screen.getByText("Err")).toBeInTheDocument();
    expect(screen.queryByText("Hint")).toBeNull();
  });

  it("forwards ref to the underlying select", () => {
    const ref = React.createRef<HTMLSelectElement>();
    render(<Select ref={ref} aria-label="opt"><option value="a">A</option></Select>);
    expect(ref.current).toBeInstanceOf(HTMLSelectElement);
  });
});
```

- [ ] **Step 2: Run tests and confirm failure**

```bash
npm test -- src/components/ui/__tests__/Select.test.tsx
```

Expected: FAIL with "Cannot find module '../Select'".

- [ ] **Step 3: Implement `Select`**

Create `web/src/components/ui/Select.tsx`:

```tsx
import React from "react";
import { cn } from "@/lib/utils";

export interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
  hint?: string;
}

export const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, error, hint, className, children, ...rest }, ref) => {
    return (
      <div>
        {label && (
          <label className="text-xs font-medium text-k-subtle mb-1 font-[var(--font-mono)] uppercase tracking-wide block">
            {label}
          </label>
        )}
        <select
          ref={ref}
          className={cn(
            "bg-k-surface border border-k-border rounded-k-sm px-3 py-2 text-sm font-[var(--font-main)] w-full focus:border-k-volt focus:outline-none",
            error && "border-k-danger-text",
            className,
          )}
          {...rest}
        >
          {children}
        </select>
        {error ? (
          <p className="text-xs text-k-danger-text mt-1">{error}</p>
        ) : hint ? (
          <p className="text-xs text-k-muted mt-1">{hint}</p>
        ) : null}
      </div>
    );
  },
);

Select.displayName = "Select";
```

- [ ] **Step 4: Run tests and confirm pass**

```bash
npm test -- src/components/ui/__tests__/Select.test.tsx
```

Expected: all tests pass.

- [ ] **Step 5: Typecheck gate**

```bash
npx tsc --noEmit
```

Expected: zero errors.

- [ ] **Step 6: Commit**

```bash
cd /Users/gonzalodevarona/Documents/klasio
git add web/src/components/ui/Select.tsx web/src/components/ui/__tests__/Select.test.tsx
git commit -m "feat(ui): add Select primitive"
```

---

## Task 6: `Card` primitive

**Files:**
- Create: `web/src/components/ui/Card.tsx`
- Create: `web/src/components/ui/__tests__/Card.test.tsx`

- [ ] **Step 1: Write failing tests**

Create `web/src/components/ui/__tests__/Card.test.tsx`:

```tsx
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
    expect(el).toHaveClass("rounded-k-lg", "bg-k-surface", "border-[1.5px]", "border-k-border");
  });

  it("applies dark surface when dark=true", () => {
    render(<Card dark data-testid="card">X</Card>);
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
    render(<Card padding="sm" data-testid="card">X</Card>);
    expect(screen.getByTestId("card")).toHaveClass("p-4");
  });

  it("padding=lg applies p-8", () => {
    render(<Card padding="lg" data-testid="card">X</Card>);
    expect(screen.getByTestId("card")).toHaveClass("p-8");
  });

  it("merges caller className", () => {
    render(<Card className="custom-class" data-testid="card">X</Card>);
    expect(screen.getByTestId("card")).toHaveClass("custom-class");
  });
});
```

- [ ] **Step 2: Run tests and confirm failure**

```bash
npm test -- src/components/ui/__tests__/Card.test.tsx
```

Expected: FAIL with "Cannot find module '../Card'".

- [ ] **Step 3: Implement `Card`**

Create `web/src/components/ui/Card.tsx`:

```tsx
import React from "react";
import { cn } from "@/lib/utils";

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  padding?: "sm" | "md" | "lg";
  dark?: boolean;
  children: React.ReactNode;
}

const PADDING_CLASSES = {
  sm: "p-4",
  md: "p-6",
  lg: "p-8",
} as const;

export function Card({ padding = "md", dark = false, className, children, ...rest }: CardProps) {
  return (
    <div
      className={cn(
        "rounded-k-lg",
        dark ? "bg-k-dark" : "bg-k-surface border-[1.5px] border-k-border",
        PADDING_CLASSES[padding],
        className,
      )}
      {...rest}
    >
      {children}
    </div>
  );
}
```

- [ ] **Step 4: Run tests and confirm pass**

```bash
npm test -- src/components/ui/__tests__/Card.test.tsx
```

Expected: all tests pass.

- [ ] **Step 5: Typecheck gate**

```bash
npx tsc --noEmit
```

Expected: zero errors.

- [ ] **Step 6: Commit**

```bash
cd /Users/gonzalodevarona/Documents/klasio
git add web/src/components/ui/Card.tsx web/src/components/ui/__tests__/Card.test.tsx
git commit -m "feat(ui): add Card primitive"
```

---

## Task 7: `StatCard` primitive

**Files:**
- Create: `web/src/components/ui/StatCard.tsx`
- Create: `web/src/components/ui/__tests__/StatCard.test.tsx`

- [ ] **Step 1: Write failing tests**

Create `web/src/components/ui/__tests__/StatCard.test.tsx`:

```tsx
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

  it("dark theme: value uses text-k-volt and label uses text-[#666]", () => {
    render(<StatCard label="ACTIVE" value={42} dark />);
    expect(screen.getByText("ACTIVE")).toHaveClass("text-[#666]");
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
```

- [ ] **Step 2: Run tests and confirm failure**

```bash
npm test -- src/components/ui/__tests__/StatCard.test.tsx
```

Expected: FAIL with "Cannot find module '../StatCard'".

- [ ] **Step 3: Implement `StatCard`**

Create `web/src/components/ui/StatCard.tsx`:

```tsx
import React from "react";
import { Card } from "./Card";
import { cn } from "@/lib/utils";

export interface StatCardProps {
  label: string;
  value: string | number;
  sub?: string;
  subColor?: string;
  dark?: boolean;
}

export function StatCard({ label, value, sub, subColor, dark = false }: StatCardProps) {
  const labelClass = dark ? "text-[#666]" : "text-k-muted";
  const valueClass = dark ? "text-k-volt" : "text-k-dark";
  const defaultSubClass = dark ? "text-k-volt" : "text-k-muted";

  return (
    <Card padding="md" dark={dark}>
      <div className="flex flex-col gap-1.5">
        <span
          className={cn(
            "font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em]",
            labelClass,
          )}
        >
          {label}
        </span>
        <span
          className={cn(
            "text-[40px] font-extrabold tracking-[-0.03em] leading-none",
            valueClass,
          )}
        >
          {value}
        </span>
        {sub && (
          <span className={cn("text-xs font-medium", subColor ?? defaultSubClass)}>
            {sub}
          </span>
        )}
      </div>
    </Card>
  );
}
```

- [ ] **Step 4: Run tests and confirm pass**

```bash
npm test -- src/components/ui/__tests__/StatCard.test.tsx
```

Expected: all tests pass.

- [ ] **Step 5: Typecheck gate**

```bash
npx tsc --noEmit
```

Expected: zero errors.

- [ ] **Step 6: Commit**

```bash
cd /Users/gonzalodevarona/Documents/klasio
git add web/src/components/ui/StatCard.tsx web/src/components/ui/__tests__/StatCard.test.tsx
git commit -m "feat(ui): add StatCard primitive"
```

---

## Task 8: `Table` compound primitive

**Files:**
- Create: `web/src/components/ui/Table.tsx`
- Create: `web/src/components/ui/__tests__/Table.test.tsx`

- [ ] **Step 1: Write failing tests**

Create `web/src/components/ui/__tests__/Table.test.tsx`:

```tsx
import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { Table, Thead, Th, Tr, Td } from "../Table";

describe("Table", () => {
  it("renders a wrapper div with overflow + border classes and an inner table", () => {
    render(
      <Table data-testid="wrap">
        <thead><tr><th>H</th></tr></thead>
        <tbody><tr><td>D</td></tr></tbody>
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
    render(<Table className="custom-class" data-testid="wrap"><thead /></Table>);
    expect(screen.getByTestId("wrap")).toHaveClass("custom-class");
  });
});

describe("Thead", () => {
  it("applies bg + border classes", () => {
    render(
      <table>
        <Thead data-testid="thead">
          <tr><th>H</th></tr>
        </Thead>
      </table>,
    );
    expect(screen.getByTestId("thead")).toHaveClass("bg-k-bg", "border-b", "border-k-border");
  });
});

describe("Th", () => {
  it("applies typography classes", () => {
    render(
      <table><thead><tr><Th>Name</Th></tr></thead></table>,
    );
    const el = screen.getByText("Name");
    expect(el.tagName).toBe("TH");
    expect(el).toHaveClass(
      "font-[var(--font-mono)]", "text-[10px]", "uppercase", "tracking-[0.1em]",
      "text-k-muted", "px-4", "py-2.5",
    );
  });

  it("right=true adds text-right", () => {
    render(<table><thead><tr><Th right>Hours</Th></tr></thead></table>);
    expect(screen.getByText("Hours")).toHaveClass("text-right");
  });

  it("right=false omits text-right", () => {
    render(<table><thead><tr><Th>Name</Th></tr></thead></table>);
    expect(screen.getByText("Name")).not.toHaveClass("text-right");
  });
});

describe("Tr", () => {
  it("applies base border class", () => {
    render(
      <table><tbody><Tr><td>X</td></Tr></tbody></table>,
    );
    expect(screen.getByText("X").parentElement).toHaveClass("border-b", "border-k-line");
  });

  it("onClick adds hover + cursor classes and fires", () => {
    const onClick = jest.fn();
    render(
      <table><tbody><Tr onClick={onClick}><td>X</td></Tr></tbody></table>,
    );
    const tr = screen.getByText("X").parentElement!;
    expect(tr).toHaveClass("hover:bg-k-surface", "cursor-pointer");
    fireEvent.click(tr);
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it("active=true adds active bg class", () => {
    render(
      <table><tbody><Tr active><td>X</td></Tr></tbody></table>,
    );
    expect(screen.getByText("X").parentElement).toHaveClass("bg-[#F9FFEA]");
  });
});

describe("Td", () => {
  it("applies base classes", () => {
    render(<table><tbody><tr><Td>X</Td></tr></tbody></table>);
    const el = screen.getByText("X");
    expect(el.tagName).toBe("TD");
    expect(el).toHaveClass("px-4", "py-3", "text-sm", "whitespace-nowrap");
  });

  it("mono adds mono font class", () => {
    render(<table><tbody><tr><Td mono>X</Td></tr></tbody></table>);
    expect(screen.getByText("X")).toHaveClass("font-[var(--font-mono)]");
  });

  it("muted adds text-k-muted", () => {
    render(<table><tbody><tr><Td muted>X</Td></tr></tbody></table>);
    expect(screen.getByText("X")).toHaveClass("text-k-muted");
  });

  it("bold adds font-semibold", () => {
    render(<table><tbody><tr><Td bold>X</Td></tr></tbody></table>);
    expect(screen.getByText("X")).toHaveClass("font-semibold");
  });

  it("right adds text-right", () => {
    render(<table><tbody><tr><Td right>X</Td></tr></tbody></table>);
    expect(screen.getByText("X")).toHaveClass("text-right");
  });
});
```

- [ ] **Step 2: Run tests and confirm failure**

```bash
npm test -- src/components/ui/__tests__/Table.test.tsx
```

Expected: FAIL with "Cannot find module '../Table'".

- [ ] **Step 3: Implement `Table`**

Create `web/src/components/ui/Table.tsx`:

```tsx
import React from "react";
import { cn } from "@/lib/utils";

export interface TableProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
}

export function Table({ className, children, ...rest }: TableProps) {
  return (
    <div
      className={cn("overflow-x-auto rounded-k-md border border-k-border w-full", className)}
      {...rest}
    >
      <table className="w-full">{children}</table>
    </div>
  );
}

export interface TheadProps extends React.HTMLAttributes<HTMLTableSectionElement> {
  children: React.ReactNode;
}

export function Thead({ className, children, ...rest }: TheadProps) {
  return (
    <thead className={cn("bg-k-bg border-b border-k-border", className)} {...rest}>
      {children}
    </thead>
  );
}

export interface ThProps extends React.ThHTMLAttributes<HTMLTableCellElement> {
  right?: boolean;
  children: React.ReactNode;
}

export function Th({ right = false, className, children, ...rest }: ThProps) {
  return (
    <th
      className={cn(
        "font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted px-4 py-2.5",
        right && "text-right",
        className,
      )}
      {...rest}
    >
      {children}
    </th>
  );
}

export interface TrProps extends React.HTMLAttributes<HTMLTableRowElement> {
  active?: boolean;
  children: React.ReactNode;
}

export function Tr({ onClick, active = false, className, children, ...rest }: TrProps) {
  return (
    <tr
      onClick={onClick}
      className={cn(
        "border-b border-k-line",
        onClick && "hover:bg-k-surface cursor-pointer",
        active && "bg-[#F9FFEA]",
        className,
      )}
      {...rest}
    >
      {children}
    </tr>
  );
}

export interface TdProps extends React.TdHTMLAttributes<HTMLTableCellElement> {
  mono?: boolean;
  muted?: boolean;
  bold?: boolean;
  right?: boolean;
  children: React.ReactNode;
}

export function Td({
  mono = false,
  muted = false,
  bold = false,
  right = false,
  className,
  children,
  ...rest
}: TdProps) {
  return (
    <td
      className={cn(
        "px-4 py-3 text-sm whitespace-nowrap",
        mono && "font-[var(--font-mono)]",
        muted && "text-k-muted",
        bold && "font-semibold",
        right && "text-right",
        className,
      )}
      {...rest}
    >
      {children}
    </td>
  );
}
```

- [ ] **Step 4: Run tests and confirm pass**

```bash
npm test -- src/components/ui/__tests__/Table.test.tsx
```

Expected: all tests pass.

- [ ] **Step 5: Typecheck gate**

```bash
npx tsc --noEmit
```

Expected: zero errors.

- [ ] **Step 6: Commit**

```bash
cd /Users/gonzalodevarona/Documents/klasio
git add web/src/components/ui/Table.tsx web/src/components/ui/__tests__/Table.test.tsx
git commit -m "feat(ui): add Table compound primitive"
```

---

## Task 9: `Pagination` primitive

**Files:**
- Create: `web/src/components/ui/Pagination.tsx`
- Create: `web/src/components/ui/__tests__/Pagination.test.tsx`

- [ ] **Step 1: Write failing tests**

Create `web/src/components/ui/__tests__/Pagination.test.tsx`:

```tsx
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
```

- [ ] **Step 2: Run tests and confirm failure**

```bash
npm test -- src/components/ui/__tests__/Pagination.test.tsx
```

Expected: FAIL with "Cannot find module '../Pagination'".

- [ ] **Step 3: Implement `Pagination`**

Create `web/src/components/ui/Pagination.tsx`:

```tsx
import React from "react";
import { Button } from "./Button";

export interface PaginationProps {
  page: number;
  totalPages: number;
  total: number;
  onPrev: () => void;
  onNext: () => void;
  labelPrev: string;
  labelNext: string;
  labelFormat: (page: number, totalPages: number, total: number) => string;
}

export function Pagination({
  page,
  totalPages,
  total,
  onPrev,
  onNext,
  labelPrev,
  labelNext,
  labelFormat,
}: PaginationProps) {
  return (
    <div className="flex justify-between items-center pt-4">
      <span className="font-[var(--font-mono)] text-xs text-k-muted">
        {labelFormat(page, totalPages, total)}
      </span>
      <div className="flex gap-2">
        <Button variant="outline" size="sm" onClick={onPrev} disabled={page === 0}>
          {labelPrev}
        </Button>
        <Button
          variant="outline"
          size="sm"
          onClick={onNext}
          disabled={page >= totalPages - 1}
        >
          {labelNext}
        </Button>
      </div>
    </div>
  );
}
```

- [ ] **Step 4: Run tests and confirm pass**

```bash
npm test -- src/components/ui/__tests__/Pagination.test.tsx
```

Expected: all tests pass.

- [ ] **Step 5: Typecheck gate**

```bash
npx tsc --noEmit
```

Expected: zero errors.

- [ ] **Step 6: Commit**

```bash
cd /Users/gonzalodevarona/Documents/klasio
git add web/src/components/ui/Pagination.tsx web/src/components/ui/__tests__/Pagination.test.tsx
git commit -m "feat(ui): add Pagination primitive"
```

---

## Task 10: `Modal` primitive (`'use client'`)

**Files:**
- Create: `web/src/components/ui/Modal.tsx`
- Create: `web/src/components/ui/__tests__/Modal.test.tsx`

- [ ] **Step 1: Write failing tests**

Create `web/src/components/ui/__tests__/Modal.test.tsx`:

```tsx
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
```

- [ ] **Step 2: Run tests and confirm failure**

```bash
npm test -- src/components/ui/__tests__/Modal.test.tsx
```

Expected: FAIL with "Cannot find module '../Modal'".

- [ ] **Step 3: Implement `Modal`**

Create `web/src/components/ui/Modal.tsx`:

```tsx
"use client";

import React, { useEffect } from "react";
import { Button } from "./Button";
import { cn } from "@/lib/utils";

export type ModalSize = "sm" | "md" | "lg" | "xl";

export interface ModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  size?: ModalSize;
  className?: string;
  children: React.ReactNode;
}

const SIZE_CLASSES: Record<ModalSize, string> = {
  sm: "max-w-[400px]",
  md: "max-w-[480px]",
  lg: "max-w-[600px]",
  xl: "max-w-[800px]",
};

export function Modal({
  open,
  onClose,
  title,
  size = "md",
  className,
  children,
}: ModalProps) {
  useEffect(() => {
    if (!open) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      data-testid="modal-overlay"
      className="fixed inset-0 z-50 bg-k-dark/55 backdrop-blur-sm flex items-center justify-center"
      onClick={onClose}
    >
      <div
        data-testid="modal-panel"
        onClick={(e) => e.stopPropagation()}
        className={cn(
          "bg-k-surface rounded-k-xl shadow-k-modal max-h-[85vh] overflow-y-auto w-full",
          SIZE_CLASSES[size],
          className,
        )}
      >
        <div className="flex justify-between items-center px-7 py-6 border-b border-k-border">
          <h2 className="text-base font-bold">{title}</h2>
          <Button
            variant="ghost"
            size="icon"
            onClick={onClose}
            data-testid="modal-close"
            aria-label="Close"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </Button>
        </div>
        <div className="px-7 py-6">{children}</div>
      </div>
    </div>
  );
}
```

- [ ] **Step 4: Run tests and confirm pass**

```bash
npm test -- src/components/ui/__tests__/Modal.test.tsx
```

Expected: all tests pass.

- [ ] **Step 5: Typecheck gate**

```bash
npx tsc --noEmit
```

Expected: zero errors.

- [ ] **Step 6: Commit**

```bash
cd /Users/gonzalodevarona/Documents/klasio
git add web/src/components/ui/Modal.tsx web/src/components/ui/__tests__/Modal.test.tsx
git commit -m "feat(ui): add Modal primitive"
```

---

## Task 11: `HoursBar` primitive

**Files:**
- Create: `web/src/components/ui/HoursBar.tsx`
- Create: `web/src/components/ui/__tests__/HoursBar.test.tsx`

- [ ] **Step 1: Write failing tests**

Create `web/src/components/ui/__tests__/HoursBar.test.tsx`:

```tsx
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
```

- [ ] **Step 2: Run tests and confirm failure**

```bash
npm test -- src/components/ui/__tests__/HoursBar.test.tsx
```

Expected: FAIL with "Cannot find module '../HoursBar'".

- [ ] **Step 3: Implement `HoursBar`**

Create `web/src/components/ui/HoursBar.tsx`:

```tsx
import React from "react";
import { cn } from "@/lib/utils";

export interface HoursBarProps {
  used: number;
  total: number;
}

function colorClass(pct: number): string {
  if (pct >= 100) return "bg-k-volt";
  if (pct >= 66) return "bg-[#8AE800]";
  if (pct >= 33) return "bg-[#FFC107]";
  return "bg-k-border";
}

export function HoursBar({ used, total }: HoursBarProps) {
  const remaining = Math.max(total - used, 0);
  const pct = total > 0 ? (remaining / total) * 100 : 0;
  const width = Math.min(pct, 100);

  return (
    <div className="flex items-center gap-2">
      <div
        data-testid="hours-bar-track"
        className="w-20 h-1 bg-k-line rounded-full overflow-hidden"
      >
        <div
          data-testid="hours-bar-fill"
          className={cn("h-full", colorClass(pct))}
          style={{ width: `${width}%` }}
        />
      </div>
      <span className="font-[var(--font-mono)] text-[11px] text-k-subtle">
        {used}/{total}h
      </span>
    </div>
  );
}
```

- [ ] **Step 4: Run tests and confirm pass**

```bash
npm test -- src/components/ui/__tests__/HoursBar.test.tsx
```

Expected: all tests pass.

- [ ] **Step 5: Typecheck gate**

```bash
npx tsc --noEmit
```

Expected: zero errors.

- [ ] **Step 6: Commit**

```bash
cd /Users/gonzalodevarona/Documents/klasio
git add web/src/components/ui/HoursBar.tsx web/src/components/ui/__tests__/HoursBar.test.tsx
git commit -m "feat(ui): add HoursBar primitive"
```

---

## Task 12: Barrel export

**Files:**
- Create: `web/src/components/ui/index.ts`

- [ ] **Step 1: Write the barrel file**

Create `web/src/components/ui/index.ts`:

```ts
export { Badge } from "./Badge";
export type { BadgeProps, BadgeVariant } from "./Badge";

export { Button } from "./Button";
export type { ButtonProps, ButtonVariant, ButtonSize } from "./Button";

export { Input } from "./Input";
export type { InputProps } from "./Input";

export { Select } from "./Select";
export type { SelectProps } from "./Select";

export { Card } from "./Card";
export type { CardProps } from "./Card";

export { StatCard } from "./StatCard";
export type { StatCardProps } from "./StatCard";

export { Table, Thead, Th, Tr, Td } from "./Table";
export type {
  TableProps,
  TheadProps,
  ThProps,
  TrProps,
  TdProps,
} from "./Table";

export { Pagination } from "./Pagination";
export type { PaginationProps } from "./Pagination";

export { Modal } from "./Modal";
export type { ModalProps, ModalSize } from "./Modal";

export { HoursBar } from "./HoursBar";
export type { HoursBarProps } from "./HoursBar";
```

- [ ] **Step 2: Typecheck gate**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npx tsc --noEmit
```

Expected: zero errors. (No new tests — barrel is type-checked transitively.)

- [ ] **Step 3: Commit**

```bash
cd /Users/gonzalodevarona/Documents/klasio
git add web/src/components/ui/index.ts
git commit -m "chore(ui): add ui primitives barrel export"
```

---

## Final Verification (after all 12 commits)

Run from `/Users/gonzalodevarona/Documents/klasio/web`:

- [ ] **All tests green**

```bash
npm test
```

Expected: full suite green, including ~80 new ui-primitive tests.

- [ ] **Typecheck clean**

```bash
npx tsc --noEmit
```

Expected: zero errors.

- [ ] **Production build succeeds**

```bash
npm run build
```

Expected: Next.js build completes without errors. (`Modal` is the only `'use client'` primitive; the rest stay RSC-friendly.)

- [ ] **Lint clean**

```bash
npm run lint
```

Expected: no new warnings introduced by `web/src/components/ui/**` or `web/src/lib/utils.ts`.

- [ ] **Visual sanity (optional, no automated check)**

The primitives have no consumers yet (per Step 2 non-goals). Visual verification happens in Step 3 when migrations begin.

---

## Out of Scope (deferred to Step 3+)

- Migrating existing components in `web/src/components/{memberships,students,programs,...}` to consume the primitives.
- Replacing `MembershipStatusBadge` and other domain wrappers with the new primitives.
- New primitives not in this spec (Dropdown, Tooltip, Toast).
- Focus-trap inside `Modal` — Escape + overlay click + close button cover the v1 contract.
- Storybook or visual regression tests.
