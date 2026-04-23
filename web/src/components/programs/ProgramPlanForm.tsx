"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { api, ApiError } from "@/lib/api";
import { ProgramModality } from "@/lib/types/programPlan";
import {
  CreateProgramPlanRequest,
  UpdateProgramPlanRequest,
  ProgramPlanDetail,
  ScheduleEntry,
} from "@/lib/types/programPlan";
import { useManagers } from "@/hooks/useManagers";

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
  tenantId?: string;
  plan?: ProgramPlanDetail;
}

export default function ProgramPlanForm({
  programId,
  tenantId,
  plan,
}: ProgramPlanFormProps) {
  const t = useTranslations("programs");
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

  const { managers, loading: loadingManagers } = useManagers(
    tenantId ? { tenantId, status: "ACTIVE", size: 100 } : undefined
  );

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
      setError(t("formPlanCostError"));
      setSubmitting(false);
      return;
    }

    if (modality === "HOURS_BASED") {
      const hoursNum = parseInt(hours, 10);
      if (isNaN(hoursNum) || hoursNum <= 0) {
        setError(t("formPlanHoursError"));
        setSubmitting(false);
        return;
      }
    }

    if (modality === "CLASSES_PER_WEEK" && scheduleEntries.length === 0) {
      setError(t("formPlanScheduleError"));
      setSubmitting(false);
      return;
    }

    if (!managerId.trim()) {
      setError(t("formPlanManagerError"));
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
          {t("formNameLabel")}
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
              ? t("formNamePlaceholderHours")
              : t("formNamePlaceholderSchedule")
          }
        />
      </div>

      {/* Modality */}
      <div>
        <label
          htmlFor="modality"
          className="block text-sm font-medium text-gray-700"
        >
          {t("formModalityLabel")}
        </label>
        {isEdit ? (
          <>
            <input
              type="text"
              id="modality"
              value={modality === "HOURS_BASED" ? t("modalityHoursBased") : t("modalityClassesPerWeek")}
              disabled
              className="mt-1 block w-full rounded-md border-gray-300 bg-gray-100 shadow-sm sm:text-sm cursor-not-allowed"
            />
            <p className="mt-1 text-xs text-gray-500">
              {t("formModalityCannotChange")}
            </p>
          </>
        ) : (
          <select
            id="modality"
            value={modality}
            onChange={(e) => setModality(e.target.value as ProgramModality)}
            className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
          >
            <option value="HOURS_BASED">{t("modalityHoursBased")}</option>
            <option value="CLASSES_PER_WEEK">{t("modalityClassesPerWeek")}</option>
          </select>
        )}
      </div>

      <div>
        <label
          htmlFor="cost"
          className="block text-sm font-medium text-gray-700"
        >
          {t("formCostLabel")}
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

      {/* Manager */}
      <div>
        <label
          htmlFor="managerId"
          className="block text-sm font-medium text-gray-700"
        >
          {t("formManagerLabel")}
        </label>
        <select
          id="managerId"
          value={managerId}
          onChange={(e) => setManagerId(e.target.value)}
          required
          disabled={loadingManagers}
          className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm disabled:bg-gray-100"
        >
          <option value="">
            {loadingManagers ? t("formManagerLoading") : t("formManagerPlaceholder")}
          </option>
          {managers.map((m) => (
            <option key={m.id} value={m.id}>
              {m.firstName} {m.lastName} - {m.identityDocumentType} {m.identityNumber}
            </option>
          ))}
        </select>
        {!loadingManagers && managers.length === 0 && (
          <p className="mt-1 text-xs text-amber-600">
            {t("formManagerNone")}
          </p>
        )}
      </div>

      {modality === "HOURS_BASED" && (
        <div>
          <label
            htmlFor="hours"
            className="block text-sm font-medium text-gray-700"
          >
            {t("formHoursLabel")}
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
            {t("formHoursHint")}
          </p>
        </div>
      )}

      {modality === "CLASSES_PER_WEEK" && (
        <div>
          <div className="flex items-center justify-between mb-3">
            <label className="block text-sm font-medium text-gray-700">
              {t("formScheduleLabel")}
            </label>
            <button
              type="button"
              onClick={addScheduleEntry}
              className="inline-flex items-center rounded-md bg-blue-50 px-3 py-1.5 text-xs font-medium text-blue-700 hover:bg-blue-100"
            >
              {t("formScheduleAddEntry")}
            </button>
          </div>

          {scheduleEntries.length === 0 && (
            <p className="text-sm text-gray-500 italic">
              {t("formScheduleEmpty")}
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

                <span className="text-gray-400 text-sm">{t("formScheduleTo")}</span>

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
                  {t("formScheduleRemove")}
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
          {t("formCancelButton")}
        </button>
        <button
          type="submit"
          disabled={submitting}
          className="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {submitting
            ? isEdit
              ? t("formUpdatingButton")
              : t("formCreatingButton")
            : isEdit
              ? t("formUpdateButton")
              : t("formCreatePlanButton")}
        </button>
      </div>
    </form>
  );
}
