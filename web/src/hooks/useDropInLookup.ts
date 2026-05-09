"use client";

import { useState, useEffect, useRef } from "react";
import type { DropInAttendeeLookupResponse } from "@/lib/dropIn";

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

export type LookupStatus = "idle" | "searching" | "found" | "notFound" | "error";

export function useDropInLookup(phone: string, debounceMs = 300) {
  const [status, setStatus] = useState<LookupStatus>("idle");
  const [data, setData] = useState<DropInAttendeeLookupResponse | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    const digits = phone.replace(/\D/g, "");
    if (digits.length < 7) {
      setStatus("idle");
      setData(null);
      return;
    }

    setStatus("searching");
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    const timer = setTimeout(async () => {
      try {
        const res = await fetch(
          `${API_BASE}/drop-in-attendees/lookup?phone=${encodeURIComponent(phone)}`,
          { credentials: "include", signal: controller.signal }
        );
        if (controller.signal.aborted) return;
        if (res.status === 404) {
          setData(null);
          setStatus("notFound");
        } else if (res.ok) {
          const result = await res.json();
          setData(result);
          setStatus("found");
        } else {
          setStatus("error");
        }
      } catch {
        if (!controller.signal.aborted) setStatus("error");
      }
    }, debounceMs);

    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [phone, debounceMs]);

  return { status, data };
}
