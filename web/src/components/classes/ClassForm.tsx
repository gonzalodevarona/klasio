"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
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
import { Input, Select, Button } from "@/components/ui";

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
  const t = useTranslations("classes");
  const tCommon = useTranslations("common");
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
      errors.name = t("formNameRequired");
    } else if (name.trim().length > 100) {
      errors.name = t("formNameMaxLength");
    }

    if (!level) {
      errors.level = t("formLevelRequired");
    }

    if (!classType) {
      errors.type = t("formTypeRequired");
    }

    if (!professorId.trim()) {
      errors.professorId = t("formProfessorRequired");
    }

    const maxStudentsNum = parseInt(maxStudents, 10);
    if (!maxStudents.trim()) {
      errors.maxStudents = t("formMaxStudentsRequired");
    } else if (isNaN(maxStudentsNum) || maxStudentsNum < 1) {
      errors.maxStudents = t("formMaxStudentsMin");
    }

    if (scheduleEntries.length === 0) {
      errors.scheduleEntries = t("formScheduleRequired");
    } else {
      for (const entry of scheduleEntries) {
        if (!entry.startTime || !entry.endTime) {
          errors.scheduleEntries = t("formScheduleTimesRequired");
          break;
        }
        if (classType === "RECURRING" && !entry.dayOfWeek) {
          errors.scheduleEntries = t("formScheduleDayRequired");
          break;
        }
        if (classType === "ONE_TIME" && !entry.specificDate) {
          errors.scheduleEntries = t("formScheduleDateRequired");
          break;
        }
      }
    }

    if (classType === "ONE_TIME" && scheduleEntries.length > 1) {
      errors.scheduleEntries = t("formScheduleOneTimeMax");
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

      {/* Name */}
      <Input
        label={t("formClassNameLabel")}
        type="text"
        value={name}
        onChange={(e) => setName(e.target.value)}
        placeholder={t("formClassNamePlaceholder")}
        error={fieldErrors.name}
      />

      {/* Level */}
      <Select
        label={t("formLevelLabel")}
        value={level}
        onChange={(e) => setLevel(e.target.value)}
        required
        error={fieldErrors.level}
      >
        <option value="">{t("formLevelPlaceholder")}</option>
        {LEVELS.map((l) => (
          <option key={l} value={l}>
            {l.charAt(0) + l.slice(1).toLowerCase()}
          </option>
        ))}
      </Select>

      {/* Type */}
      {!isEdit && (
        <div>
          <label className="block text-sm font-medium text-k-subtle mb-1">
            {t("formTypeLabel")} <span className="text-red-500">*</span>
          </label>
          <div className="flex gap-4">
            <label className="flex items-center gap-2 cursor-pointer">
              {/* TODO: no primitive for type="radio" */}
              <input
                type="radio"
                name="classType"
                value="RECURRING"
                checked={classType === "RECURRING"}
                onChange={(e) => {
                  setClassType(e.target.value);
                  setScheduleEntries([emptyScheduleEntry()]);
                }}
                className="accent-k-volt focus:ring-k-volt"
              />
              <span className="text-sm text-k-subtle">{t("formTypeRecurring")}</span>
            </label>
            <label className="flex items-center gap-2 cursor-pointer">
              {/* TODO: no primitive for type="radio" */}
              <input
                type="radio"
                name="classType"
                value="ONE_TIME"
                checked={classType === "ONE_TIME"}
                onChange={(e) => {
                  setClassType(e.target.value);
                  setScheduleEntries([emptyScheduleEntry()]);
                }}
                className="accent-k-volt focus:ring-k-volt"
              />
              <span className="text-sm text-k-subtle">{t("formTypeOneTime")}</span>
            </label>
          </div>
          {fieldErrors.type && (
            <p className="mt-1 text-sm text-red-600">{fieldErrors.type}</p>
          )}
        </div>
      )}

      {/* Max Students */}
      <Input
        label={t("formMaxStudentsLabel")}
        type="number"
        min="1"
        value={maxStudents}
        onChange={(e) => setMaxStudents(e.target.value)}
        placeholder={t("formMaxStudentsPlaceholder")}
        error={fieldErrors.maxStudents}
      />

      {/* Professor (required) */}
      <Select
        label={t("formProfessorLabel")}
        value={professorId}
        onChange={(e) => setProfessorId(e.target.value)}
        required
        disabled={professorsLoading}
        error={fieldErrors.professorId}
      >
        <option value="">
          {professorsLoading ? t("formProfessorLoading") : t("formProfessorPlaceholder")}
        </option>
        {professors.map((p) => (
          <option key={p.id} value={p.id}>
            {p.firstName} {p.lastName}
          </option>
        ))}
      </Select>

      {/* Schedule Entries */}
      <div>
        <label className="block text-sm font-medium text-k-subtle mb-2">
          {t("formScheduleLabel")} <span className="text-red-500">*</span>
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
              className="flex flex-wrap items-end gap-3 p-3 border border-k-border rounded-md bg-k-surface"
            >
              {classType === "RECURRING" ? (
                <div className="flex-1 min-w-[140px]">
                  <Select
                    label={t("formScheduleDayLabel")}
                    value={entry.dayOfWeek}
                    onChange={(e) =>
                      updateScheduleEntry(index, "dayOfWeek", e.target.value)
                    }
                  >
                    <option value="">{t("formScheduleDayPlaceholder")}</option>
                    {DAYS_OF_WEEK.map((day) => (
                      <option key={day} value={day}>
                        {day.charAt(0) + day.slice(1).toLowerCase()}
                      </option>
                    ))}
                  </Select>
                </div>
              ) : (
                <div className="flex-1 min-w-[140px]">
                  <label className="block text-xs text-k-muted mb-1">
                    {t("formScheduleDateLabel")}
                  </label>
                  {/* TODO: no primitive for type="date" */}
                  <input
                    type="date"
                    value={entry.specificDate}
                    onChange={(e) =>
                      updateScheduleEntry(index, "specificDate", e.target.value)
                    }
                    className="bg-k-surface border border-k-border rounded-k-sm px-3 py-2 text-sm focus:border-k-volt focus:outline-none block w-full"
                  />
                </div>
              )}

              <div className="min-w-[100px]">
                <label className="block text-xs text-k-muted mb-1">
                  {t("formScheduleStartLabel")}
                </label>
                {/* TODO: no primitive for type="time" */}
                <input
                  type="time"
                  value={entry.startTime}
                  onChange={(e) =>
                    updateScheduleEntry(index, "startTime", e.target.value)
                  }
                  className="bg-k-surface border border-k-border rounded-k-sm px-3 py-2 text-sm focus:border-k-volt focus:outline-none block w-full"
                />
              </div>

              <div className="min-w-[100px]">
                <label className="block text-xs text-k-muted mb-1">
                  {t("formScheduleEndLabel")}
                </label>
                {/* TODO: no primitive for type="time" */}
                <input
                  type="time"
                  value={entry.endTime}
                  onChange={(e) =>
                    updateScheduleEntry(index, "endTime", e.target.value)
                  }
                  className="bg-k-surface border border-k-border rounded-k-sm px-3 py-2 text-sm focus:border-k-volt focus:outline-none block w-full"
                />
              </div>

              {scheduleEntries.length > 1 && (
                <Button
                  variant="danger"
                  size="sm"
                  type="button"
                  onClick={() => removeScheduleEntry(index)}
                >
                  {t("formScheduleRemoveButton")}
                </Button>
              )}
            </div>
          ))}
        </div>

        {classType === "RECURRING" && (
          <Button
            variant="outline"
            size="sm"
            type="button"
            onClick={addScheduleEntry}
            className="mt-2"
          >
            {t("formScheduleAddEntry")}
          </Button>
        )}
      </div>

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
