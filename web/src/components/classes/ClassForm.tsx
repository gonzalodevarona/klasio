"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import {
  ClassLevel,
  ClassScheduleEntry,
  ClassType,
  CreateClassRequest,
  ProgramClassDetail,
  UpdateClassRequest,
} from "@/lib/types/programClass";
import { useAllActiveProfessors } from "@/hooks/useProfessors";

interface FieldErrors {
  name?: string;
  level?: string;
  type?: string;
  professorId?: string;
  maxStudents?: string;
  scheduleEntries?: string;
  [key: string]: string | undefined;
}

interface ScheduleEntryFormData {
  dayOfWeek: string;
  specificDate: string;
  startTime: string;
  endTime: string;
}

interface ClassFormProps {
  programId: string;
  programClass?: ProgramClassDetail;
}

const DAYS_OF_WEEK = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];

const LEVELS: ClassLevel[] = ["BEGINNER", "INTERMEDIATE", "ADVANCED"];

function emptyScheduleEntry(): ScheduleEntryFormData {
  return { dayOfWeek: "", specificDate: "", startTime: "", endTime: "" };
}

export default function ClassForm({ programId, programClass }: ClassFormProps) {
  const router = useRouter();
  const isEdit = !!programClass;
  const { professors, loading: professorsLoading } = useAllActiveProfessors();

  const [name, setName] = useState(programClass?.name ?? "");
  const [level, setLevel] = useState<string>(programClass?.level ?? "");
  const [classType, setClassType] = useState<string>(
    programClass?.type ?? "RECURRING"
  );
  const [maxStudents, setMaxStudents] = useState<string>(
    programClass?.maxStudents?.toString() ?? ""
  );
  const [professorId, setProfessorId] = useState(
    programClass?.professorId ?? ""
  );
  const [scheduleEntries, setScheduleEntries] = useState<
    ScheduleEntryFormData[]
  >(
    programClass?.scheduleEntries?.map((e) => ({
      dayOfWeek: e.dayOfWeek ?? "",
      specificDate: e.specificDate ?? "",
      startTime: e.startTime ?? "",
      endTime: e.endTime ?? "",
    })) ?? [emptyScheduleEntry()]
  );

  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [apiError, setApiError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function validate(): FieldErrors {
    const errors: FieldErrors = {};

    if (!name.trim()) {
      errors.name = "Name is required.";
    } else if (name.trim().length > 100) {
      errors.name = "Name must be at most 100 characters.";
    }

    if (!level) {
      errors.level = "Level is required.";
    }

    if (!classType) {
      errors.type = "Type is required.";
    }

    if (!professorId.trim()) {
      errors.professorId = "Professor is required.";
    }

    const maxStudentsNum = parseInt(maxStudents, 10);
    if (!maxStudents.trim()) {
      errors.maxStudents = "Max students is required.";
    } else if (isNaN(maxStudentsNum) || maxStudentsNum < 1) {
      errors.maxStudents = "Max students must be at least 1.";
    }

    if (scheduleEntries.length === 0) {
      errors.scheduleEntries = "At least one schedule entry is required.";
    } else {
      for (const entry of scheduleEntries) {
        if (!entry.startTime || !entry.endTime) {
          errors.scheduleEntries = "All schedule entries must have start and end times.";
          break;
        }
        if (classType === "RECURRING" && !entry.dayOfWeek) {
          errors.scheduleEntries = "Recurring classes require a day of the week for each entry.";
          break;
        }
        if (classType === "ONE_TIME" && !entry.specificDate) {
          errors.scheduleEntries = "One-time classes require a specific date.";
          break;
        }
      }
    }

    if (classType === "ONE_TIME" && scheduleEntries.length > 1) {
      errors.scheduleEntries = "One-time classes can only have one schedule entry.";
    }

    return errors;
  }

  function addScheduleEntry() {
    setScheduleEntries([...scheduleEntries, emptyScheduleEntry()]);
  }

  function removeScheduleEntry(index: number) {
    setScheduleEntries(scheduleEntries.filter((_, i) => i !== index));
  }

  function updateScheduleEntry(
    index: number,
    field: keyof ScheduleEntryFormData,
    value: string
  ) {
    const updated = [...scheduleEntries];
    updated[index] = { ...updated[index], [field]: value };
    setScheduleEntries(updated);
  }

  function buildScheduleEntries(): ClassScheduleEntry[] {
    return scheduleEntries.map((e) => ({
      dayOfWeek: classType === "RECURRING" ? e.dayOfWeek || undefined : undefined,
      specificDate: classType === "ONE_TIME" ? e.specificDate || undefined : undefined,
      startTime: e.startTime,
      endTime: e.endTime,
    }));
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
        const body: UpdateClassRequest = {
          name: name.trim(),
          level: level as ClassLevel,
          scheduleEntries: buildScheduleEntries(),
          maxStudents: parseInt(maxStudents, 10),
        };
        await api.put<ProgramClassDetail>(
          `/programs/${programId}/classes/${programClass.id}`,
          body
        );

        // Handle professor assignment changes separately
        const previousProfessorId = programClass.professorId ?? "";
        const newProfessorId = professorId.trim();
        if (newProfessorId !== previousProfessorId) {
          if (newProfessorId) {
            await api.put(`/programs/${programId}/classes/${programClass.id}/professor`, {
              professorId: newProfessorId,
            });
          } else if (previousProfessorId) {
            await api.delete(`/programs/${programId}/classes/${programClass.id}/professor`);
          }
        }

        router.push(`/programs/${programId}/classes/${programClass.id}`);
      } else {
        const body: CreateClassRequest = {
          name: name.trim(),
          level: level as ClassLevel,
          type: classType as ClassType,
          scheduleEntries: buildScheduleEntries(),
          professorId: professorId.trim(),
          maxStudents: parseInt(maxStudents, 10),
        };
        const created = await api.post<ProgramClassDetail>(
          `/programs/${programId}/classes`,
          body
        );
        router.push(`/programs/${programId}/classes/${created.id}`);
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

      {/* Name */}
      <div>
        <label
          htmlFor="name"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Class Name <span className="text-red-500">*</span>
        </label>
        <input
          id="name"
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className={`block w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
            fieldErrors.name ? "border-red-500" : "border-gray-300"
          }`}
          placeholder="e.g. Kids Beginner Monday"
        />
        {fieldErrors.name && (
          <p className="mt-1 text-sm text-red-600">{fieldErrors.name}</p>
        )}
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
          className={`block w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
            fieldErrors.level ? "border-red-500" : "border-gray-300"
          }`}
        >
          <option value="">Select a level</option>
          {LEVELS.map((l) => (
            <option key={l} value={l}>
              {l.charAt(0) + l.slice(1).toLowerCase()}
            </option>
          ))}
        </select>
        {fieldErrors.level && (
          <p className="mt-1 text-sm text-red-600">{fieldErrors.level}</p>
        )}
      </div>

      {/* Type */}
      {!isEdit && (
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Class Type <span className="text-red-500">*</span>
          </label>
          <div className="flex gap-4">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="radio"
                name="classType"
                value="RECURRING"
                checked={classType === "RECURRING"}
                onChange={(e) => {
                  setClassType(e.target.value);
                  setScheduleEntries([emptyScheduleEntry()]);
                }}
                className="text-blue-600 focus:ring-blue-500"
              />
              <span className="text-sm text-gray-700">Recurring</span>
            </label>
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="radio"
                name="classType"
                value="ONE_TIME"
                checked={classType === "ONE_TIME"}
                onChange={(e) => {
                  setClassType(e.target.value);
                  setScheduleEntries([emptyScheduleEntry()]);
                }}
                className="text-blue-600 focus:ring-blue-500"
              />
              <span className="text-sm text-gray-700">One-Time</span>
            </label>
          </div>
          {fieldErrors.type && (
            <p className="mt-1 text-sm text-red-600">{fieldErrors.type}</p>
          )}
        </div>
      )}

      {/* Max Students */}
      <div>
        <label
          htmlFor="maxStudents"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Max Students <span className="text-red-500">*</span>
        </label>
        <input
          id="maxStudents"
          type="number"
          min="1"
          value={maxStudents}
          onChange={(e) => setMaxStudents(e.target.value)}
          className={`block w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
            fieldErrors.maxStudents ? "border-red-500" : "border-gray-300"
          }`}
          placeholder="e.g. 20"
        />
        {fieldErrors.maxStudents && (
          <p className="mt-1 text-sm text-red-600">{fieldErrors.maxStudents}</p>
        )}
      </div>

      {/* Professor (required) */}
      <div>
        <label
          htmlFor="professorId"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Professor <span className="text-red-500">*</span>
        </label>
        <select
          id="professorId"
          value={professorId}
          onChange={(e) => setProfessorId(e.target.value)}
          disabled={professorsLoading}
          className={`block w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 ${
            fieldErrors.professorId ? "border-red-500" : "border-gray-300"
          }`}
        >
          <option value="">
            {professorsLoading ? "Loading professors..." : "Select a professor"}
          </option>
          {professors.map((p) => (
            <option key={p.id} value={p.id}>
              {p.firstName} {p.lastName}
            </option>
          ))}
        </select>
        {fieldErrors.professorId && (
          <p className="mt-1 text-sm text-red-600">{fieldErrors.professorId}</p>
        )}
      </div>

      {/* Schedule Entries */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Schedule <span className="text-red-500">*</span>
        </label>
        {fieldErrors.scheduleEntries && (
          <p className="mb-2 text-sm text-red-600">
            {fieldErrors.scheduleEntries}
          </p>
        )}
        <div className="space-y-3">
          {scheduleEntries.map((entry, index) => (
            <div
              key={index}
              className="flex flex-wrap items-end gap-3 p-3 border border-gray-200 rounded-md bg-gray-50"
            >
              {classType === "RECURRING" ? (
                <div className="flex-1 min-w-[140px]">
                  <label className="block text-xs text-gray-500 mb-1">
                    Day of Week
                  </label>
                  <select
                    value={entry.dayOfWeek}
                    onChange={(e) =>
                      updateScheduleEntry(index, "dayOfWeek", e.target.value)
                    }
                    className="block w-full rounded-md border border-gray-300 px-2 py-1.5 text-sm"
                  >
                    <option value="">Select day</option>
                    {DAYS_OF_WEEK.map((day) => (
                      <option key={day} value={day}>
                        {day.charAt(0) + day.slice(1).toLowerCase()}
                      </option>
                    ))}
                  </select>
                </div>
              ) : (
                <div className="flex-1 min-w-[140px]">
                  <label className="block text-xs text-gray-500 mb-1">
                    Date
                  </label>
                  <input
                    type="date"
                    value={entry.specificDate}
                    onChange={(e) =>
                      updateScheduleEntry(index, "specificDate", e.target.value)
                    }
                    className="block w-full rounded-md border border-gray-300 px-2 py-1.5 text-sm"
                  />
                </div>
              )}

              <div className="min-w-[100px]">
                <label className="block text-xs text-gray-500 mb-1">
                  Start Time
                </label>
                <input
                  type="time"
                  value={entry.startTime}
                  onChange={(e) =>
                    updateScheduleEntry(index, "startTime", e.target.value)
                  }
                  className="block w-full rounded-md border border-gray-300 px-2 py-1.5 text-sm"
                />
              </div>

              <div className="min-w-[100px]">
                <label className="block text-xs text-gray-500 mb-1">
                  End Time
                </label>
                <input
                  type="time"
                  value={entry.endTime}
                  onChange={(e) =>
                    updateScheduleEntry(index, "endTime", e.target.value)
                  }
                  className="block w-full rounded-md border border-gray-300 px-2 py-1.5 text-sm"
                />
              </div>

              {scheduleEntries.length > 1 && (
                <button
                  type="button"
                  onClick={() => removeScheduleEntry(index)}
                  className="text-red-500 hover:text-red-700 text-sm pb-1"
                >
                  Remove
                </button>
              )}
            </div>
          ))}
        </div>

        {classType === "RECURRING" && (
          <button
            type="button"
            onClick={addScheduleEntry}
            className="mt-2 text-sm text-blue-600 hover:text-blue-800"
          >
            + Add Schedule Entry
          </button>
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
              : "Create Class"}
        </button>
      </div>
    </form>
  );
}
