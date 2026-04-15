"use client";

import { IDENTITY_DOCUMENT_TYPES } from "@/lib/types/identity";
import type { IdentityDocumentType } from "@/lib/types/identity";

interface DocumentFieldsProps {
  documentType: IdentityDocumentType;
  documentNumber: string;
  onDocumentTypeChange: (value: IdentityDocumentType) => void;
  onDocumentNumberChange: (value: string) => void;
  errors?: {
    documentType?: string;
    documentNumber?: string;
  };
  disabled?: boolean;
  labelClassName?: string;
  inputClassName?: string;
}

export default function DocumentFields({
  documentType,
  documentNumber,
  onDocumentTypeChange,
  onDocumentNumberChange,
  errors = {},
  disabled = false,
  labelClassName = "block text-sm font-medium text-gray-700 mb-1",
  inputClassName = "block w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500",
}: DocumentFieldsProps) {
  return (
    <div className="grid grid-cols-2 gap-4">
      <div>
        <label htmlFor="identityDocumentType" className={labelClassName}>
          Document Type <span className="text-red-500">*</span>
        </label>
        <select
          id="identityDocumentType"
          value={documentType}
          onChange={(e) => onDocumentTypeChange(e.target.value as IdentityDocumentType)}
          disabled={disabled}
          className={`${inputClassName} ${
            errors.documentType ? "border-red-500" : "border-gray-300"
          }`}
        >
          {IDENTITY_DOCUMENT_TYPES.map((dt) => (
            <option key={dt.value} value={dt.value}>
              {dt.value} — {dt.label}
            </option>
          ))}
        </select>
        {errors.documentType && (
          <p className="mt-1 text-sm text-red-600">{errors.documentType}</p>
        )}
      </div>

      <div>
        <label htmlFor="identityNumber" className={labelClassName}>
          Document Number <span className="text-red-500">*</span>
        </label>
        <input
          id="identityNumber"
          type="text"
          value={documentNumber}
          onChange={(e) => onDocumentNumberChange(e.target.value)}
          disabled={disabled}
          maxLength={30}
          placeholder="e.g. 1234567890"
          className={`${inputClassName} ${
            errors.documentNumber ? "border-red-500" : "border-gray-300"
          }`}
        />
        {errors.documentNumber && (
          <p className="mt-1 text-sm text-red-600">{errors.documentNumber}</p>
        )}
      </div>
    </div>
  );
}
