"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import { TenantDetail } from "@/lib/types/tenant";
import { Country } from "@/lib/countries";
import LogoUpload from "./LogoUpload";
import CountrySelect from "./CountrySelect";

interface FieldErrors {
  name?: string;
  discipline?: string;
  language?: string;
  slug?: string;
  contactEmail?: string;
  contactPhone?: string;
  contactStreet?: string;
  contactCity?: string;
  contactState?: string;
  contactCountry?: string;
  logo?: string;
  [key: string]: string | undefined;
}

function slugify(value: string): string {
  return value
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
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
  const [discipline, setDiscipline] = useState("");
  const [language, setLanguage] = useState("");
  const [slug, setSlug] = useState("");
  const [contactEmail, setContactEmail] = useState("");
  const [contactPhone, setContactPhone] = useState("");
  const [contactStreet, setContactStreet] = useState("");
  const [contactCity, setContactCity] = useState("");
  const [contactState, setContactState] = useState("");
  const [selectedCountry, setSelectedCountry] = useState<Country | null>(null);
  const [logo, setLogo] = useState<File | null>(null);

  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [apiError, setApiError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const slugPreview = slug || slugify(name);

  function validate(): FieldErrors {
    const errors: FieldErrors = {};

    if (!name.trim()) errors.name = "Name is required.";
    if (!discipline.trim()) errors.discipline = "Discipline is required.";
    if (!language) errors.language = "Language is required.";
    if (!slugPreview.trim()) errors.slug = "Slug is required.";

    if (!contactEmail.trim()) {
      errors.contactEmail = "Contact email is required.";
    } else if (!validateEmail(contactEmail.trim())) {
      errors.contactEmail = "Enter a valid email address.";
    }

    if (!selectedCountry) {
      errors.contactCountry = "Country is required.";
    }

    if (!contactPhone.trim()) {
      errors.contactPhone = "Contact phone is required.";
    } else if (!/^\d+$/.test(contactPhone.trim())) {
      errors.contactPhone = "Phone must contain digits only.";
    }

    if (!contactStreet.trim()) errors.contactStreet = "Street address is required.";
    if (!contactCity.trim()) errors.contactCity = "City is required.";
    if (!contactState.trim()) errors.contactState = "State is required.";

    if (!logo) errors.logo = "Logo is required.";

    return errors;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();

    const errors = validate();
    setFieldErrors(errors);
    setApiError(null);

    if (Object.keys(errors).length > 0) return;

    setSubmitting(true);

    try {
      const formData = new FormData();
      formData.append("name", name.trim());
      formData.append("discipline", discipline.trim());
      formData.append("language", language);
      formData.append("slug", slugPreview.trim());
      formData.append("contactEmail", contactEmail.trim());
      formData.append("contactPhone", contactPhone.trim());
      formData.append("contactPhoneIndicator", selectedCountry!.dialCode);
      formData.append("contactStreet", contactStreet.trim());
      formData.append("contactCity", contactCity.trim());
      formData.append("contactState", contactState.trim());
      formData.append("contactCountry", selectedCountry!.name);
      formData.append("logo", logo!);

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

  const inputClass = (error?: string) =>
    `block w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
      error ? "border-red-500" : "border-gray-300"
    }`;

  return (
    <form onSubmit={handleSubmit} className="max-w-2xl space-y-6" noValidate>
      {apiError && (
        <div className="rounded-md bg-red-50 p-4 text-sm text-red-700 border border-red-200" role="alert">
          {apiError}
        </div>
      )}

      {/* Name */}
      <div>
        <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
          Name <span className="text-red-500">*</span>
        </label>
        <input
          id="name"
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className={inputClass(fieldErrors.name)}
          placeholder="e.g. Liga Antioque&ntilde;a de Tenis"
        />
        {fieldErrors.name && <p className="mt-1 text-sm text-red-600">{fieldErrors.name}</p>}
      </div>

      {/* Discipline */}
      <div>
        <label htmlFor="discipline" className="block text-sm font-medium text-gray-700 mb-1">
          Discipline <span className="text-red-500">*</span>
        </label>
        <input
          id="discipline"
          type="text"
          value={discipline}
          onChange={(e) => setDiscipline(e.target.value)}
          className={inputClass(fieldErrors.discipline)}
          placeholder="e.g. Tennis"
        />
        {fieldErrors.discipline && <p className="mt-1 text-sm text-red-600">{fieldErrors.discipline}</p>}
      </div>

      {/* Language */}
      <div>
        <label htmlFor="language" className="block text-sm font-medium text-gray-700 mb-1">
          Language <span className="text-red-500">*</span>
        </label>
        <select
          id="language"
          value={language}
          onChange={(e) => setLanguage(e.target.value)}
          className={inputClass(fieldErrors.language)}
        >
          <option value="" disabled>Select language...</option>
          <option value="es">Spanish</option>
          <option value="en">English</option>
        </select>
        {fieldErrors.language && <p className="mt-1 text-sm text-red-600">{fieldErrors.language}</p>}
      </div>

      {/* Slug */}
      <div>
        <label htmlFor="slug" className="block text-sm font-medium text-gray-700 mb-1">
          Slug <span className="text-red-500">*</span>{" "}
          <span className="text-xs text-gray-400">(auto-filled from name)</span>
        </label>
        <input
          id="slug"
          type="text"
          value={slug}
          onChange={(e) => setSlug(e.target.value)}
          className={inputClass(fieldErrors.slug)}
          placeholder={slugify(name) || "auto-generated from name"}
        />
        {slugPreview && (
          <p className="mt-1 text-xs text-gray-500">
            Preview: <span data-testid="slug-preview" className="font-mono">{slugPreview}</span>
          </p>
        )}
        {fieldErrors.slug && <p className="mt-1 text-sm text-red-600">{fieldErrors.slug}</p>}
      </div>

      {/* Contact Email */}
      <div>
        <label htmlFor="contactEmail" className="block text-sm font-medium text-gray-700 mb-1">
          Contact Email <span className="text-red-500">*</span>
        </label>
        <input
          id="contactEmail"
          type="email"
          value={contactEmail}
          onChange={(e) => setContactEmail(e.target.value)}
          className={inputClass(fieldErrors.contactEmail)}
          placeholder="contact@league.com"
        />
        {fieldErrors.contactEmail && <p className="mt-1 text-sm text-red-600">{fieldErrors.contactEmail}</p>}
      </div>

      {/* Contact Address block */}
      <fieldset className="rounded-lg border border-gray-200 p-4 space-y-4">
        <legend className="text-sm font-semibold text-gray-700 px-1">
          Contact Address <span className="text-red-500">*</span>
        </legend>

        {/* Street */}
        <div>
          <label htmlFor="contactStreet" className="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">
            Street Address <span className="text-red-500">*</span>
          </label>
          <input
            id="contactStreet"
            type="text"
            value={contactStreet}
            onChange={(e) => setContactStreet(e.target.value)}
            className={inputClass(fieldErrors.contactStreet)}
            placeholder="e.g. Calle 50 #45-12"
          />
          {fieldErrors.contactStreet && <p className="mt-1 text-sm text-red-600">{fieldErrors.contactStreet}</p>}
        </div>

        {/* City + State */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="contactCity" className="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">
              City <span className="text-red-500">*</span>
            </label>
            <input
              id="contactCity"
              type="text"
              value={contactCity}
              onChange={(e) => setContactCity(e.target.value)}
              className={inputClass(fieldErrors.contactCity)}
              placeholder="e.g. Medell&iacute;n"
            />
            {fieldErrors.contactCity && <p className="mt-1 text-sm text-red-600">{fieldErrors.contactCity}</p>}
          </div>
          <div>
            <label htmlFor="contactState" className="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">
              State / Dept. <span className="text-red-500">*</span>
            </label>
            <input
              id="contactState"
              type="text"
              value={contactState}
              onChange={(e) => setContactState(e.target.value)}
              className={inputClass(fieldErrors.contactState)}
              placeholder="e.g. Antioquia"
            />
            {fieldErrors.contactState && <p className="mt-1 text-sm text-red-600">{fieldErrors.contactState}</p>}
          </div>
        </div>

        {/* Country */}
        <div>
          <label className="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">
            Country <span className="text-red-500">*</span>
          </label>
          <CountrySelect
            value={selectedCountry}
            onChange={setSelectedCountry}
            error={fieldErrors.contactCountry}
          />
        </div>
      </fieldset>

      {/* Contact Phone — disabled until country selected */}
      <div>
        <label htmlFor="contactPhone" className="block text-sm font-medium text-gray-700 mb-1">
          Contact Phone <span className="text-red-500">*</span>
        </label>
        <div className="flex rounded-md border border-gray-300 shadow-sm overflow-hidden focus-within:ring-2 focus-within:ring-blue-500">
          <span className="inline-flex items-center px-3 bg-gray-50 border-r border-gray-300 text-sm font-semibold text-gray-600 select-none">
            {selectedCountry ? `+${selectedCountry.dialCode}` : "—"}
          </span>
          <input
            id="contactPhone"
            type="tel"
            value={contactPhone}
            onChange={(e) => setContactPhone(e.target.value.replace(/\D/g, ""))}
            disabled={!selectedCountry}
            className="flex-1 px-3 py-2 text-sm outline-none disabled:bg-gray-50 disabled:text-gray-400"
            placeholder={selectedCountry ? "3001234567" : "Select a country first"}
          />
        </div>
        {!selectedCountry && (
          <p className="mt-1 text-xs text-gray-400">Select a country to enable this field.</p>
        )}
        {fieldErrors.contactPhone && <p className="mt-1 text-sm text-red-600">{fieldErrors.contactPhone}</p>}
      </div>

      {/* Logo */}
      <LogoUpload onFileSelect={setLogo} error={fieldErrors.logo} required />

      {/* Submit */}
      <div className="pt-2">
        <button
          type="submit"
          disabled={submitting}
          className="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {submitting ? "Creating..." : "Create Tenant"}
        </button>
      </div>
    </form>
  );
}
