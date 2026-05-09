import { renderHook, act } from "@testing-library/react";
import { useRegisterDropIn, DropInPhoneConflictError } from "../useRegisterDropIn";

global.fetch = jest.fn();

describe("useRegisterDropIn", () => {
  beforeEach(() => (global.fetch as jest.Mock).mockReset());

  const input = {
    startTime: "18:00:00",
    attendee: { newAttendee: { fullName: "Ana García", phone: "3001234567" } },
    amount: "25000",
    paymentMethod: "CASH" as const,
  };

  it("calls fetch with correct URL and body", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({
        registrationId: "r1",
        attendeeId: "a1",
        paymentId: "p1",
        status: "PRESENT",
        attendeeWasNew: true,
        attendeeTotalVisits: 1,
      }),
    });

    const { result } = renderHook(() => useRegisterDropIn("c1", "2026-05-09"));
    await act(async () => {
      await result.current.mutate(input);
    });

    const [url, opts] = (global.fetch as jest.Mock).mock.calls[0];
    expect(url).toContain("/classes/c1/sessions/2026-05-09/drop-in");
    expect(opts.method).toBe("POST");
    expect(JSON.parse(opts.body)).toEqual(input);
  });

  it("isPending toggles: true during request, false after", async () => {
    let resolvePromise!: (v: unknown) => void;
    const fetchPromise = new Promise((res) => { resolvePromise = res; });
    (global.fetch as jest.Mock).mockReturnValueOnce(fetchPromise);

    const { result } = renderHook(() => useRegisterDropIn("c1", "2026-05-09"));
    expect(result.current.isPending).toBe(false);

    let mutatePromise: Promise<unknown>;
    act(() => {
      mutatePromise = result.current.mutate(input);
    });
    expect(result.current.isPending).toBe(true);

    await act(async () => {
      resolvePromise({ ok: true, status: 201, json: async () => ({ registrationId: "r1", attendeeId: "a1", paymentId: "p1", status: "PRESENT", attendeeWasNew: true, attendeeTotalVisits: 1 }) });
      await mutatePromise!;
    });
    expect(result.current.isPending).toBe(false);
  });

  it("throws DropInPhoneConflictError on 409 with DROP_IN_PHONE_EXISTS", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: false,
      status: 409,
      json: async () => ({
        error: { code: "DROP_IN_PHONE_EXISTS", message: "Phone already registered" },
        existingAttendeeId: "existing-uuid",
        fullName: "Ana García",
        totalVisits: 5,
      }),
    });

    const { result } = renderHook(() => useRegisterDropIn("c1", "2026-05-09"));
    let caughtError: unknown;
    await act(async () => {
      try {
        await result.current.mutate(input);
      } catch (e) {
        caughtError = e;
      }
    });

    expect(caughtError).toBeInstanceOf(DropInPhoneConflictError);
    const conflictErr = caughtError as DropInPhoneConflictError;
    expect(conflictErr.existingAttendeeId).toBe("existing-uuid");
    expect(conflictErr.fullName).toBe("Ana García");
    expect(conflictErr.totalVisits).toBe(5);
    expect(result.current.error).toBeInstanceOf(DropInPhoneConflictError);
  });

  it("throws generic error on other failures", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: false,
      status: 400,
      json: async () => ({ error: { code: "VALIDATION_ERROR", message: "Bad request" } }),
    });

    const { result } = renderHook(() => useRegisterDropIn("c1", "2026-05-09"));
    let caughtError: unknown;
    await act(async () => {
      try {
        await result.current.mutate(input);
      } catch (e) {
        caughtError = e;
      }
    });

    expect(caughtError).toMatchObject({ code: "VALIDATION_ERROR" });
    expect(result.current.error).not.toBeNull();
    expect(result.current.error).not.toBeInstanceOf(DropInPhoneConflictError);
  });
});
