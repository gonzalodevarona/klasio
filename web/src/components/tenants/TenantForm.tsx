"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import { TenantDetail } from "@/lib/types/tenant";
import LogoUpload from "./LogoUpload";

interface FieldErrors {
  name?: string;
  sportDiscipline?: string;
  contactEmail?: string;
  logo?: string;
  [key: string]: string | undefined;
}

function slugify(value: string): string {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9\s-]/g, "")
    .replace(/[\s]+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "");
}

function validateEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

export default function TenantForm() {
  const router = useRouter();

  const [name, setName] = useState("");
  const [sportDiscipline, setSportDiscipline] = useState("");
  const [slug, setSlug] = useState("");
  const [contactEmail, setContactEmail] = useState("");
  const [contactPhone, setContactPhone] = useState("");
  const [contactAddress, setContactAddress] = useState("");
  const [logo, setLogo] = useState<File | null>(null);

  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [apiError, setApiError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const slugPreview = slug || slugify(name);

  function validate(): FieldErrors {
    const errors: FieldErrors = {};

    if (!name.trim()) {
      errors.name = "Name is required.";
    }

    if (!sportDiscipline.trim()) {
      errors.sportDiscipline = "Sport discipline is required.";
    }

    if (!contactEmail.trim()) {
      errors.contactEmail = "Contact email is required.";
    } else if (!validateEmail(contactEmail.trim())) {
      errors.contactEmail = "Enter a valid email address.";
    }

    return errors;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();

    const errors = validate();
    setFieldErrors(errors);
    setApiError(null);

    if (Object.keys(errors).length > 0) {
      return;
    }

    setSubmitting(true);

    try {
      const formData = new FormData();
      formData.append("name", name.trim());
      formData.append("sportDiscipline", sportDiscipline.trim());
      formData.append("contactEmail", contactEmail.trim());

      if (slug.trim()) {
        formData.append("slug", slug.trim());
      }
      if (contactPhone.trim()) {
        formData.append("contactPhone", contactPhone.trim());
      }
      if (contactAddress.trim()) {
        formData.append("contactAddress", contactAddress.trim());
      }
      if (logo) {
        formData.append("logo", logo);
      }

      const created = await api.postForm<TenantDetail>("/tenants", formData);
      router.push(`/tenants/${created.slug}`);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.details && err.details.length > 0) {
          const mapped: FieldErrors = {};
          for (const detail of err.details) {
            mapped[detail.field] = detail.message;
          }
          setFieldErrors(mapped);
        }
        setApiError(err.message);
      } else {
        setApiError("An unexpected error occurred. Please try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="max-w-2xl space-y-6" noValidate>
      {apiError && (
        <div
          className="rounded-md bg-red-50 p-4 text-sm text-red-700 border border-red-200"
          role="alert"
        >
          {apiError}
        </div>
      )}

      {/* Name */}
      <div>
        <label
          htmlFor="name"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Name <span className="text-red-500">*</span>
        </label>
        <input
          id="name"
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className={`block w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
            fieldErrors.name ? "border-red-500" : "border-gray-300"
          }`}
          placeholder="e.g. Liga Antioque&ntilde;a de Tenis"
        />
        {fieldErrors.name && (
          <p className="mt-1 text-sm text-red-600">{fieldErrors.name}</p>
        )}
      </div>

      {/* Sport Discipline */}
      <div>
        <label
          htmlFor="sportDiscipline"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Sport Discipline <span className="text-red-500">*</span>
        </label>
        <input
          id="sportDiscipline"
          type="text"
          value={sportDiscipline}
          onChange={(e) => setSportDiscipline(e.target.value)}
          className={`block w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
            fieldErrors.sportDiscipline ? "border-red-500" : "border-gray-300"
          }`}
          placeholder="e.g. Tennis"
        />
        {fieldErrors.sportDiscipline && (
          <p className="mt-1 text-sm text-red-600">
            {fieldErrors.sportDiscipline}
          </p>
        )}
      </div>

      {/* Slug */}
      <div>
        <label
          htmlFor="slug"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Slug <span className="text-xs text-gray-400">(optional)</span>
        </label>
        <input
          id="slug"
          type="text"
          value={slug}
          onChange={(e) => setSlug(e.target.value)}
          className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="auto-generated from name if left empty"
        />
        {slugPreview && (
          <p className="mt-1 text-xs text-gray-500">
            Preview: <span data-testid="slug-preview" className="font-mono">{slugPreview}</span>
          </p>
        )}
      </div>

      {/* Contact Email */}
      <div>
        <label
          htmlFor="contactEmail"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Contact Email <span className="text-red-500">*</span>
        </label>
        <input
          id="contactEmail"
          type="email"
          value={contactEmail}
          onChange={(e) => setContactEmail(e.target.value)}
          className={`block w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
            fieldErrors.contactEmail ? "border-red-500" : "border-gray-300"
          }`}
          placeholder="contact@league.com"
        />
        {fieldErrors.contactEmail && (
          <p className="mt-1 text-sm text-red-600">
            {fieldErrors.contactEmail}
          </p>
        )}
      </div>

      {/* Contact Phone */}
      <div>
        <label
          htmlFor="contactPhone"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Contact Phone <span className="text-xs text-gray-400">(optional)</span>
        </label>
        <input
          id="contactPhone"
          type="tel"
          value={contactPhone}
          onChange={(e) => setContactPhone(e.target.value)}
          className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="+57 300 123 4567"
        />
      </div>

      {/* Contact Address */}
      <div>
        <label
          htmlFor="contactAddress"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Contact Address <span className="text-xs text-gray-400">(optional)</span>
        </label>
        <input
          id="contactAddress"
          type="text"
          value={contactAddress}
          onChange={(e) => setContactAddress(e.target.value)}
          className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="Calle 50 #45-12, Medellin"
        />
      </div>

      {/* Logo */}
      <LogoUpload onFileSelect={setLogo} error={fieldErrors.logo} />

      {/* Submit */}
      <div className="pt-2">
        <button
          type="submit"
          disabled={submitting}
          className="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {submitting ? "Creating..." : "Create League"}
        </button>
      </div>
    </form>
  );
}
