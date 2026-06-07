"use client";

import { FormEvent, useState, useMemo } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { api, ApiError } from "@/lib/api";
import {
  StudentDetail,
  CreateStudentRequest,
  BLOOD_TYPES,
} from "@/lib/types/student";
import type { IdentityDocumentType } from "@/lib/types/identity";
import DocumentFields from "@/components/common/DocumentFields";
import { Input, Select, Button } from "@/components/ui";

interface FieldErrors {
  [key: string]: string | undefined;
}

interface StudentFormProps {
  student?: StudentDetail;
  mode?: "admin" | "self";
  onSubmit?: (payload: CreateStudentRequest) => Promise<void>;
  submitLabel?: string;
}

const EMAIL_REGEX = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

function calculateAge(dateOfBirth: string): number {
  const dob = new Date(dateOfBirth);
  const today = new Date();
  let age = today.getFullYear() - dob.getFullYear();
  const monthDiff = today.getMonth() - dob.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < dob.getDate())) {
    age--;
  }
  return age;
}

export default function StudentForm({ student, mode = "admin", onSubmit, submitLabel }: StudentFormProps) {
  const router = useRouter();
  const t = useTranslations("students");
  const tValidation = useTranslations("validation");
  const tCommon = useTranslations("common");
  const isEdit = !!student;

  const [firstName, setFirstName] = useState(student?.firstName ?? "");
  const [lastName, setLastName] = useState(student?.lastName ?? "");
  const [email, setEmail] = useState(student?.email ?? "");
  const [dateOfBirth, setDateOfBirth] = useState(student?.dateOfBirth ?? "");
  const [eps, setEps] = useState(student?.eps ?? "");
  const [identityNumber, setIdentityNumber] = useState(student?.identityNumber ?? "");
  const [identityDocumentType, setIdentityDocumentType] = useState<IdentityDocumentType>(
    (student?.identityDocumentType as IdentityDocumentType) ?? "CC"
  );
  const [bloodType, setBloodType] = useState(student?.bloodType ?? "");
  const [phone, setPhone] = useState(student?.phone ?? "");
  const [tutorFirstName, setTutorFirstName] = useState(student?.tutorFirstName ?? "");
  const [tutorLastName, setTutorLastName] = useState(student?.tutorLastName ?? "");
  const [tutorRelationship, setTutorRelationship] = useState(student?.tutorRelationship ?? "");
  const [tutorPhone, setTutorPhone] = useState(student?.tutorPhone ?? "");
  const [tutorEmail, setTutorEmail] = useState(student?.tutorEmail ?? "");

  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [apiError, setApiError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [selfSuccess, setSelfSuccess] = useState(false);

  const isMinor = useMemo(() => {
    if (!dateOfBirth) return false;
    return calculateAge(dateOfBirth) < 18;
  }, [dateOfBirth]);

  function validate(): FieldErrors {
    const errors: FieldErrors = {};

    if (!firstName.trim()) errors.firstName = tValidation("firstName.required");
    else if (firstName.trim().length > 100) errors.firstName = tValidation("firstName.maxLength");

    if (!lastName.trim()) errors.lastName = tValidation("lastName.required");
    else if (lastName.trim().length > 100) errors.lastName = tValidation("lastName.maxLength");

    if (!email.trim()) errors.email = tValidation("email.required");
    else if (!EMAIL_REGEX.test(email.trim())) errors.email = tValidation("email.invalid");
    else if (email.trim().length > 255) errors.email = tValidation("email.maxLength");

    if (!dateOfBirth) errors.dateOfBirth = tValidation("dateOfBirth.required");

    if (!eps.trim()) errors.eps = tValidation("eps.required");
    else if (eps.trim().length > 100) errors.eps = tValidation("eps.maxLength");

    if (!identityNumber.trim()) errors.identityNumber = tValidation("identityNumber.required");
    else if (identityNumber.trim().length > 30) errors.identityNumber = tValidation("identityNumber.maxLength");

    if (!identityDocumentType) errors.identityDocumentType = tValidation("documentType.required");

    if (!phone.trim()) errors.phone = tValidation("phone.required");

    if (isMinor) {
      if (!tutorFirstName.trim()) errors.tutorFirstName = tValidation("tutorFirstName.required");
      if (!tutorLastName.trim()) errors.tutorLastName = tValidation("tutorLastName.required");
      if (!tutorRelationship.trim()) errors.tutorRelationship = tValidation("tutorRelationship.required");
      if (!tutorPhone.trim()) errors.tutorPhone = tValidation("tutorPhone.required");
    }

    if (tutorEmail.trim() && !EMAIL_REGEX.test(tutorEmail.trim())) {
      errors.tutorEmail = tValidation("tutorEmail.invalid");
    }

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
      const body: CreateStudentRequest = {
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim(),
        dateOfBirth,
        eps: eps.trim(),
        identityNumber: identityNumber.trim(),
        identityDocumentType,
        bloodType: bloodType || null,
        phone: phone.trim(),
        tutorFirstName: tutorFirstName?.trim() || null,
        tutorLastName: tutorLastName?.trim() || null,
        tutorRelationship: tutorRelationship?.trim() || null,
        tutorPhone: tutorPhone?.trim() || null,
        tutorEmail: tutorEmail?.trim() || null,
      };

      if (mode === "self") {
        await onSubmit!(body);
        setSelfSuccess(true);
        return;
      }

      // admin path: isEdit → put, else → post + redirect
      if (isEdit) {
        await api.put<StudentDetail>(`/students/${student!.id}`, body);
        router.push(`/students/${student!.id}`);
      } else {
        const created = await api.post<StudentDetail>("/students", body);
        router.push(`/students/${created.id}`);
      }
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
        setApiError(err instanceof Error ? err.message : tCommon("unexpectedError"));
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (mode === "self" && selfSuccess) {
    return (
      <div className="rounded-md bg-green-50 border border-green-200 p-6 text-center" role="status">
        <h2 className="text-lg font-semibold text-green-800 mb-2">{t("selfSuccessTitle")}</h2>
        <p className="text-sm text-green-700">{t("selfSuccessMessage")}</p>
        <a href="/login" className="mt-4 inline-block text-sm text-k-volt">{t("goToLogin")}</a>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="max-w-3xl space-y-8" noValidate>
      {apiError && (
        <div className="rounded-md bg-red-50 p-4 text-sm text-red-700 border border-red-200" role="alert">
          {apiError}
        </div>
      )}

      {/* Personal Information */}
      <fieldset>
        <legend className="text-base font-semibold text-gray-900 mb-4">{t("formPersonalInfoLegend")}</legend>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            label={t("formFirstNameLabel")}
            type="text"
            value={firstName}
            onChange={(e) => setFirstName(e.target.value)}
            placeholder={t("formFirstNamePlaceholder")}
            error={fieldErrors.firstName}
          />

          <Input
            label={t("formLastNameLabel")}
            type="text"
            value={lastName}
            onChange={(e) => setLastName(e.target.value)}
            placeholder={t("formLastNamePlaceholder")}
            error={fieldErrors.lastName}
          />

          <Input
            label={t("formEmailLabel")}
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder={t("formEmailPlaceholder")}
            error={fieldErrors.email}
          />

          <Input
            label={t("formPhoneLabel")}
            type="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder={t("formPhonePlaceholder")}
            error={fieldErrors.phone}
          />

          <div>
            <label htmlFor="dateOfBirth" className="block text-sm font-medium text-k-subtle mb-1">
              {t("formDateOfBirthLabel")}
            </label>
            {/* TODO: no primitive for type="date" */}
            <input
              id="dateOfBirth"
              type="date"
              value={dateOfBirth}
              onChange={(e) => setDateOfBirth(e.target.value)}
              className="bg-k-surface border border-k-border rounded-k-sm px-3 py-2 text-sm focus:border-k-volt focus:outline-none"
              max={new Date().toISOString().split("T")[0]}
            />
            {dateOfBirth && (
              <p className="mt-1 text-xs text-k-muted">
                {t("formAgeInfo", { age: calculateAge(dateOfBirth), minor: isMinor ? t("formAgeMinor") : "" })}
              </p>
            )}
            {fieldErrors.dateOfBirth && <p className="mt-1 text-sm text-red-600">{fieldErrors.dateOfBirth}</p>}
          </div>

          <Input
            label={t("formEpsLabel")}
            type="text"
            value={eps}
            onChange={(e) => setEps(e.target.value)}
            placeholder={t("formEpsPlaceholder")}
            error={fieldErrors.eps}
          />
        </div>
      </fieldset>

      {/* Identity & Health */}
      <fieldset>
        <legend className="text-base font-semibold text-gray-900 mb-4">{t("formIdentityHealthLegend")}</legend>
        <div className="space-y-4">
          <DocumentFields
            documentType={identityDocumentType}
            documentNumber={identityNumber}
            onDocumentTypeChange={setIdentityDocumentType}
            onDocumentNumberChange={setIdentityNumber}
            errors={{
              documentType: fieldErrors.identityDocumentType,
              documentNumber: fieldErrors.identityNumber,
            }}
            disabled={submitting}
          />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Select
              label={t("formBloodTypeLabel")}
              value={bloodType}
              onChange={(e) => setBloodType(e.target.value)}
            >
              <option value="">{t("formBloodTypePlaceholder")}</option>
              {BLOOD_TYPES.map((bt) => (
                <option key={bt} value={bt}>{bt}</option>
              ))}
            </Select>
          </div>
        </div>
      </fieldset>

      {/* Tutor Information (conditional) */}
      {isMinor && (
        <fieldset>
          <legend className="text-base font-semibold text-gray-900 mb-1">{t("formTutorInfoLegend")}</legend>
          <p className="text-sm text-amber-600 mb-4">{t("formTutorInfoNote")}</p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label={t("formTutorFirstNameLabel")}
              type="text"
              value={tutorFirstName}
              onChange={(e) => setTutorFirstName(e.target.value)}
              error={fieldErrors.tutorFirstName}
            />

            <Input
              label={t("formTutorLastNameLabel")}
              type="text"
              value={tutorLastName}
              onChange={(e) => setTutorLastName(e.target.value)}
              error={fieldErrors.tutorLastName}
            />

            <Input
              label={t("formTutorRelationshipLabel")}
              type="text"
              value={tutorRelationship}
              onChange={(e) => setTutorRelationship(e.target.value)}
              placeholder={t("formTutorRelationshipPlaceholder")}
              error={fieldErrors.tutorRelationship}
            />

            <Input
              label={t("formTutorPhoneLabel")}
              type="tel"
              value={tutorPhone}
              onChange={(e) => setTutorPhone(e.target.value)}
              placeholder={t("formTutorPhonePlaceholder")}
              error={fieldErrors.tutorPhone}
            />

            <div className="sm:col-span-2">
              <Input
                label={t("formTutorEmailLabel")}
                type="email"
                value={tutorEmail}
                onChange={(e) => setTutorEmail(e.target.value)}
                placeholder={t("formTutorEmailPlaceholder")}
                error={fieldErrors.tutorEmail}
              />
            </div>
          </div>
        </fieldset>
      )}

      {/* Submit */}
      <div className="pt-2">
        <Button variant="volt" type="submit" disabled={submitting}>
          {submitting
            ? (isEdit ? tCommon("saving") : tCommon("creating"))
            : (submitLabel ?? (isEdit ? t("formSaveButton") : t("formCreateButton")))}
        </Button>
      </div>
    </form>
  );
}
