"use client";

import { useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { Loader2 } from "lucide-react";
import { useDropInLookup } from "@/hooks/useDropInLookup";
import { useRegisterDropIn, DropInPhoneConflictError } from "@/hooks/useRegisterDropIn";
import { PhoneCollisionDialog } from "./PhoneCollisionDialog";

interface Props {
  classId: string;
  sessionDate: string;
  startTime: string;
  programDropInPrice: string;
  onRegistered: () => void;
  onClose: () => void;
}

type CollisionState = {
  open: boolean;
  existingAttendeeId: string;
  fullName: string;
  totalVisits: number;
  phone: string;
};

export function DropInModal({
  classId,
  sessionDate,
  startTime,
  programDropInPrice,
  onRegistered,
  onClose,
}: Props) {
  const t = useTranslations("attendance.dropIn");
  const { mutate, isPending } = useRegisterDropIn(classId, sessionDate);

  const [phone, setPhone] = useState("");
  const [name, setName] = useState("");
  const [amount, setAmount] = useState(programDropInPrice);
  const [paymentMethod, setPaymentMethod] = useState<"CASH" | "TRANSFER">("CASH");
  const [serverError, setServerError] = useState<string | null>(null);
  const [successBanner, setSuccessBanner] = useState<string | null>(null);
  const [collision, setCollision] = useState<CollisionState>({
    open: false,
    existingAttendeeId: "",
    fullName: "",
    totalVisits: 0,
    phone: "",
  });

  const phoneRef = useRef<HTMLInputElement>(null);
  const nameRef = useRef<HTMLInputElement>(null);
  const amountRef = useRef<HTMLInputElement>(null);
  // Stores the existingAttendeeId when resubmitting after a phone collision
  const existingIdRef = useRef<string | undefined>(undefined);

  const { status: lookupStatus, data: lookupData } = useDropInLookup(phone);

  useEffect(() => {
    phoneRef.current?.focus();
  }, []);

  useEffect(() => {
    if (lookupStatus === "found" && lookupData) {
      setName(lookupData.fullName);
      setTimeout(() => amountRef.current?.focus(), 50);
    }
    if (lookupStatus === "notFound") {
      setName("");
      setTimeout(() => nameRef.current?.focus(), 50);
    }
  }, [lookupStatus, lookupData]);

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [onClose]);

  const canSubmit =
    phone.replace(/\D/g, "").length >= 7 &&
    (lookupStatus === "found" ||
      (lookupStatus === "notFound" && name.trim().length > 0)) &&
    parseFloat(amount) > 0 &&
    !isPending;

  const handleSubmit = async (existingIdOverride?: string) => {
    setServerError(null);
    // Prefer the explicitly passed override; fall back to ref (set by collision confirm)
    const resolvedExistingId =
      existingIdOverride ?? existingIdRef.current ?? (lookupStatus === "found" ? lookupData?.id : undefined);
    // Clear the ref after consuming it
    existingIdRef.current = undefined;
    const useExisting = resolvedExistingId;
    const input = {
      startTime,
      attendee: useExisting
        ? { existingId: useExisting }
        : { newAttendee: { fullName: name.trim(), phone: phone.replace(/\D/g, "") } },
      amount,
      paymentMethod,
    } as const;

    try {
      await mutate(input as Parameters<typeof mutate>[0]);
      const displayName =
        lookupStatus === "found" && lookupData ? lookupData.fullName : name;
      setSuccessBanner(t("successBanner", { fullName: displayName }));
      setTimeout(() => {
        onRegistered();
        onClose();
      }, 1500);
    } catch (err: unknown) {
      if (err instanceof DropInPhoneConflictError) {
        setCollision({
          open: true,
          existingAttendeeId: err.existingAttendeeId,
          fullName: err.fullName,
          totalVisits: err.totalVisits,
          phone,
        });
      } else {
        const code = (err as { code?: string })?.code ?? "UNKNOWN";
        try {
          setServerError(t(`errors.${code}` as Parameters<typeof t>[0]));
        } catch {
          setServerError(t("errors.UNKNOWN"));
        }
      }
    }
  };

  const nameReadOnly = lookupStatus === "found";
  const nameDisabled =
    lookupStatus === "idle" || lookupStatus === "searching";

  return (
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
        <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">
              {t("modalTitle")}
            </h2>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 text-xl leading-none"
            >
              &times;
            </button>
          </div>

          {/* Phone */}
          <div className="mb-3">
            <label htmlFor="dropin-phone" className="block text-sm font-medium text-gray-700 mb-1">
              {t("phoneLabel")} *
            </label>
            <div className="relative">
              <input
                id="dropin-phone"
                ref={phoneRef}
                type="tel"
                inputMode="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder={t("phonePlaceholder")}
                maxLength={20}
                className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 pr-8"
              />
              {lookupStatus === "searching" && (
                <Loader2 className="absolute right-2 top-2.5 w-4 h-4 animate-spin text-gray-400" />
              )}
            </div>
            {lookupStatus === "found" && lookupData && (
              <p className="text-xs text-green-600 mt-1">
                {t("recurringVisitor", { count: lookupData.totalVisits + 1 })}
              </p>
            )}
            {lookupStatus === "error" && (
              <p className="text-xs text-red-600 mt-1">
                {t("errors.lookupFailed")}
              </p>
            )}
          </div>

          {/* Name */}
          <div className="mb-3">
            <label htmlFor="dropin-name" className="block text-sm font-medium text-gray-700 mb-1">
              {t("fullNameLabel")} *
            </label>
            <input
              id="dropin-name"
              ref={nameRef}
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              readOnly={nameReadOnly}
              disabled={nameDisabled}
              maxLength={200}
              className={`w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 ${
                nameReadOnly ? "bg-gray-50 text-gray-500 cursor-not-allowed" : ""
              } ${nameDisabled ? "bg-gray-100 text-gray-400 cursor-not-allowed" : ""}`}
            />
          </div>

          {/* Amount + Payment Method */}
          <div className="flex gap-3 mb-4">
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {t("amountLabel")} *
              </label>
              <input
                ref={amountRef}
                type="number"
                min="0.01"
                step="0.01"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {t("paymentMethodLabel")} *
              </label>
              <div className="flex border border-gray-300 rounded-md overflow-hidden">
                {(["CASH", "TRANSFER"] as const).map((m) => (
                  <button
                    key={m}
                    type="button"
                    onClick={() => setPaymentMethod(m)}
                    className={`flex-1 py-2 text-sm font-medium transition-colors ${
                      paymentMethod === m
                        ? "bg-indigo-600 text-white"
                        : "bg-white text-gray-700 hover:bg-gray-50"
                    }`}
                  >
                    {t(`paymentMethod.${m.toLowerCase()}` as Parameters<typeof t>[0])}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {serverError && (
            <div className="mb-3 p-3 bg-red-50 border border-red-200 rounded-md">
              <p className="text-sm text-red-700">{serverError}</p>
            </div>
          )}

          {successBanner && (
            <div
              className="mb-3 p-3 bg-green-50 border border-green-200 rounded-md"
              aria-live="polite"
            >
              <p className="text-sm text-green-700">{successBanner}</p>
            </div>
          )}

          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
            >
              {t("cancelButton")}
            </button>
            <button
              type="button"
              onClick={() => handleSubmit()}
              disabled={!canSubmit || isPending}
              className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-md hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
            >
              {isPending && <Loader2 className="w-4 h-4 animate-spin" />}
              {t("submitButton")}
            </button>
          </div>
        </div>
      </div>

      <PhoneCollisionDialog
        open={collision.open}
        phone={collision.phone}
        fullName={collision.fullName}
        totalVisits={collision.totalVisits}
        existingAttendeeId={collision.existingAttendeeId}
        onConfirm={(existingId) => {
          existingIdRef.current = existingId;
          setCollision((c) => ({ ...c, open: false }));
          handleSubmit(existingId);
        }}
        onCancel={() => {
          setCollision((c) => ({ ...c, open: false }));
          phoneRef.current?.focus();
        }}
      />
    </>
  );
}
