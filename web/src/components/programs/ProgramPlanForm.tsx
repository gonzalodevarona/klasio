"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import { ProgramModality } from "@/lib/types/programPlan";
import {
  CreateProgramPlanRequest,
  UpdateProgramPlanRequest,
  ProgramPlanDetail,
  ScheduleEntry,
} from "@/lib/types/programPlan";

const DAYS_OF_WEEK = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];

const DAY_LABELS: Record<string, string> = {
  MONDAY: "Mon",
  TUESDAY: "Tue",
  WEDNESDAY: "Wed",
  THURSDAY: "Thu",
  FRIDAY: "Fri",
  SATURDAY: "Sat",
  SUNDAY: "Sun",
};

interface ProgramPlanFormProps {
  programId: string;
  plan?: ProgramPlanDetail;
}

export default function ProgramPlanForm({
  programId,
  plan,
}: ProgramPlanFormProps) {
  const router = useRouter();
  const isEdit = !!plan;

  const [name, setName] = useState(plan?.name ?? "");
  const [modality, setModality] = useState<ProgramModality>(
    plan?.modality ?? "HOURS_BASED"
  );
  const [cost, setCost] = useState(plan?.cost?.toString() ?? "");
  const [hours, setHours] = useState(plan?.hours?.toString() ?? "");
  const [managerId, setManagerId] = useState(plan?.managerId ?? "");
  const [scheduleEntries, setScheduleEntries] = useState<ScheduleEntry[]>(
    plan?.scheduleEntries ?? []
  );
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function addScheduleEntry() {
    setScheduleEntries((prev) => [
      ...prev,
      { dayOfWeek: "MONDAY", startTime: "18:00", endTime: "20:00" },
    ]);
  }

  function removeScheduleEntry(index: number) {
    setScheduleEntries((prev) => prev.filter((_, i) => i !== index));
  }

  function updateScheduleEntry(
    index: number,
    field: keyof ScheduleEntry,
    value: string
  ) {
    setScheduleEntries((prev) =>
      prev.map((entry, i) =>
        i === index ? { ...entry, [field]: value } : entry
      )
    );
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);

    const costNum = parseFloat(cost);
    if (isNaN(costNum) || costNum <= 0) {
      setError("Cost must be a positive number.");
      setSubmitting(false);
      return;
    }

    if (modality === "HOURS_BASED") {
      const hoursNum = parseInt(hours, 10);
      if (isNaN(hoursNum) || hoursNum <= 0) {
        setError("Hours must be a positive integer.");
        setSubmitting(false);
        return;
      }
    }

    if (modality === "CLASSES_PER_WEEK" && scheduleEntries.length === 0) {
      setError("At least one schedule entry is required.");
      setSubmitting(false);
      return;
    }

    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
    if (!managerId.trim()) {
      setError("Manager ID is required.");
      setSubmitting(false);
      return;
    }
    if (!uuidRegex.test(managerId.trim())) {
      setError("Manager ID must be a valid UUID.");
      setSubmitting(false);
      return;
    }

    try {
      if (isEdit) {
        const body: UpdateProgramPlanRequest = {
          name: name.trim(),
          cost: costNum,
          hours: modality === "HOURS_BASED" ? parseInt(hours, 10) : null,
          managerId: managerId.trim(),
          scheduleEntries:
            modality === "CLASSES_PER_WEEK" ? scheduleEntries : undefined,
        };
        await api.put(`/programs/${programId}/plans/${plan.id}`, body);
      } else {
        const body: CreateProgramPlanRequest = {
          name: name.trim(),
          modality,
          cost: costNum,
          hours: modality === "HOURS_BASED" ? parseInt(hours, 10) : null,
          managerId: managerId.trim(),
          scheduleEntries:
            modality === "CLASSES_PER_WEEK" ? scheduleEntries : undefined,
        };
        await api.post(`/programs/${programId}/plans`, body);
      }
      router.push(`/programs/${programId}`);
    } catch (err) {
      setError(
        err instanceof ApiError
          ? err.message
          : `Failed to ${isEdit ? "update" : "create"} plan.`
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {error && (
        <div
          className="rounded-md bg-red-50 p-4 text-sm text-red-700 border border-red-200"
          role="alert"
        >
          {error}
        </div>
      )}

      <div>
        <label
          htmlFor="name"
          className="block text-sm font-medium text-gray-700"
        >
          Plan Name
        </label>
        <input
          type="text"
          id="name"
          name="name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
          maxLength={100}
          className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
          placeholder={
            modality === "HOURS_BASED"
              ? "e.g., 4 Hours"
              : "e.g., Mon/Wed/Thu Evening"
          }
        />
      </div>

      {/* Modality */}
      <div>
        <label
          htmlFor="modality"
          className="block text-sm font-medium text-gray-700"
        >
          Modality
        </label>
        {isEdit ? (
          <>
            <input
              type="text"
              id="modality"
              value={modality === "HOURS_BASED" ? "Hours Based" : "Classes per Week"}
              disabled
              className="mt-1 block w-full rounded-md border-gray-300 bg-gray-100 shadow-sm sm:text-sm cursor-not-allowed"
            />
            <p className="mt-1 text-xs text-gray-500">
              Modality cannot be changed after creation.
            </p>
          </>
        ) : (
          <select
            id="modality"
            value={modality}
            onChange={(e) => setModality(e.target.value as ProgramModality)}
            className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
          >
            <option value="HOURS_BASED">Hours Based</option>
            <option value="CLASSES_PER_WEEK">Classes per Week</option>
          </select>
        )}
      </div>

      <div>
        <label
          htmlFor="cost"
          className="block text-sm font-medium text-gray-700"
        >
          Cost (COP)
        </label>
        <input
          type="number"
          id="cost"
          name="cost"
          value={cost}
          onChange={(e) => setCost(e.target.value)}
          required
          min="1"
          step="1"
          className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
        />
      </div>

      {/* Manager ID */}
      <div>
        <label
          htmlFor="managerId"
          className="block text-sm font-medium text-gray-700"
        >
          Manager ID
        </label>
        <input
          type="text"
          id="managerId"
          name="managerId"
          value={managerId}
          onChange={(e) => setManagerId(e.target.value)}
          required
          className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
          placeholder="e.g. 550e8400-e29b-41d4-a716-446655440000"
        />
        <p className="mt-1 text-xs text-gray-500">
          UUID of the manager assigned to this plan.
        </p>
      </div>

      {modality === "HOURS_BASED" && (
        <div>
          <label
            htmlFor="hours"
            className="block text-sm font-medium text-gray-700"
          >
            Hours
          </label>
          <input
            type="number"
            id="hours"
            name="hours"
            value={hours}
            onChange={(e) => setHours(e.target.value)}
            required
            min="1"
            step="1"
            className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
          />
          <p className="mt-1 text-xs text-gray-500">
            Number of hours included in this plan.
          </p>
        </div>
      )}

      {modality === "CLASSES_PER_WEEK" && (
        <div>
          <div className="flex items-center justify-between mb-3">
            <label className="block text-sm font-medium text-gray-700">
              Schedule Entries
            </label>
            <button
              type="button"
              onClick={addScheduleEntry}
              className="inline-flex items-center rounded-md bg-blue-50 px-3 py-1.5 text-xs font-medium text-blue-700 hover:bg-blue-100"
            >
              + Add Entry
            </button>
          </div>

          {scheduleEntries.length === 0 && (
            <p className="text-sm text-gray-500 italic">
              No schedule entries. Click &quot;Add Entry&quot; to define class
              times.
            </p>
          )}

          <div className="space-y-3">
            {scheduleEntries.map((entry, index) => (
              <div
                key={index}
                className="flex items-center gap-3 p-3 bg-gray-50 rounded-md"
              >
                <select
                  value={entry.dayOfWeek}
                  onChange={(e) =>
                    updateScheduleEntry(index, "dayOfWeek", e.target.value)
                  }
                  className="rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                  aria-label="Day of week"
                >
                  {DAYS_OF_WEEK.map((day) => (
                    <option key={day} value={day}>
                      {DAY_LABELS[day]}
                    </option>
                  ))}
                </select>

                <input
                  type="time"
                  value={entry.startTime}
                  onChange={(e) =>
                    updateScheduleEntry(index, "startTime", e.target.value)
                  }
                  className="rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                  aria-label="Start time"
                />

                <span className="text-gray-400 text-sm">to</span>

                <input
                  type="time"
                  value={entry.endTime}
                  onChange={(e) =>
                    updateScheduleEntry(index, "endTime", e.target.value)
                  }
                  className="rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                  aria-label="End time"
                />

                <button
                  type="button"
                  onClick={() => removeScheduleEntry(index)}
                  className="text-red-500 hover:text-red-700 text-sm font-medium"
                  aria-label="Remove entry"
                >
                  Remove
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="flex justify-end gap-3 pt-4 border-t">
        <button
          type="button"
          onClick={() => router.push(`/programs/${programId}`)}
          className="inline-flex items-center rounded-md bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={submitting}
          className="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {submitting
            ? isEdit
              ? "Updating..."
              : "Creating..."
            : isEdit
              ? "Update Plan"
              : "Create Plan"}
        </button>
      </div>
    </form>
  );
}
