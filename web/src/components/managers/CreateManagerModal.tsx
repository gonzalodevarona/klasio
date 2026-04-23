"use client";

import { useEffect, useState } from "react";
import { X } from "lucide-react";
import { useTranslations } from "next-intl";
import { useCreateManager, useManagerTenantOptions } from "@/hooks/useManagers";

const IDENTITY_DOCUMENT_TYPES = [
  { value: "CC",  label: "CC" },
  { value: "CE",  label: "CE" },
  { value: "TI",  label: "TI" },
  { value: "PP",  label: "PP" },
  { value: "NIT", label: "NIT" },
];


interface FormState {
  tenantId: string;
  email: string;
  identityDocumentType: string;
  identityNumber: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
}

interface Props {
  onClose: () => void;
  onCreated: () => void;
  /** When provided, the tenant selector is hidden and this value is always used. */
  defaultTenantId?: string;
}

export default function CreateManagerModal({ onClose, onCreated, defaultTenantId }: Props) {
  const t = useTranslations("managers");
  const tValidation = useTranslations("validation");

  const showTenantSelector = !defaultTenantId;
  const { options: tenantOptions, loading: loadingTenants } = useManagerTenantOptions();
  const { create, loading, error, clearError } = useCreateManager();

  const [form, setForm] = useState<FormState>({
    tenantId:             defaultTenantId ?? "",
    email:                "",
    identityDocumentType: "CC",
    identityNumber:       "",
    firstName:            "",
    lastName:             "",
    phoneNumber:          "",
  });
  const [phoneError, setPhoneError] = useState<string | null>(null);

  // Auto-select the first tenant for SUPERADMIN when the list loads.
  useEffect(() => {
    if (!showTenantSelector) return;
    if (form.tenantId) return;
    const firstId = Object.keys(tenantOptions)[0];
    if (firstId) setForm((f) => ({ ...f, tenantId: firstId }));
  }, [tenantOptions, showTenantSelector, form.tenantId]);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [onClose]);

  function set(field: keyof FormState, value: string) {
    clearError();
    if (field === "phoneNumber") setPhoneError(null);
    setForm((f) => ({ ...f, [field]: value }));
  }

  function validatePhone(): boolean {
    if (!form.phoneNumber.trim()) {
      setPhoneError(tValidation("phone.required"));
      return false;
    }
    return true;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!validatePhone()) return;
    try {
      await create({
        tenantId:             form.tenantId,
        email:                form.email,
        identityDocumentType: form.identityDocumentType,
        identityNumber:       form.identityNumber,
        firstName:            form.firstName,
        lastName:             form.lastName,
        phoneNumber:          form.phoneNumber.trim() || undefined,
        password:             "",   // signals backend to auto-generate
      });
      onCreated();
    } catch {
      // error surfaced via hook
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={onClose} aria-hidden="true" />

      <div className="relative bg-white rounded-xl shadow-2xl w-full max-w-md">
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">{t("formTitle")}</h2>
          <button onClick={onClose} className="p-1 text-gray-400 hover:text-gray-600 rounded transition-colors" aria-label="Close">
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">
          {error && (
            <div className="rounded-md bg-red-50 border border-red-200 p-3 text-sm text-red-700">{error}</div>
          )}

          {/* Tenant — only visible to SUPERADMIN */}
          {showTenantSelector && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {t("formTenantLabel")}
              </label>
              <select
                value={form.tenantId}
                onChange={(e) => set("tenantId", e.target.value)}
                required
                disabled={loadingTenants}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
              >
                <option value="">{t("formTenantPlaceholder")}</option>
                {Object.entries(tenantOptions).map(([id, name]) => (
                  <option key={id} value={id}>{name}</option>
                ))}
              </select>
            </div>
          )}

          {/* Name */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {t("formFirstNameLabel")}
              </label>
              <input
                type="text" value={form.firstName} onChange={(e) => set("firstName", e.target.value)}
                required maxLength={100} placeholder={t("formFirstNamePlaceholder")}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {t("formLastNameLabel")}
              </label>
              <input
                type="text" value={form.lastName} onChange={(e) => set("lastName", e.target.value)}
                required maxLength={100} placeholder={t("formLastNamePlaceholder")}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          {/* Email */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              {t("formEmailLabel")}
            </label>
            <input
              type="email" value={form.email} onChange={(e) => set("email", e.target.value)}
              required placeholder={t("formEmailPlaceholder")}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
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

          {/* Identity document */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {t("formDocTypeLabel")}
              </label>
              <select
                value={form.identityDocumentType} onChange={(e) => set("identityDocumentType", e.target.value)}
                required
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                {IDENTITY_DOCUMENT_TYPES.map((dt) => (
                  <option key={dt.value} value={dt.value}>{dt.value}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {t("formIdNumberLabel")}
              </label>
              <input
                type="text" value={form.identityNumber} onChange={(e) => set("identityNumber", e.target.value)}
                required minLength={3} maxLength={30} placeholder={t("formIdNumberPlaceholder")}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          <p className="text-xs text-gray-500">
            {t("formTempPasswordNote")}
          </p>

          <div className="flex justify-end gap-3 pt-1">
            <button type="button" onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition-colors">
              {t("formCancelButton")}
            </button>
            <button type="submit" disabled={loading || (showTenantSelector && !form.tenantId)}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
              {loading ? t("formCreatingButton") : t("formCreateButton")}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
