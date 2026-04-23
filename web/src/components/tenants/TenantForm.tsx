"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
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
  const t = useTranslations("tenants.form");
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

    if (!name.trim()) errors.name = t("errorNameRequired");
    if (!discipline.trim()) errors.discipline = t("errorDisciplineRequired");
    if (!language) errors.language = t("errorLanguageRequired");
    if (!slugPreview.trim()) errors.slug = t("errorSlugRequired");

    if (!contactEmail.trim()) {
      errors.contactEmail = t("errorContactEmailRequired");
    } else if (!validateEmail(contactEmail.trim())) {
      errors.contactEmail = t("errorContactEmailInvalid");
    }

    if (!selectedCountry) {
      errors.contactCountry = t("errorCountryRequired");
    }

    if (!contactPhone.trim()) {
      errors.contactPhone = t("errorContactPhoneRequired");
    } else if (!/^\d+$/.test(contactPhone.trim())) {
      errors.contactPhone = t("errorContactPhoneInvalid");
    }

    if (!contactStreet.trim()) errors.contactStreet = t("errorStreetRequired");
    if (!contactCity.trim()) errors.contactCity = t("errorCityRequired");
    if (!contactState.trim()) errors.contactState = t("errorStateRequired");

    if (!logo) errors.logo = t("errorLogoRequired");

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
        setApiError(t("errorUnexpected"));
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

      <div>
        <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
          {t("labelName")} <span className="text-red-500">*</span>
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

      <div>
        <label htmlFor="discipline" className="block text-sm font-medium text-gray-700 mb-1">
          {t("labelDiscipline")} <span className="text-red-500">*</span>
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

      <div>
        <label htmlFor="language" className="block text-sm font-medium text-gray-700 mb-1">
          {t("labelLanguage")} <span className="text-red-500">*</span>
        </label>
        <select
          id="language"
          value={language}
          onChange={(e) => setLanguage(e.target.value)}
          className={inputClass(fieldErrors.language)}
        >
          <option value="" disabled>{t("languagePlaceholder")}</option>
          <option value="es">{t("languageSpanish")}</option>
          <option value="en">{t("languageEnglish")}</option>
        </select>
        {fieldErrors.language && <p className="mt-1 text-sm text-red-600">{fieldErrors.language}</p>}
      </div>

      <div>
        <label htmlFor="slug" className="block text-sm font-medium text-gray-700 mb-1">
          {t("labelSlug")} <span className="text-red-500">*</span>{" "}
          <span className="text-xs text-gray-400">{t("slugAutoFill")}</span>
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
            {t("slugPreview")} <span data-testid="slug-preview" className="font-mono">{slugPreview}</span>
          </p>
        )}
        {fieldErrors.slug && <p className="mt-1 text-sm text-red-600">{fieldErrors.slug}</p>}
      </div>

      <div>
        <label htmlFor="contactEmail" className="block text-sm font-medium text-gray-700 mb-1">
          {t("labelContactEmail")} <span className="text-red-500">*</span>
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

      <fieldset className="rounded-lg border border-gray-200 p-4 space-y-4">
        <legend className="text-sm font-semibold text-gray-700 px-1">
          {t("legendContactAddress")} <span className="text-red-500">*</span>
        </legend>

        <div>
          <label htmlFor="contactStreet" className="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">
            {t("labelStreet")} <span className="text-red-500">*</span>
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

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="contactCity" className="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">
              {t("labelCity")} <span className="text-red-500">*</span>
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
              {t("labelState")} <span className="text-red-500">*</span>
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

        <div>
          <label className="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">
            {t("labelCountry")} <span className="text-red-500">*</span>
          </label>
          <CountrySelect
            value={selectedCountry}
            onChange={setSelectedCountry}
            error={fieldErrors.contactCountry}
          />
        </div>
      </fieldset>

      <div>
        <label htmlFor="contactPhone" className="block text-sm font-medium text-gray-700 mb-1">
          {t("labelContactPhone")} <span className="text-red-500">*</span>
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
            placeholder={selectedCountry ? "3001234567" : t("phoneSelectCountryFirst")}
          />
        </div>
        {!selectedCountry && (
          <p className="mt-1 text-xs text-gray-400">{t("phoneSelectCountryHint")}</p>
        )}
        {fieldErrors.contactPhone && <p className="mt-1 text-sm text-red-600">{fieldErrors.contactPhone}</p>}
      </div>

      <LogoUpload onFileSelect={setLogo} error={fieldErrors.logo} required />

      <div className="pt-2">
        <button
          type="submit"
          disabled={submitting}
          className="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {submitting ? t("submitting") : t("submitButton")}
        </button>
      </div>
    </form>
  );
}
