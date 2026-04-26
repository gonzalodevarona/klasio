# Notifications Token Restyle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate the three notifications components (`NotificationItem`, `NotificationTypeIcon`, `NotificationList`) from raw Tailwind palette classes to Klasio design-system tokens, with no behavioral, hook, state, prop, routing, or i18n changes.

**Architecture:** Pure className swap plus two purely structural DOM additions in `NotificationItem` (a 3px volt accent bar for unread items and an 8×8 icon container). Tests assert the structural invariants that the spec promises (accent bar, icon wrapper, token classes on key surfaces) so a future regression that drops a token surfaces immediately. Spec lives at `docs/superpowers/specs/2026-04-25-notifications-token-restyle-design.md`.

**Tech Stack:** Next.js 15.1, React 19, TypeScript 5.9, Tailwind 3.4, Jest 29 + ts-jest, `@testing-library/react`, `@testing-library/jest-dom`, `next-intl` test wrapper at `web/__test-support__/renderWithIntl.tsx`.

---

## Engineering Notes

**TDD discipline on a pure restyle.** ClassName-only swaps invite brittle tests. We mitigate by testing only what the spec calls out as a *structural* or *behaviorally observable* invariant: the unread accent bar exists, the icon container exists, the active tab carries the elevated surface, the focus ring uses the volt token. Each test is one assertion at one point of contract — not a snapshot. If a designer later changes the literal token name, exactly one line in one test moves.

**File ordering.** We do `NotificationTypeIcon` first (smallest, zero dependencies, sets the token-mapping precedent), then `NotificationItem` (depends on the icon component for the wrapper test), then `NotificationList` (depends on `NotificationItem` for any rendered-item assertion).

**Verification budget.** Each task ends with the targeted Jest run plus a `git commit`. The final task runs `npx tsc --noEmit` and a global Jest pass to catch any cross-file fallout.

**Token availability.** All tokens used (`bg-k-bg`, `bg-k-surface`, `text-k-dark`, `text-k-muted`, `border-k-border`, `divide-k-line`, `bg-k-volt`, `text-k-warn-text`, `text-k-danger-text`, `rounded-k-sm`, `rounded-k-md`, `shadow-k-card`) are confirmed present in `web/tailwind.config.ts`. Two literal hex values are intentionally inlined: `#F9FFEA` (unread tint, not yet a named token) and `#9A9A98` on the timestamp (mirrors `k-muted` but kept literal per spec to clarify the timestamp-specific intent).

**i18n.** The `notifications.*` and `pagination.*` namespaces are already populated in both `messages/en.json` and the test helper `__test-support__/renderWithIntl.tsx`. No translation work.

---

## File Inventory

| File | Action | Responsibility after change |
|---|---|---|
| `web/src/components/notifications/NotificationTypeIcon.tsx` | Modify | Maps notification type → Lucide icon with token-colored stroke |
| `web/src/components/notifications/NotificationItem.tsx` | Modify | Renders one row: accent bar (unread), icon wrapper, title/body/timestamp, click handler |
| `web/src/components/notifications/NotificationList.tsx` | Modify | Tab toggle, mark-all, list container, loading/empty/error, pagination |
| `web/src/components/notifications/__tests__/NotificationTypeIcon.test.tsx` | Create | Token-color regression for the three icon variants |
| `web/src/components/notifications/__tests__/NotificationItem.test.tsx` | Create | Accent bar visibility, icon wrapper, surface/focus tokens |
| `web/src/components/notifications/__tests__/NotificationList.test.tsx` | Create | Active-tab elevation, list container border, error/empty token, pagination border |

No production files are added or deleted. The existing `NotificationBell.test.tsx` is untouched.

---

## Task 1: Restyle `NotificationTypeIcon`

**Files:**
- Create: `web/src/components/notifications/__tests__/NotificationTypeIcon.test.tsx`
- Modify: `web/src/components/notifications/NotificationTypeIcon.tsx`

- [ ] **Step 1: Write the failing test**

Create `web/src/components/notifications/__tests__/NotificationTypeIcon.test.tsx`:

```tsx
import { render } from "@testing-library/react";
import NotificationTypeIcon from "../NotificationTypeIcon";

describe("NotificationTypeIcon", () => {
  it("renders ALERTED with k-warn-text token", () => {
    const { container } = render(<NotificationTypeIcon type="CLASS_SESSION_ALERTED" />);
    const svg = container.querySelector("svg");
    expect(svg).not.toBeNull();
    expect(svg).toHaveClass("text-k-warn-text");
  });

  it("renders CANCELLED with k-danger-text token", () => {
    const { container } = render(<NotificationTypeIcon type="CLASS_SESSION_CANCELLED" />);
    expect(container.querySelector("svg")).toHaveClass("text-k-danger-text");
  });

  it("renders default (unknown type) with k-muted token", () => {
    const { container } = render(<NotificationTypeIcon type="SOME_OTHER_TYPE" />);
    expect(container.querySelector("svg")).toHaveClass("text-k-muted");
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd web && npx jest src/components/notifications/__tests__/NotificationTypeIcon.test.tsx`
Expected: 3 failures asserting the current classes (`text-amber-500`, `text-red-500`, `text-gray-400`) do not match the new tokens.

- [ ] **Step 3: Implement the token swap**

Replace the entire body of `web/src/components/notifications/NotificationTypeIcon.tsx` with:

```tsx
"use client";

import { AlertTriangle, Bell } from "lucide-react";

interface NotificationTypeIconProps {
  type: string;
}

export default function NotificationTypeIcon({ type }: NotificationTypeIconProps) {
  if (type === "CLASS_SESSION_ALERTED") {
    return <AlertTriangle className="w-5 h-5 text-k-warn-text" />;
  }
  if (type === "CLASS_SESSION_CANCELLED") {
    return <Bell className="w-5 h-5 text-k-danger-text" />;
  }
  return <Bell className="w-5 h-5 text-k-muted" />;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd web && npx jest src/components/notifications/__tests__/NotificationTypeIcon.test.tsx`
Expected: 3 passing tests.

- [ ] **Step 5: Commit**

```bash
git add web/src/components/notifications/NotificationTypeIcon.tsx \
        web/src/components/notifications/__tests__/NotificationTypeIcon.test.tsx
git commit -m "refactor(notifications): migrate NotificationTypeIcon to design tokens"
```

---

## Task 2: Restyle `NotificationItem`

**Files:**
- Create: `web/src/components/notifications/__tests__/NotificationItem.test.tsx`
- Modify: `web/src/components/notifications/NotificationItem.tsx`

- [ ] **Step 1: Write the failing test**

Create `web/src/components/notifications/__tests__/NotificationItem.test.tsx`:

```tsx
import { render, screen } from "@testing-library/react";
import NotificationItem from "../NotificationItem";

jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn() }),
}));

const baseNotification = {
  id: "n-1",
  type: "CLASS_SESSION_ALERTED",
  title: "Class alert",
  body: "Your class has an alert",
  read: false,
  createdAt: new Date().toISOString(),
  metadata: { classId: "c-1" },
} as const;

describe("NotificationItem", () => {
  it("renders the volt accent bar and tinted background when unread", () => {
    render(<NotificationItem notification={baseNotification} onRead={jest.fn()} />);
    const button = screen.getByRole("button");

    // outer button is positioned and tinted
    expect(button).toHaveClass("relative");
    expect(button).toHaveClass("bg-[#F9FFEA]");

    // accent bar is the first absolute span carrying the volt token
    const accent = button.querySelector("span.bg-k-volt");
    expect(accent).not.toBeNull();
    expect(accent).toHaveClass("absolute", "w-[3px]");
  });

  it("uses k-surface background and omits the accent bar when read", () => {
    render(
      <NotificationItem
        notification={{ ...baseNotification, read: true }}
        onRead={jest.fn()}
      />,
    );
    const button = screen.getByRole("button");

    expect(button).toHaveClass("bg-k-surface");
    expect(button.querySelector("span.bg-k-volt")).toBeNull();
  });

  it("renders the icon inside an 8x8 k-bg wrapper", () => {
    const { container } = render(
      <NotificationItem notification={baseNotification} onRead={jest.fn()} />,
    );
    const wrapper = container.querySelector("div.bg-k-bg.w-8.h-8");
    expect(wrapper).not.toBeNull();
    expect(wrapper?.querySelector("svg")).not.toBeNull();
  });

  it("uses the k-volt focus ring token on the outer button", () => {
    render(<NotificationItem notification={baseNotification} onRead={jest.fn()} />);
    expect(screen.getByRole("button")).toHaveClass("focus-visible:ring-k-volt");
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd web && npx jest src/components/notifications/__tests__/NotificationItem.test.tsx`
Expected: 4 failures — outer button missing `relative`/`bg-[#F9FFEA]`/`bg-k-surface`/`focus-visible:ring-k-volt`; no accent span; no icon wrapper.

- [ ] **Step 3: Implement the token swap and structural additions**

Replace the entire body of `web/src/components/notifications/NotificationItem.tsx` with:

```tsx
"use client";

import { useRouter } from "next/navigation";
import type { Notification } from "@/hooks/useNotifications";
import NotificationTypeIcon from "./NotificationTypeIcon";

function formatRelativeTime(isoString: string): string {
  const diff = Date.now() - new Date(isoString).getTime();
  const seconds = Math.floor(diff / 1000);
  if (seconds < 60) return "just now";
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

interface NotificationItemProps {
  notification: Notification;
  onRead: (id: string) => void;
}

export default function NotificationItem({
  notification,
  onRead,
}: NotificationItemProps) {
  const router = useRouter();

  function handleClick() {
    onRead(notification.id);

    const isSessionType =
      notification.type === "CLASS_SESSION_ALERTED" ||
      notification.type === "CLASS_SESSION_CANCELLED";
    const classId = notification.metadata?.classId;

    if (isSessionType && classId) {
      router.push(`/classes/${classId}`);
    } else {
      router.push("/notifications");
    }
  }

  return (
    <button
      type="button"
      onClick={handleClick}
      className={[
        "relative w-full text-left flex items-start gap-3 px-4 py-3 transition-colors",
        "hover:bg-k-bg focus:outline-none focus-visible:ring-2 focus-visible:ring-k-volt",
        notification.read ? "bg-k-surface" : "bg-[#F9FFEA]",
      ].join(" ")}
    >
      {!notification.read && (
        <span className="absolute left-0 top-0 bottom-0 w-[3px] bg-k-volt rounded-r-full" />
      )}
      <div className="mt-0.5 shrink-0 w-8 h-8 rounded-[8px] bg-k-bg flex items-center justify-center">
        <NotificationTypeIcon type={notification.type} />
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-k-dark truncate">
          {notification.title}
        </p>
        <p className="text-xs text-k-muted mt-0.5 line-clamp-2">
          {notification.body}
        </p>
        <p className="text-[#9A9A98] font-[var(--font-mono)] text-[10px] mt-1">
          {formatRelativeTime(notification.createdAt)}
        </p>
      </div>
    </button>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd web && npx jest src/components/notifications/__tests__/NotificationItem.test.tsx`
Expected: 4 passing tests.

- [ ] **Step 5: Commit**

```bash
git add web/src/components/notifications/NotificationItem.tsx \
        web/src/components/notifications/__tests__/NotificationItem.test.tsx
git commit -m "refactor(notifications): migrate NotificationItem to design tokens"
```

---

## Task 3: Restyle `NotificationList`

**Files:**
- Create: `web/src/components/notifications/__tests__/NotificationList.test.tsx`
- Modify: `web/src/components/notifications/NotificationList.tsx`

- [ ] **Step 1: Write the failing test**

Create `web/src/components/notifications/__tests__/NotificationList.test.tsx`:

```tsx
import React from "react";
import { screen } from "@testing-library/react";
import { renderWithIntl as render } from "../../../../__test-support__/renderWithIntl";
import NotificationList from "../NotificationList";

jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn() }),
}));

jest.mock("@/context/NotificationCountContext", () => ({
  useNotificationCount: () => ({ count: 0, refreshCount: jest.fn() }),
  NotificationCountProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

const useNotificationsMock = jest.fn();

jest.mock("@/hooks/useNotifications", () => ({
  useNotifications: (...args: unknown[]) => useNotificationsMock(...args),
  useMarkNotificationRead: () => ({ markRead: jest.fn() }),
  useMarkAllNotificationsRead: () => ({ markAllRead: jest.fn() }),
}));

function setNotifications(state: {
  notifications?: unknown[];
  totalPages?: number;
  isLoading?: boolean;
  error?: string | null;
}) {
  useNotificationsMock.mockReturnValue({
    notifications: state.notifications ?? [],
    totalPages: state.totalPages ?? 0,
    isLoading: state.isLoading ?? false,
    error: state.error ?? null,
    refresh: jest.fn(),
  });
}

describe("NotificationList", () => {
  beforeEach(() => useNotificationsMock.mockReset());

  it("renders the active 'All' tab on the elevated surface token", () => {
    setNotifications({});
    render(<NotificationList />);
    const allTab = screen.getByRole("button", { name: "All" });
    expect(allTab).toHaveClass("bg-k-surface", "shadow-k-card", "text-k-dark");
  });

  it("renders the empty state with the muted token", () => {
    setNotifications({});
    render(<NotificationList />);
    const empty = screen.getByText("No notifications found.");
    expect(empty).toHaveClass("text-k-muted");
  });

  it("renders the error state with the danger token", () => {
    setNotifications({ error: "Boom" });
    render(<NotificationList />);
    expect(screen.getByText("Boom")).toHaveClass("text-k-danger-text");
  });

  it("wraps non-empty notifications in a k-border list container", () => {
    setNotifications({
      notifications: [
        {
          id: "n-1",
          type: "CLASS_SESSION_ALERTED",
          title: "t",
          body: "b",
          read: false,
          createdAt: new Date().toISOString(),
          metadata: {},
        },
      ],
      totalPages: 1,
    });
    const { container } = render(<NotificationList />);
    const wrapper = container.querySelector("div.border-k-border.divide-k-line");
    expect(wrapper).not.toBeNull();
  });

  it("renders pagination buttons with the k-border token when multiple pages", () => {
    setNotifications({ notifications: [], totalPages: 3 });
    render(<NotificationList />);
    expect(screen.getByRole("button", { name: "Previous" })).toHaveClass("border-k-border");
    expect(screen.getByRole("button", { name: "Next" })).toHaveClass("border-k-border");
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd web && npx jest src/components/notifications/__tests__/NotificationList.test.tsx`
Expected: 5 failures — active tab missing `bg-k-surface`/`shadow-k-card`; empty/error/list/pagination missing token classes.

- [ ] **Step 3: Implement the token swap**

Replace the entire body of `web/src/components/notifications/NotificationList.tsx` with:

```tsx
"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import {
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotifications,
} from "@/hooks/useNotifications";
import { useNotificationCount } from "@/context/NotificationCountContext";
import NotificationItem from "./NotificationItem";

export default function NotificationList() {
  const t = useTranslations("notifications");
  const tPag = useTranslations("pagination");
  const [page, setPage] = useState(0);
  const [unreadOnly, setUnreadOnly] = useState(false);

  const { notifications, totalPages, isLoading, error, refresh } =
    useNotifications(page, unreadOnly);
  const { markRead } = useMarkNotificationRead();
  const { markAllRead } = useMarkAllNotificationsRead();
  const { refreshCount } = useNotificationCount();

  async function handleRead(id: string) {
    await markRead(id);
    refresh();
    refreshCount();
  }

  async function handleMarkAll() {
    await markAllRead();
    refresh();
    refreshCount();
  }

  function handleTabChange(nextUnreadOnly: boolean) {
    setUnreadOnly(nextUnreadOnly);
    setPage(0);
  }

  return (
    <div className="space-y-4">
      {/* Controls */}
      <div className="flex items-center justify-between gap-3 flex-wrap">
        {/* Tab toggle */}
        <div className="flex gap-1 bg-k-bg rounded-k-sm p-1">
          <button
            type="button"
            onClick={() => handleTabChange(false)}
            className={[
              "px-3 py-1 text-sm rounded-[6px] transition-colors",
              !unreadOnly
                ? "bg-k-surface font-semibold text-k-dark shadow-k-card"
                : "text-k-muted hover:text-k-dark",
            ].join(" ")}
          >
            {t("tabAll")}
          </button>
          <button
            type="button"
            onClick={() => handleTabChange(true)}
            className={[
              "px-3 py-1 text-sm rounded-[6px] transition-colors",
              unreadOnly
                ? "bg-k-surface font-semibold text-k-dark shadow-k-card"
                : "text-k-muted hover:text-k-dark",
            ].join(" ")}
          >
            {t("tabUnreadOnly")}
          </button>
        </div>

        <button
          type="button"
          onClick={handleMarkAll}
          className="text-k-muted hover:text-k-dark text-sm font-medium transition-colors"
        >
          {t("markAllAsRead")}
        </button>
      </div>

      {/* Content */}
      {isLoading && (
        <p className="text-sm text-k-muted py-6 text-center">Loading…</p>
      )}

      {error && (
        <p className="text-sm text-k-danger-text py-6 text-center">{error}</p>
      )}

      {!isLoading && !error && notifications.length === 0 && (
        <p className="text-sm text-k-muted py-6 text-center">
          {t("listEmpty")}
        </p>
      )}

      {!isLoading && !error && notifications.length > 0 && (
        <div className="rounded-k-md border border-k-border divide-y divide-k-line overflow-hidden">
          {notifications.map((n) => (
            <NotificationItem key={n.id} notification={n} onRead={handleRead} />
          ))}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between text-sm text-k-muted">
          <button
            type="button"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="px-3 py-1 rounded border border-k-border disabled:opacity-40 hover:bg-k-bg transition-colors"
          >
            {tPag("previous")}
          </button>
          <span className="font-[var(--font-mono)] text-xs">
            {tPag("summary", { current: page + 1, total: totalPages, count: totalPages })}
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
            className="px-3 py-1 rounded border border-k-border disabled:opacity-40 hover:bg-k-bg transition-colors"
          >
            {tPag("next")}
          </button>
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd web && npx jest src/components/notifications/__tests__/NotificationList.test.tsx`
Expected: 5 passing tests.

- [ ] **Step 5: Commit**

```bash
git add web/src/components/notifications/NotificationList.tsx \
        web/src/components/notifications/__tests__/NotificationList.test.tsx
git commit -m "refactor(notifications): migrate NotificationList to design tokens"
```

---

## Task 4: Cross-file verification

**Files:** none modified by default; this task only writes new code if a regression surfaces.

- [ ] **Step 1: Confirm no legacy palette classes remain in the three production files**

Run:

```bash
cd web && grep -nE \
  'bg-blue-50|bg-white|bg-gray-50|ring-indigo|text-gray-900|text-gray-500|text-gray-400|text-indigo-(600|800)|text-amber-500|text-red-500|border-gray-200|divide-gray-100' \
  src/components/notifications/NotificationItem.tsx \
  src/components/notifications/NotificationTypeIcon.tsx \
  src/components/notifications/NotificationList.tsx
```

Expected: no output (exit code 1). If any line matches, return to the relevant task and complete the missing token swap.

- [ ] **Step 2: TypeScript check**

Run: `cd web && npx tsc --noEmit`
Expected: zero errors. If errors appear in the three modified files or their tests, fix them before proceeding. Errors in unrelated files predate this work and should be left alone.

- [ ] **Step 3: Full notifications test pass**

Run: `cd web && npx jest src/components/notifications`
Expected: every test in the directory passes — the three new files plus the pre-existing `NotificationBell.test.tsx`.

- [ ] **Step 4: Visual smoke (manual)**

Start the dev server and exercise the rendered surface:

```bash
cd web && npm run dev
```

In a browser at `http://localhost:3000/notifications`, verify:

1. An unread row shows a 3px volt accent bar at its left edge over a faint volt-tinted background.
2. A read row shows the warm `k-surface` background with no accent bar.
3. Hovering a row transitions to the `k-bg` shade.
4. Tab → focus the row with the keyboard. The focus ring is volt, not indigo.
5. The active tab pill sits on `k-surface` with a soft card shadow; the inactive tab is muted text on the `k-bg` track.
6. The "Mark all as read" link is muted and darkens on hover.
7. With no data, the empty-state copy is muted; trigger an error and confirm the danger token color.
8. With more than one page, pagination buttons render with the `k-border` outline and `k-bg` hover.

Document the smoke result in the commit message of the next step. If something looks wrong visually but passes tests, fix the offending file *and* extend its test to lock the behavior in.

- [ ] **Step 5: Final commit (only if Step 4 surfaces a fix)**

If Step 4 was clean, skip this step. Otherwise:

```bash
git add web/src/components/notifications
git commit -m "fix(notifications): correct token regression caught in visual smoke"
```

---

## Self-Review Notes

- **Spec coverage.** Every bullet in `2026-04-25-notifications-token-restyle-design.md` maps to a concrete edit in Task 1, 2, or 3. No spec line is left to "TODO".
- **No placeholders.** Every step ships either runnable code, a runnable command, or a documented manual check. No "implement appropriately."
- **Type consistency.** The `Notification` type is imported from `@/hooks/useNotifications` (unchanged). Test fixtures match its shape (`id`, `type`, `title`, `body`, `read`, `createdAt`, `metadata`). The mock for `next/navigation` uses the same shape (`useRouter() => { push }`) that the component calls.
- **Token consistency.** Across all three files: `k-bg` (track / hover), `k-surface` (active surface), `k-volt` (accent + focus ring), `k-dark` (primary text), `k-muted` (secondary text), `k-border`/`k-line` (rules), `k-warn-text`/`k-danger-text` (semantic icon + error). No drift between tasks.
