"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { api, ApiError } from "@/lib/api";
import {
  ProfessorDetail,
  CreateProfessorRequest,
  UpdateProfessorRequest,
} from "@/lib/types/professor";
import type { IdentityDocumentType } from "@/lib/types/identity";
import DocumentFields from "@/components/common/DocumentFields";
import { Input, Button } from "@/components/ui";

interface FieldErrors {
  firstName?: string;
  lastName?: string;
  email?: string;
  phoneNumber?: string;
  identityDocumentType?: string;
  identityNumber?: string;
  [key: string]: string | undefined;
}

interface ProfessorFormProps {
  professor?: ProfessorDetail;
}

const EMAIL_REGEX = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

export default function ProfessorForm({ professor }: ProfessorFormProps) {
  const router = useRouter();
  const t = useTranslations("professors");
  const tValidation = useTranslations("validation");
  const tCommon = useTranslations("common");
  const isEdit = !!professor;

  const [firstName, setFirstName] = useState(professor?.firstName ?? "");
  const [lastName, setLastName] = useState(professor?.lastName ?? "");
  const [email, setEmail] = useState(professor?.email ?? "");
  const [phoneNumber, setPhoneNumber] = useState(professor?.phoneNumber ?? "");
  const [identityDocumentType, setIdentityDocumentType] = useState<IdentityDocumentType>(
    professor?.identityDocumentType ?? "CC"
  );
  const [identityNumber, setIdentityNumber] = useState(professor?.identityNumber ?? "");

  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [apiError, setApiError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function validate(): FieldErrors {
    const errors: FieldErrors = {};

    if (!firstName.trim()) {
      errors.firstName = tValidation("firstName.required");
    } else if (firstName.trim().length > 100) {
      errors.firstName = tValidation("firstName.maxLength");
    }

    if (!lastName.trim()) {
      errors.lastName = tValidation("lastName.required");
    } else if (lastName.trim().length > 100) {
      errors.lastName = tValidation("lastName.maxLength");
    }

    if (!email.trim()) {
      errors.email = tValidation("email.required");
    } else if (!EMAIL_REGEX.test(email.trim())) {
      errors.email = tValidation("email.invalid");
    } else if (email.trim().length > 255) {
      errors.email = tValidation("email.maxLength");
    }

    if (phoneNumber.trim().length > 20) {
      errors.phoneNumber = tValidation("phone.maxLength");
    }

    if (!identityNumber.trim()) {
      errors.identityNumber = tValidation("documentNumber.required");
    } else if (identityNumber.trim().length > 30) {
      errors.identityNumber = tValidation("documentNumber.maxLength");
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
      if (isEdit) {
        const body: UpdateProfessorRequest = {
          firstName: firstName.trim(),
          lastName: lastName.trim(),
          email: email.trim(),
          phoneNumber: phoneNumber.trim() || undefined,
          identityDocumentType,
          identityNumber: identityNumber.trim(),
        };
        await api.put<ProfessorDetail>(`/professors/${professor.id}`, body);
        router.push(`/professors/${professor.id}`);
      } else {
        const body: CreateProfessorRequest = {
          firstName: firstName.trim(),
          lastName: lastName.trim(),
          email: email.trim(),
          phoneNumber: phoneNumber.trim() || undefined,
          identityDocumentType,
          identityNumber: identityNumber.trim(),
        };
        const created = await api.post<ProfessorDetail>("/professors", body);
        router.push(`/professors/${created.id}`);
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
        setApiError(tCommon("unexpectedError"));
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

      {/* First Name */}
      <Input
        label={t("formFirstNameLabel")}
        type="text"
        value={firstName}
        onChange={(e) => setFirstName(e.target.value)}
        placeholder={t("formFirstNamePlaceholder")}
        error={fieldErrors.firstName}
      />

      {/* Last Name */}
      <Input
        label={t("formLastNameLabel")}
        type="text"
        value={lastName}
        onChange={(e) => setLastName(e.target.value)}
        placeholder={t("formLastNamePlaceholder")}
        error={fieldErrors.lastName}
      />

      {/* Email */}
      <Input
        label={t("formEmailLabel")}
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder={t("formEmailPlaceholder")}
        error={fieldErrors.email}
      />

      {/* Phone Number */}
      <Input
        label={t("formPhoneLabel")}
        type="tel"
        value={phoneNumber}
        onChange={(e) => setPhoneNumber(e.target.value)}
        placeholder={t("formPhonePlaceholder")}
        error={fieldErrors.phoneNumber}
      />

      {/* Identity Document */}
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

      {/* Submit */}
      <div className="pt-2">
        <Button variant="volt" type="submit" disabled={submitting}>
          {submitting
            ? isEdit
              ? t("formSavingButton")
              : t("formCreatingButton")
            : isEdit
              ? t("formSaveButton")
              : t("formCreateButton")}
        </Button>
      </div>
    </form>
  );
}
