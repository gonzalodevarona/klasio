"use client";

import { useEffect, useState } from "react";
import { X } from "lucide-react";
import { useUpdateProfessor } from "@/hooks/useProfessors";
import { ProfessorSummary } from "@/lib/types/professor";
import type { IdentityDocumentType } from "@/lib/types/identity";

const IDENTITY_DOCUMENT_TYPES = [
  { value: "CC",  label: "CC" },
  { value: "CE",  label: "CE" },
  { value: "TI",  label: "TI" },
  { value: "PP",  label: "PP" },
  { value: "NIT", label: "NIT" },
];

const EMAIL_REGEX = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
const PHONE_REGEX = /^\+[1-9]\d{6,19}$/;

interface FieldErrors {
  email?: string;
  phoneNumber?: string;
  identityNumber?: string;
}

interface Props {
  professor: ProfessorSummary;
  onClose: () => void;
  onUpdated: () => void;
}

export default function EditProfessorModal({ professor, onClose, onUpdated }: Props) {
  const { update, loading, error, clearError } = useUpdateProfessor();

  const [form, setForm] = useState({
    firstName:            professor.firstName,
    lastName:             professor.lastName,
    email:                professor.email,
    phoneNumber:          professor.phoneNumber ?? "",
    identityDocumentType: professor.identityDocumentType,
    identityNumber:       professor.identityNumber,
  });
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [onClose]);

  function set(field: keyof typeof form, value: string) {
    clearError();
    setFieldErrors((prev) => ({ ...prev, [field]: undefined }));
    setForm((f) => ({ ...f, [field]: value }));
  }

  function validate(): FieldErrors {
    const errors: FieldErrors = {};
    if (!form.email.trim()) {
      errors.email = "Email is required.";
    } else if (!EMAIL_REGEX.test(form.email.trim())) {
      errors.email = "Enter a valid email address.";
    }
    const phone = form.phoneNumber.trim();
    if (!phone) {
      errors.phoneNumber = "Phone number is required.";
    } else if (!PHONE_REGEX.test(phone)) {
      errors.phoneNumber = "Enter a valid WhatsApp number in E.164 format, e.g. +573001234567";
    }
    if (!form.identityNumber.trim()) errors.identityNumber = "Document number is required.";
    return errors;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    try {
      await update(professor.id, {
        firstName:            form.firstName,
        lastName:             form.lastName,
        email:                form.email.trim(),
        phoneNumber:          form.phoneNumber.trim() || undefined,
        identityDocumentType: form.identityDocumentType as IdentityDocumentType,
        identityNumber:       form.identityNumber.trim(),
      });
      onUpdated();
    } catch {
      // error surfaced via hook
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={onClose} aria-hidden="true" />
      <div className="relative bg-white rounded-xl shadow-2xl w-full max-w-md max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 sticky top-0 bg-white z-10">
          <h2 className="text-lg font-semibold text-gray-900">Edit Professor</h2>
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
              <label className="block text-sm font-medium text-gray-700 mb-1">First Name</label>
              <input type="text" value={form.firstName} onChange={(e) => set("firstName", e.target.value)}
                maxLength={100}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Last Name</label>
              <input type="text" value={form.lastName} onChange={(e) => set("lastName", e.target.value)}
                maxLength={100}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Email <span className="text-red-500">*</span>
            </label>
            <input type="email" value={form.email} onChange={(e) => set("email", e.target.value)}
              required
              className={`w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${fieldErrors.email ? "border-red-500" : "border-gray-300"}`} />
            {fieldErrors.email && <p className="mt-1 text-xs text-red-600">{fieldErrors.email}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Phone (WhatsApp) <span className="text-red-500">*</span>
            </label>
            <input type="tel" value={form.phoneNumber} onChange={(e) => set("phoneNumber", e.target.value)}
              placeholder="+573001234567" maxLength={20}
              className={`w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${fieldErrors.phoneNumber ? "border-red-500" : "border-gray-300"}`} />
            {fieldErrors.phoneNumber && <p className="mt-1 text-xs text-red-600">{fieldErrors.phoneNumber}</p>}
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Doc. Type <span className="text-red-500">*</span>
              </label>
              <select value={form.identityDocumentType} onChange={(e) => set("identityDocumentType", e.target.value)}
                required
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                {IDENTITY_DOCUMENT_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>{t.value}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                ID Number <span className="text-red-500">*</span>
              </label>
              <input type="text" value={form.identityNumber} onChange={(e) => set("identityNumber", e.target.value)}
                required minLength={3} maxLength={30}
                className={`w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${fieldErrors.identityNumber ? "border-red-500" : "border-gray-300"}`} />
              {fieldErrors.identityNumber && <p className="mt-1 text-xs text-red-600">{fieldErrors.identityNumber}</p>}
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition-colors">
              Cancel
            </button>
            <button type="submit" disabled={loading}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
              {loading ? "Saving..." : "Save Changes"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
