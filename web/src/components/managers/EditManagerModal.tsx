"use client";

import { useEffect, useState } from "react";
import { X } from "lucide-react";
import { useTranslations } from "next-intl";
import { useUpdateManager } from "@/hooks/useManagers";
import { ManagerSummary } from "@/lib/types/manager";

const IDENTITY_DOCUMENT_TYPES = [
  { value: "CC",  label: "CC" },
  { value: "CE",  label: "CE" },
  { value: "TI",  label: "TI" },
  { value: "PP",  label: "PP" },
  { value: "NIT", label: "NIT" },
];

// E.164: +<country code><number>, 8-20 chars total
const PHONE_REGEX = /^\+[1-9]\d{6,19}$/;

interface Props {
  manager: ManagerSummary;
  onClose: () => void;
  onUpdated: (updated: ManagerSummary) => void;
}

export default function EditManagerModal({ manager, onClose, onUpdated }: Props) {
  const t = useTranslations("managers");
  const tValidation = useTranslations("validation");

  const { update, loading, error, clearError } = useUpdateManager();

  const [form, setForm] = useState({
    firstName:            manager.firstName ?? "",
    lastName:             manager.lastName ?? "",
    email:                manager.email,
    identityDocumentType: manager.identityDocumentType,
    identityNumber:       manager.identityNumber,
    phoneNumber:          manager.phoneNumber ?? "",
  });
  const [phoneError, setPhoneError] = useState<string | null>(null);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [onClose]);

  function set(field: keyof typeof form, value: string) {
    clearError();
    if (field === "phoneNumber") setPhoneError(null);
    setForm((f) => ({ ...f, [field]: value }));
  }

  function validatePhone(): boolean {
    const phone = form.phoneNumber.trim();
    if (!phone) {
      setPhoneError(tValidation("phone.required"));
      return false;
    }
    if (!PHONE_REGEX.test(phone)) {
      setPhoneError(tValidation("phone.invalid"));
      return false;
    }
    return true;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!validatePhone()) return;
    try {
      const updated = await update(manager.id, {
        firstName:            form.firstName || undefined,
        lastName:             form.lastName || undefined,
        email:                form.email,
        identityDocumentType: form.identityDocumentType,
        identityNumber:       form.identityNumber,
        phoneNumber:          form.phoneNumber.trim() || undefined,
      });
      onUpdated(updated);
    } catch {
      // error surfaced via hook
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={onClose} aria-hidden="true" />
      <div className="relative bg-white rounded-xl shadow-2xl w-full max-w-md">
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <div>
            <h2 className="text-lg font-semibold text-gray-900">{t("editTitle")}</h2>
            <p className="text-xs text-gray-500 mt-0.5">{manager.tenantName}</p>
          </div>
          <button onClick={onClose} className="p-1 text-gray-400 hover:text-gray-600 rounded transition-colors" aria-label="Close">
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">
          {error && (
            <div className="rounded-md bg-red-50 border border-red-200 p-3 text-sm text-red-700">{error}</div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t("formFirstNameEditLabel")}</label>
              <input type="text" value={form.firstName} onChange={(e) => set("firstName", e.target.value)}
                maxLength={100}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t("formLastNameEditLabel")}</label>
              <input type="text" value={form.lastName} onChange={(e) => set("lastName", e.target.value)}
                maxLength={100}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              {t("formEmailLabel")}
            </label>
            <input type="email" value={form.email} onChange={(e) => set("email", e.target.value)}
              required
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>

          {/* Phone */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              {t("formPhoneLabel")}
            </label>
            <input
              type="tel" value={form.phoneNumber} onChange={(e) => set("phoneNumber", e.target.value)}
              placeholder={t("formPhonePlaceholder")} maxLength={20}
              className={`w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${phoneError ? "border-red-500" : "border-gray-300"}`}
            />
            {phoneError && <p className="mt-1 text-xs text-red-600">{phoneError}</p>}
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {t("formDocTypeLabel")}
              </label>
              <select value={form.identityDocumentType} onChange={(e) => set("identityDocumentType", e.target.value)}
                required
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                {IDENTITY_DOCUMENT_TYPES.map((dt) => (
                  <option key={dt.value} value={dt.value}>{dt.value}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {t("formIdNumberLabel")}
              </label>
              <input type="text" value={form.identityNumber} onChange={(e) => set("identityNumber", e.target.value)}
                required minLength={3} maxLength={30}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition-colors">
              {t("formCancelButton")}
            </button>
            <button type="submit" disabled={loading}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
              {loading ? t("formSavingButton") : t("formSaveButton")}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
