"use client";

import { FormEvent, useState, useMemo } from "react";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import {
  StudentDetail,
  CreateStudentRequest,
  UpdateStudentRequest,
  BLOOD_TYPES,
} from "@/lib/types/student";
import type { IdentityDocumentType } from "@/lib/types/identity";
import DocumentFields from "@/components/common/DocumentFields";

interface FieldErrors {
  [key: string]: string | undefined;
}

interface StudentFormProps {
  student?: StudentDetail;
}

const EMAIL_REGEX = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
const PHONE_REGEX = /^\+[1-9]\d{6,19}$/;

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

export default function StudentForm({ student }: StudentFormProps) {
  const router = useRouter();
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

  const isMinor = useMemo(() => {
    if (!dateOfBirth) return false;
    return calculateAge(dateOfBirth) < 18;
  }, [dateOfBirth]);

  function validate(): FieldErrors {
    const errors: FieldErrors = {};

    if (!firstName.trim()) errors.firstName = "First name is required.";
    else if (firstName.trim().length > 100) errors.firstName = "First name must be at most 100 characters.";

    if (!lastName.trim()) errors.lastName = "Last name is required.";
    else if (lastName.trim().length > 100) errors.lastName = "Last name must be at most 100 characters.";

    if (!email.trim()) errors.email = "Email is required.";
    else if (!EMAIL_REGEX.test(email.trim())) errors.email = "Please enter a valid email address.";
    else if (email.trim().length > 255) errors.email = "Email must be at most 255 characters.";

    if (!dateOfBirth) errors.dateOfBirth = "Date of birth is required.";

    if (!eps.trim()) errors.eps = "EPS is required.";
    else if (eps.trim().length > 100) errors.eps = "EPS must be at most 100 characters.";

    if (!identityNumber.trim()) errors.identityNumber = "Identity number is required.";
    else if (identityNumber.trim().length > 30) errors.identityNumber = "Identity number must be at most 30 characters.";

    if (!identityDocumentType) errors.identityDocumentType = "Document type is required.";

    if (!phone.trim()) errors.phone = "Phone number is required.";
    else if (!PHONE_REGEX.test(phone.trim())) errors.phone = "Enter a valid WhatsApp number in E.164 format, e.g. +573001234567";

    if (isMinor) {
      if (!tutorFirstName.trim()) errors.tutorFirstName = "Tutor first name is required for minors.";
      if (!tutorLastName.trim()) errors.tutorLastName = "Tutor last name is required for minors.";
      if (!tutorRelationship.trim()) errors.tutorRelationship = "Tutor relationship is required for minors.";
      if (!tutorPhone.trim()) errors.tutorPhone = "Tutor phone is required for minors.";
    }

    if (tutorEmail.trim() && !EMAIL_REGEX.test(tutorEmail.trim())) {
      errors.tutorEmail = "Please enter a valid tutor email address.";
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
      const body: CreateStudentRequest | UpdateStudentRequest = {
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim(),
        dateOfBirth,
        eps: eps.trim(),
        identityNumber: identityNumber.trim(),
        identityDocumentType,
        bloodType: bloodType || null,
        phone: phone.trim(),
        tutorFirstName: tutorFirstName.trim() || null,
        tutorLastName: tutorLastName.trim() || null,
        tutorRelationship: tutorRelationship.trim() || null,
        tutorPhone: tutorPhone.trim() || null,
        tutorEmail: tutorEmail.trim() || null,
      };

      if (isEdit) {
        await api.put<StudentDetail>(`/students/${student.id}`, body);
        router.push(`/students/${student.id}`);
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
        setApiError("An unexpected error occurred. Please try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  const inputClass = (field: string) =>
    `block w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
      fieldErrors[field] ? "border-red-500" : "border-gray-300"
    }`;

  return (
    <form onSubmit={handleSubmit} className="max-w-3xl space-y-8" noValidate>
      {apiError && (
        <div className="rounded-md bg-red-50 p-4 text-sm text-red-700 border border-red-200" role="alert">
          {apiError}
        </div>
      )}

      {/* Personal Information */}
      <fieldset>
        <legend className="text-base font-semibold text-gray-900 mb-4">Personal Information</legend>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label htmlFor="firstName" className="block text-sm font-medium text-gray-700 mb-1">
              First Name <span className="text-red-500">*</span>
            </label>
            <input id="firstName" type="text" value={firstName} onChange={(e) => setFirstName(e.target.value)}
              className={inputClass("firstName")} placeholder="e.g. Maria" />
            {fieldErrors.firstName && <p className="mt-1 text-sm text-red-600">{fieldErrors.firstName}</p>}
          </div>

          <div>
            <label htmlFor="lastName" className="block text-sm font-medium text-gray-700 mb-1">
              Last Name <span className="text-red-500">*</span>
            </label>
            <input id="lastName" type="text" value={lastName} onChange={(e) => setLastName(e.target.value)}
              className={inputClass("lastName")} placeholder="e.g. Rodriguez" />
            {fieldErrors.lastName && <p className="mt-1 text-sm text-red-600">{fieldErrors.lastName}</p>}
          </div>

          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
              Email <span className="text-red-500">*</span>
            </label>
            <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)}
              className={inputClass("email")} placeholder="e.g. maria@example.com" />
            {fieldErrors.email && <p className="mt-1 text-sm text-red-600">{fieldErrors.email}</p>}
          </div>

          <div>
            <label htmlFor="phone" className="block text-sm font-medium text-gray-700 mb-1">
              Phone (WhatsApp) <span className="text-red-500">*</span>
            </label>
            <input id="phone" type="tel" value={phone} onChange={(e) => setPhone(e.target.value)}
              className={inputClass("phone")} placeholder="e.g. +573001234567" />
            {fieldErrors.phone && <p className="mt-1 text-sm text-red-600">{fieldErrors.phone}</p>}
          </div>

          <div>
            <label htmlFor="dateOfBirth" className="block text-sm font-medium text-gray-700 mb-1">
              Date of Birth <span className="text-red-500">*</span>
            </label>
            <input id="dateOfBirth" type="date" value={dateOfBirth} onChange={(e) => setDateOfBirth(e.target.value)}
              className={inputClass("dateOfBirth")} max={new Date().toISOString().split("T")[0]} />
            {dateOfBirth && (
              <p className="mt-1 text-xs text-gray-500">
                Age: {calculateAge(dateOfBirth)} years{isMinor && " (minor)"}
              </p>
            )}
            {fieldErrors.dateOfBirth && <p className="mt-1 text-sm text-red-600">{fieldErrors.dateOfBirth}</p>}
          </div>

          <div>
            <label htmlFor="eps" className="block text-sm font-medium text-gray-700 mb-1">
              EPS <span className="text-red-500">*</span>
            </label>
            <input id="eps" type="text" value={eps} onChange={(e) => setEps(e.target.value)}
              className={inputClass("eps")} placeholder="e.g. Sura, Nueva EPS" />
            {fieldErrors.eps && <p className="mt-1 text-sm text-red-600">{fieldErrors.eps}</p>}
          </div>
        </div>
      </fieldset>

      {/* Identity & Health */}
      <fieldset>
        <legend className="text-base font-semibold text-gray-900 mb-4">Identity & Health</legend>
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
          <div>
            <label htmlFor="bloodType" className="block text-sm font-medium text-gray-700 mb-1">Blood Type</label>
            <select id="bloodType" value={bloodType} onChange={(e) => setBloodType(e.target.value)}
              className={inputClass("bloodType")}>
              <option value="">-- Select (optional) --</option>
              {BLOOD_TYPES.map((bt) => (
                <option key={bt} value={bt}>{bt}</option>
              ))}
            </select>
          </div>
          </div>
        </div>
      </fieldset>

      {/* Tutor Information (conditional) */}
      {isMinor && (
        <fieldset>
          <legend className="text-base font-semibold text-gray-900 mb-1">Tutor Information</legend>
          <p className="text-sm text-amber-600 mb-4">Required for students under 18 years of age.</p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label htmlFor="tutorFirstName" className="block text-sm font-medium text-gray-700 mb-1">
                Tutor First Name <span className="text-red-500">*</span>
              </label>
              <input id="tutorFirstName" type="text" value={tutorFirstName}
                onChange={(e) => setTutorFirstName(e.target.value)}
                className={inputClass("tutorFirstName")} />
              {fieldErrors.tutorFirstName && <p className="mt-1 text-sm text-red-600">{fieldErrors.tutorFirstName}</p>}
            </div>

            <div>
              <label htmlFor="tutorLastName" className="block text-sm font-medium text-gray-700 mb-1">
                Tutor Last Name <span className="text-red-500">*</span>
              </label>
              <input id="tutorLastName" type="text" value={tutorLastName}
                onChange={(e) => setTutorLastName(e.target.value)}
                className={inputClass("tutorLastName")} />
              {fieldErrors.tutorLastName && <p className="mt-1 text-sm text-red-600">{fieldErrors.tutorLastName}</p>}
            </div>

            <div>
              <label htmlFor="tutorRelationship" className="block text-sm font-medium text-gray-700 mb-1">
                Relationship <span className="text-red-500">*</span>
              </label>
              <input id="tutorRelationship" type="text" value={tutorRelationship}
                onChange={(e) => setTutorRelationship(e.target.value)}
                className={inputClass("tutorRelationship")} placeholder="e.g. Mother, Father, Guardian" />
              {fieldErrors.tutorRelationship && <p className="mt-1 text-sm text-red-600">{fieldErrors.tutorRelationship}</p>}
            </div>

            <div>
              <label htmlFor="tutorPhone" className="block text-sm font-medium text-gray-700 mb-1">
                Tutor Phone <span className="text-red-500">*</span>
              </label>
              <input id="tutorPhone" type="tel" value={tutorPhone}
                onChange={(e) => setTutorPhone(e.target.value)}
                className={inputClass("tutorPhone")} placeholder="e.g. 3001234567" />
              {fieldErrors.tutorPhone && <p className="mt-1 text-sm text-red-600">{fieldErrors.tutorPhone}</p>}
            </div>

            <div className="sm:col-span-2">
              <label htmlFor="tutorEmail" className="block text-sm font-medium text-gray-700 mb-1">Tutor Email</label>
              <input id="tutorEmail" type="email" value={tutorEmail}
                onChange={(e) => setTutorEmail(e.target.value)}
                className={inputClass("tutorEmail")} placeholder="e.g. tutor@example.com" />
              {fieldErrors.tutorEmail && <p className="mt-1 text-sm text-red-600">{fieldErrors.tutorEmail}</p>}
            </div>
          </div>
        </fieldset>
      )}

      {/* Submit */}
      <div className="pt-2">
        <button type="submit" disabled={submitting}
          className="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed">
          {submitting ? (isEdit ? "Saving..." : "Creating...") : (isEdit ? "Save Changes" : "Create Student")}
        </button>
      </div>
    </form>
  );
}
