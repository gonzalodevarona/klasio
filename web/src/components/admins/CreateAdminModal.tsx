"use client";

import { useEffect, useState } from "react";
import { X } from "lucide-react";
import { useCreateAdmin, useTenantOptions } from "@/hooks/useAdmins";
import { CreateAdminRequest } from "@/lib/types/admin";

const IDENTITY_DOCUMENT_TYPES = [
  { value: "CC",  label: "Cédula de Ciudadanía (CC)" },
  { value: "CE",  label: "Cédula de Extranjería (CE)" },
  { value: "TI",  label: "Tarjeta de Identidad (TI)" },
  { value: "PP",  label: "Pasaporte (PP)" },
  { value: "NIT", label: "NIT" },
];

const PHONE_REGEX = /^\+[1-9]\d{6,19}$/;

interface Props {
  onClose: () => void;
  onCreated: () => void;
  defaultTenantId?: string;
}

export default function CreateAdminModal({ onClose, onCreated, defaultTenantId }: Props) {
  const { options: tenantOptions, loading: loadingTenants } = useTenantOptions();
  const { create, loading, error, clearError } = useCreateAdmin();

  const [form, setForm] = useState<CreateAdminRequest>({
    tenantId:             defaultTenantId ?? "",
    email:                "",
    password:             "",
    identityDocumentType: "CC",
    identityNumber:       "",
    firstName:            "",
    lastName:             "",
    phoneNumber:          "",
  });
  const [phoneError, setPhoneError] = useState<string | null>(null);

  useEffect(() => {
    if (!form.tenantId && !defaultTenantId) {
      const firstId = Object.keys(tenantOptions)[0];
      if (firstId) setForm((f) => ({ ...f, tenantId: firstId }));
    }
  }, [tenantOptions, defaultTenantId, form.tenantId]);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [onClose]);

  function set(field: keyof CreateAdminRequest, value: string) {
    clearError();
    if (field === "phoneNumber") setPhoneError(null);
    setForm((f) => ({ ...f, [field]: value }));
  }

  function validatePhone(): boolean {
    if (!form.phoneNumber.trim()) {
      setPhoneError("Phone number is required.");
      return false;
    }
    if (!PHONE_REGEX.test(form.phoneNumber.trim())) {
      setPhoneError("Enter a valid WhatsApp number in E.164 format, e.g. +573001234567");
      return false;
    }
    return true;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!validatePhone()) return;
    try {
      await create(form);
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
          <h2 className="text-lg font-semibold text-gray-900">Create Admin User</h2>
          <button onClick={onClose} className="p-1 text-gray-400 hover:text-gray-600 rounded transition-colors" aria-label="Close">
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">
          {error && (
            <div className="rounded-md bg-red-50 border border-red-200 p-3 text-sm text-red-700">{error}</div>
          )}

          {/* Tenant */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Tenant (League) <span className="text-red-500">*</span>
            </label>
            <select
              value={form.tenantId}
              onChange={(e) => set("tenantId", e.target.value)}
              required
              disabled={loadingTenants || !!defaultTenantId}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
            >
              <option value="">Select a tenant...</option>
              {Object.entries(tenantOptions).map(([id, name]) => (
                <option key={id} value={id}>{name}</option>
              ))}
            </select>
          </div>

          {/* Name */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                First Name <span className="text-red-500">*</span>
              </label>
              <input type="text" value={form.firstName} onChange={(e) => set("firstName", e.target.value)}
                required maxLength={100} placeholder="John"
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Last Name <span className="text-red-500">*</span>
              </label>
              <input type="text" value={form.lastName} onChange={(e) => set("lastName", e.target.value)}
                required maxLength={100} placeholder="Doe"
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
          </div>

          {/* Email */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Email <span className="text-red-500">*</span>
            </label>
            <input type="email" value={form.email} onChange={(e) => set("email", e.target.value)}
              required placeholder="admin@example.com"
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>

          {/* Phone */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Phone (WhatsApp) <span className="text-red-500">*</span>
            </label>
            <input type="tel" value={form.phoneNumber} onChange={(e) => set("phoneNumber", e.target.value)}
              placeholder="+573001234567" maxLength={20}
              className={`w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${phoneError ? "border-red-500" : "border-gray-300"}`} />
            {phoneError && <p className="mt-1 text-xs text-red-600">{phoneError}</p>}
          </div>

          {/* Password */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Password <span className="text-red-500">*</span>
            </label>
            <input type="password" value={form.password} onChange={(e) => set("password", e.target.value)}
              required minLength={8} maxLength={72} placeholder="Min. 8 characters"
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>

          {/* Identity document */}
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
                required minLength={3} maxLength={30} placeholder="Document number"
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition-colors">
              Cancel
            </button>
            <button type="submit" disabled={loading || !form.tenantId}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
              {loading ? "Creating..." : "Create Admin"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
