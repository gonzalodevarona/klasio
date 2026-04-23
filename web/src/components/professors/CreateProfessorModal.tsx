"use client";

import { useEffect, useState } from "react";
import { X } from "lucide-react";
import { useTranslations } from "next-intl";
import { useCreateProfessor } from "@/hooks/useProfessors";
import type { IdentityDocumentType } from "@/lib/types/identity";

const IDENTITY_DOCUMENT_TYPES = [
  { value: "CC",  label: "CC" },
  { value: "CE",  label: "CE" },
  { value: "TI",  label: "TI" },
  { value: "PP",  label: "PP" },
  { value: "NIT", label: "NIT" },
];

const EMAIL_REGEX = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

interface FormState {
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  identityDocumentType: string;
  identityNumber: string;
}

interface FieldErrors {
  firstName?: string;
  lastName?: string;
  email?: string;
  phoneNumber?: string;
  identityNumber?: string;
}

interface Props {
  onClose: () => void;
  onCreated: () => void;
}

export default function CreateProfessorModal({ onClose, onCreated }: Props) {
  const t = useTranslations("professors");
  const tValidation = useTranslations("validation");
  const { create, loading, error, clearError } = useCreateProfessor();

  const [form, setForm] = useState<FormState>({
    firstName:            "",
    lastName:             "",
    email:                "",
    phoneNumber:          "",
    identityDocumentType: "CC",
    identityNumber:       "",
  });
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [onClose]);

  function set(field: keyof FormState, value: string) {
    clearError();
    setFieldErrors((prev) => ({ ...prev, [field]: undefined }));
    setForm((f) => ({ ...f, [field]: value }));
  }

  function validate(): FieldErrors {
    const errors: FieldErrors = {};
    if (!form.firstName.trim()) errors.firstName = tValidation("firstName.required");
    if (!form.lastName.trim())  errors.lastName  = tValidation("lastName.required");
    if (!form.email.trim()) {
      errors.email = tValidation("email.required");
    } else if (!EMAIL_REGEX.test(form.email.trim())) {
      errors.email = tValidation("email.invalid");
    }
    if (!form.phoneNumber.trim()) {
      errors.phoneNumber = tValidation("phone.required");
    }
    if (!form.identityNumber.trim()) errors.identityNumber = tValidation("documentNumber.required");
    return errors;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    try {
      await create({
        firstName:            form.firstName.trim(),
        lastName:             form.lastName.trim(),
        email:                form.email.trim(),
        phoneNumber:          form.phoneNumber.trim() || undefined,
        identityDocumentType: form.identityDocumentType as IdentityDocumentType,
        identityNumber:       form.identityNumber.trim(),
      });
      onCreated();
    } catch {
      // error surfaced via hook
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={onClose} aria-hidden="true" />

      <div className="relative bg-white rounded-xl shadow-2xl w-full max-w-md max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 sticky top-0 bg-white z-10">
          <h2 className="text-lg font-semibold text-gray-900">{t("formCreateTitle")}</h2>
          <button onClick={onClose} className="p-1 text-gray-400 hover:text-gray-600 rounded transition-colors" aria-label="Close">
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">
          {error && (
            <div className="rounded-md bg-red-50 border border-red-200 p-3 text-sm text-red-700">{error}</div>
          )}

          {/* Name */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {t("formFirstNameLabel")}
              </label>
              <input type="text" value={form.firstName} onChange={(e) => set("firstName", e.target.value)}
                required maxLength={100} placeholder={t("formFirstNamePlaceholder")}
                className={`w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${fieldErrors.firstName ? "border-red-500" : "border-gray-300"}`} />
              {fieldErrors.firstName && <p className="mt-1 text-xs text-red-600">{fieldErrors.firstName}</p>}
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {t("formLastNameLabel")}
              </label>
              <input type="text" value={form.lastName} onChange={(e) => set("lastName", e.target.value)}
                required maxLength={100} placeholder={t("formLastNamePlaceholder")}
                className={`w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${fieldErrors.lastName ? "border-red-500" : "border-gray-300"}`} />
              {fieldErrors.lastName && <p className="mt-1 text-xs text-red-600">{fieldErrors.lastName}</p>}
            </div>
          </div>

          {/* Email */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              {t("formEmailLabel")}
            </label>
            <input type="email" value={form.email} onChange={(e) => set("email", e.target.value)}
              required placeholder={t("formEmailPlaceholder")}
              className={`w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${fieldErrors.email ? "border-red-500" : "border-gray-300"}`} />
            {fieldErrors.email && <p className="mt-1 text-xs text-red-600">{fieldErrors.email}</p>}
          </div>

          {/* Phone */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              {t("formPhoneLabel")}
            </label>
            <input type="tel" value={form.phoneNumber} onChange={(e) => set("phoneNumber", e.target.value)}
              placeholder={t("formPhonePlaceholder")} maxLength={20}
              className={`w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${fieldErrors.phoneNumber ? "border-red-500" : "border-gray-300"}`} />
            {fieldErrors.phoneNumber && <p className="mt-1 text-xs text-red-600">{fieldErrors.phoneNumber}</p>}
          </div>

          {/* Identity document */}
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
                required minLength={3} maxLength={30} placeholder={t("formIdNumberPlaceholder")}
                className={`w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${fieldErrors.identityNumber ? "border-red-500" : "border-gray-300"}`} />
              {fieldErrors.identityNumber && <p className="mt-1 text-xs text-red-600">{fieldErrors.identityNumber}</p>}
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-1">
            <button type="button" onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition-colors">
              {t("formCancelButton")}
            </button>
            <button type="submit" disabled={loading}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
              {loading ? t("formCreatingButton") : t("formCreateButton")}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
