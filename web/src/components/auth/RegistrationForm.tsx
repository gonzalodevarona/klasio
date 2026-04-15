"use client";

import { useState, useMemo } from "react";
import PasswordPolicyChecker from "./PasswordPolicyChecker";
import DocumentFields from "@/components/common/DocumentFields";
import type { IdentityDocumentType } from "@/lib/types/identity";
import type { AuthError } from "@/lib/types/auth";

interface RegistrationFormProps {
  tenantSlug: string;
}

export default function RegistrationForm({ tenantSlug }: RegistrationFormProps) {
  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    dateOfBirth: "",
    identityDocumentType: "CC" as IdentityDocumentType,
    identityNumber: "",
    eps: "",
    email: "",
    password: "",
    tutorFullName: "",
    tutorRelationship: "",
    tutorContact: "",
  });

  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<{
    code: string;
    message: string;
    violations?: string[];
  } | null>(null);

  const isMinor = useMemo(() => {
    if (!formData.dateOfBirth) return false;
    const dob = new Date(formData.dateOfBirth);
    const today = new Date();
    let age = today.getFullYear() - dob.getFullYear();
    const monthDiff = today.getMonth() - dob.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < dob.getDate())) {
      age--;
    }
    return age < 18;
  }, [formData.dateOfBirth]);

  function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const body = {
        firstName: formData.firstName,
        lastName: formData.lastName,
        dateOfBirth: formData.dateOfBirth,
        identityDocumentType: formData.identityDocumentType,
        identityNumber: formData.identityNumber,
        eps: formData.eps,
        email: formData.email,
        password: formData.password,
        ...(formData.tutorFullName && { tutorFullName: formData.tutorFullName }),
        ...(formData.tutorRelationship && { tutorRelationship: formData.tutorRelationship }),
        ...(formData.tutorContact && { tutorContact: formData.tutorContact }),
      };

      const response = await fetch(`/api/auth/register/${tenantSlug}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });

      if (!response.ok) {
        const data = (await response.json()) as AuthError;
        setError({
          code: data.error.code,
          message: data.error.message,
          violations: data.error.violations,
        });
        return;
      }

      setSuccess(true);
    } catch {
      setError({ code: "NETWORK_ERROR", message: "Unable to connect to server" });
    } finally {
      setLoading(false);
    }
  }

  if (success) {
    return (
      <div className="bg-green-50 border border-green-200 rounded-md p-6 text-center">
        <h2 className="text-lg font-semibold text-green-800 mb-2">Registration Successful!</h2>
        <p className="text-sm text-green-700">
          We&apos;ve sent a verification email to <strong>{formData.email}</strong>.
          Please check your inbox and click the verification link to activate your account.
        </p>
        <a
          href="/login"
          className="mt-4 inline-block text-sm text-indigo-600 hover:text-indigo-500"
        >
          Go to Login
        </a>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-md p-4">
          <p className="text-sm text-red-800">{error.message}</p>
          {error.violations && (
            <ul className="mt-2 list-disc list-inside text-xs text-red-700">
              {error.violations.map((v) => (
                <li key={v}>{v}</li>
              ))}
            </ul>
          )}
        </div>
      )}

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label htmlFor="firstName" className="block text-sm font-medium text-gray-700">
            First Name *
          </label>
          <input
            id="firstName"
            name="firstName"
            type="text"
            value={formData.firstName}
            onChange={handleChange}
            required
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
          />
        </div>
        <div>
          <label htmlFor="lastName" className="block text-sm font-medium text-gray-700">
            Last Name *
          </label>
          <input
            id="lastName"
            name="lastName"
            type="text"
            value={formData.lastName}
            onChange={handleChange}
            required
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
          />
        </div>
      </div>

      <div>
        <label htmlFor="email" className="block text-sm font-medium text-gray-700">
          Email *
        </label>
        <input
          id="email"
          name="email"
          type="email"
          value={formData.email}
          onChange={handleChange}
          required
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
        />
      </div>

      <div>
        <label htmlFor="password" className="block text-sm font-medium text-gray-700">
          Password *
        </label>
        <input
          id="password"
          name="password"
          type="password"
          value={formData.password}
          onChange={handleChange}
          required
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
        />
        <PasswordPolicyChecker password={formData.password} />
      </div>

      <div>
        <label htmlFor="dateOfBirth" className="block text-sm font-medium text-gray-700">
          Date of Birth *
        </label>
        <input
          id="dateOfBirth"
          name="dateOfBirth"
          type="date"
          value={formData.dateOfBirth}
          onChange={handleChange}
          required
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
        />
      </div>

      <DocumentFields
        documentType={formData.identityDocumentType}
        documentNumber={formData.identityNumber}
        onDocumentTypeChange={(val) =>
          setFormData((prev) => ({ ...prev, identityDocumentType: val }))
        }
        onDocumentNumberChange={(val) =>
          setFormData((prev) => ({ ...prev, identityNumber: val }))
        }
        labelClassName="block text-sm font-medium text-gray-700"
        inputClassName="mt-1 block w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
      />

      <div>
        <label htmlFor="eps" className="block text-sm font-medium text-gray-700">
          EPS *
        </label>
        <input
          id="eps"
          name="eps"
          type="text"
          value={formData.eps}
          onChange={handleChange}
          required
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
        />
      </div>

      {isMinor && (
        <fieldset className="border border-amber-200 bg-amber-50 rounded-md p-4 space-y-4">
          <legend className="text-sm font-medium text-amber-800 px-2">
            Tutor Information (required for minors)
          </legend>
          <div>
            <label htmlFor="tutorFullName" className="block text-sm font-medium text-gray-700">
              Tutor Full Name *
            </label>
            <input
              id="tutorFullName"
              name="tutorFullName"
              type="text"
              value={formData.tutorFullName}
              onChange={handleChange}
              required={isMinor}
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
            />
          </div>
          <div>
            <label htmlFor="tutorRelationship" className="block text-sm font-medium text-gray-700">
              Relationship *
            </label>
            <input
              id="tutorRelationship"
              name="tutorRelationship"
              type="text"
              value={formData.tutorRelationship}
              onChange={handleChange}
              required={isMinor}
              placeholder="e.g., Mother, Father, Legal Guardian"
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
            />
          </div>
          <div>
            <label htmlFor="tutorContact" className="block text-sm font-medium text-gray-700">
              Tutor Phone *
            </label>
            <input
              id="tutorContact"
              name="tutorContact"
              type="text"
              value={formData.tutorContact}
              onChange={handleChange}
              required={isMinor}
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
            />
          </div>
        </fieldset>
      )}

      <button
        type="submit"
        disabled={loading}
        className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
      >
        {loading ? "Creating account..." : "Create Account"}
      </button>

      <p className="text-center text-sm text-gray-600">
        Already have an account?{" "}
        <a href="/login" className="text-indigo-600 hover:text-indigo-500">
          Sign in
        </a>
      </p>
    </form>
  );
}
