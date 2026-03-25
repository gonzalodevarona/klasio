"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import {
  ProfessorDetail,
  CreateProfessorRequest,
  UpdateProfessorRequest,
} from "@/lib/types/professor";

interface FieldErrors {
  firstName?: string;
  lastName?: string;
  email?: string;
  phoneNumber?: string;
  [key: string]: string | undefined;
}

interface ProfessorFormProps {
  professor?: ProfessorDetail;
}

const EMAIL_REGEX = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

export default function ProfessorForm({ professor }: ProfessorFormProps) {
  const router = useRouter();
  const isEdit = !!professor;

  const [firstName, setFirstName] = useState(professor?.firstName ?? "");
  const [lastName, setLastName] = useState(professor?.lastName ?? "");
  const [email, setEmail] = useState(professor?.email ?? "");
  const [phoneNumber, setPhoneNumber] = useState(professor?.phoneNumber ?? "");

  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [apiError, setApiError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function validate(): FieldErrors {
    const errors: FieldErrors = {};

    if (!firstName.trim()) {
      errors.firstName = "First name is required.";
    } else if (firstName.trim().length > 100) {
      errors.firstName = "First name must be at most 100 characters.";
    }

    if (!lastName.trim()) {
      errors.lastName = "Last name is required.";
    } else if (lastName.trim().length > 100) {
      errors.lastName = "Last name must be at most 100 characters.";
    }

    if (!email.trim()) {
      errors.email = "Email is required.";
    } else if (!EMAIL_REGEX.test(email.trim())) {
      errors.email = "Please enter a valid email address.";
    } else if (email.trim().length > 255) {
      errors.email = "Email must be at most 255 characters.";
    }

    if (phoneNumber.trim().length > 20) {
      errors.phoneNumber = "Phone number must be at most 20 characters.";
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
        };
        await api.put<ProfessorDetail>(`/professors/${professor.id}`, body);
        router.push(`/professors/${professor.id}`);
      } else {
        const body: CreateProfessorRequest = {
          firstName: firstName.trim(),
          lastName: lastName.trim(),
          email: email.trim(),
          phoneNumber: phoneNumber.trim() || undefined,
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

      {/* First Name */}
      <div>
        <label
          htmlFor="firstName"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          First Name <span className="text-red-500">*</span>
        </label>
        <input
          id="firstName"
          type="text"
          value={firstName}
          onChange={(e) => setFirstName(e.target.value)}
          className={`block w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
            fieldErrors.firstName ? "border-red-500" : "border-gray-300"
          }`}
          placeholder="e.g. Carlos"
        />
        {fieldErrors.firstName && (
          <p className="mt-1 text-sm text-red-600">{fieldErrors.firstName}</p>
        )}
      </div>

      {/* Last Name */}
      <div>
        <label
          htmlFor="lastName"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Last Name <span className="text-red-500">*</span>
        </label>
        <input
          id="lastName"
          type="text"
          value={lastName}
          onChange={(e) => setLastName(e.target.value)}
          className={`block w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
            fieldErrors.lastName ? "border-red-500" : "border-gray-300"
          }`}
          placeholder="e.g. Martinez"
        />
        {fieldErrors.lastName && (
          <p className="mt-1 text-sm text-red-600">{fieldErrors.lastName}</p>
        )}
      </div>

      {/* Email */}
      <div>
        <label
          htmlFor="email"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Email <span className="text-red-500">*</span>
        </label>
        <input
          id="email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className={`block w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
            fieldErrors.email ? "border-red-500" : "border-gray-300"
          }`}
          placeholder="e.g. carlos@example.com"
        />
        {fieldErrors.email && (
          <p className="mt-1 text-sm text-red-600">{fieldErrors.email}</p>
        )}
      </div>

      {/* Phone Number */}
      <div>
        <label
          htmlFor="phoneNumber"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Phone Number{" "}
          <span className="text-gray-400 font-normal">
            (must be valid for WhatsApp)
          </span>
        </label>
        <input
          id="phoneNumber"
          type="tel"
          value={phoneNumber}
          onChange={(e) => setPhoneNumber(e.target.value)}
          className={`block w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
            fieldErrors.phoneNumber ? "border-red-500" : "border-gray-300"
          }`}
          placeholder="e.g. +573001234567"
        />
        {fieldErrors.phoneNumber && (
          <p className="mt-1 text-sm text-red-600">{fieldErrors.phoneNumber}</p>
        )}
      </div>

      {/* Submit */}
      <div className="pt-2">
        <button
          type="submit"
          disabled={submitting}
          className="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {submitting
            ? isEdit
              ? "Saving..."
              : "Creating..."
            : isEdit
              ? "Save Changes"
              : "Create Professor"}
        </button>
      </div>
    </form>
  );
}
