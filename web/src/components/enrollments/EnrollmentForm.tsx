"use client";

import { FormEvent, useState } from "react";
import { api, ApiError } from "@/lib/api";
import { usePrograms } from "@/hooks/usePrograms";
import { Level } from "@/lib/types/enrollment";

interface EnrollmentFormProps {
  studentId: string;
  onSuccess: () => void;
}

const LEVELS: Level[] = ["BEGINNER", "INTERMEDIATE", "ADVANCED"];

export default function EnrollmentForm({ studentId, onSuccess }: EnrollmentFormProps) {
  const { programs, loading: programsLoading } = usePrograms(0, 200, "ACTIVE");

  const [programId, setProgramId] = useState("");
  const [level, setLevel] = useState("");
  const [apiError, setApiError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function validate(): string | null {
    if (!programId) return "Please select a program.";
    if (!level) return "Please select a level.";
    return null;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();

    const validationError = validate();
    setApiError(null);

    if (validationError) {
      setApiError(validationError);
      return;
    }

    setSubmitting(true);

    try {
      await api.post(`/programs/${programId}/enrollments`, {
        studentId,
        level,
      });
      onSuccess();
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.code === "ENROLLMENT_ALREADY_EXISTS") {
          setApiError("This student is already enrolled in this program at the selected level.");
        } else {
          setApiError(err.message);
        }
      } else {
        setApiError("An unexpected error occurred. Please try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4" noValidate>
      {apiError && (
        <div
          className="rounded-md bg-red-50 p-4 text-sm text-red-700 border border-red-200"
          role="alert"
        >
          {apiError}
        </div>
      )}

      {/* Program */}
      <div>
        <label
          htmlFor="programId"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Program <span className="text-red-500">*</span>
        </label>
        <select
          id="programId"
          value={programId}
          onChange={(e) => setProgramId(e.target.value)}
          disabled={programsLoading}
          className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">
            {programsLoading ? "Loading programs..." : "Select a program"}
          </option>
          {programs.map((program) => (
            <option key={program.id} value={program.id}>
              {program.name}
            </option>
          ))}
        </select>
      </div>

      {/* Level */}
      <div>
        <label
          htmlFor="level"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Level <span className="text-red-500">*</span>
        </label>
        <select
          id="level"
          value={level}
          onChange={(e) => setLevel(e.target.value)}
          className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">Select a level</option>
          {LEVELS.map((l) => (
            <option key={l} value={l}>
              {l.charAt(0) + l.slice(1).toLowerCase()}
            </option>
          ))}
        </select>
      </div>

      {/* Submit */}
      <div className="pt-2">
        <button
          type="submit"
          disabled={submitting || programsLoading}
          className="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {submitting ? "Enrolling..." : "Enroll Student"}
        </button>
      </div>
    </form>
  );
}
