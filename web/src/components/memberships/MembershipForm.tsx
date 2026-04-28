"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Select, Button } from "@/components/ui";
import { CreateMembershipRequest } from "@/lib/types/membership";
import { ProgramPlanSummary } from "@/lib/types/program";

interface MembershipFormProps {
  studentId: string;
  plans: ProgramPlanSummary[];
  onSubmit: (data: CreateMembershipRequest) => Promise<void>;
  onCancel: () => void;
}

export default function MembershipForm({
  studentId,
  plans,
  onSubmit,
  onCancel,
}: MembershipFormProps) {
  const t = useTranslations("memberships");
  const tMembership = useTranslations("membership");
  const tCommon = useTranslations("common");

  const today = new Date();
  const todayIso = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;

  const [planId, setPlanId] = useState("");
  const [startDate, setStartDate] = useState(todayIso);
  const [paymentValidated, setPaymentValidated] = useState(false);
  const [activateDirectly, setActivateDirectly] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const selectedPlan = plans.find((p) => p.id === planId);
  const isValid = planId !== "" && startDate !== "";

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!isValid) return;
    setSubmitting(true);
    setError(null);
    try {
      await onSubmit({
        studentId,
        planId,
        startDate,
        paymentValidated,
        activateDirectly: paymentValidated ? activateDirectly : false,
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : tCommon("unexpectedError"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <div>
        <Select
          label={t("formPlanLabel")}
          value={planId}
          onChange={(e) => setPlanId(e.target.value)}
          required
        >
          <option value="">{t("formPlanSelectPlaceholder")}</option>
          {plans.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name} — {p.hours} hours
            </option>
          ))}
        </Select>
        {selectedPlan && (
          <div className="mt-2 rounded-lg border border-k-border bg-k-surface px-4 py-3 space-y-1.5">
            <p className="text-sm font-semibold text-k-ink">{selectedPlan.name}</p>
            <div className="grid grid-cols-2 gap-x-6 gap-y-1 text-sm">
              <div>
                <span className="text-k-volt font-medium">{t("formModalityLabel")}</span>
                <span className="text-k-subtle">
                  {selectedPlan.modality === "HOURS_BASED"
                    ? t("formModalityHoursBased")
                    : selectedPlan.modality === "UNLIMITED"
                    ? tMembership("modality.unlimited")
                    : t("formModalityClassesPerWeek")}
                </span>
              </div>
              {selectedPlan.modality === "UNLIMITED" ? (
                <div>
                  <span className="text-k-subtle">{tMembership("unlimited.label")}</span>
                </div>
              ) : selectedPlan.modality === "HOURS_BASED" && selectedPlan.hours != null ? (
                <div>
                  <span className="text-k-volt font-medium">{t("formHoursLabel")}</span>
                  <span className="text-k-subtle">{selectedPlan.hours}h / month</span>
                </div>
              ) : null}
              <div>
                <span className="text-k-volt font-medium">{t("formCostLabel")}</span>
                <span className="text-k-subtle">${Number(selectedPlan.cost).toLocaleString()}</span>
              </div>
            </div>
          </div>
        )}
      </div>

      <div>
        <label className="block text-sm font-medium text-k-subtle mb-1">
          {t("formStartDateLabel")}
        </label>
        {/* TODO: no primitive for type="date" */}
        <input
          type="date"
          value={startDate}
          onChange={(e) => setStartDate(e.target.value)}
          required
          className="w-full bg-k-surface border border-k-border rounded-k-sm px-3 py-2 text-sm focus:border-k-volt focus:outline-none"
        />
        <p className="mt-1 text-xs text-k-muted">{t("formExpiresHint")}</p>
      </div>

      <div className="space-y-2">
        <label className="flex items-center gap-3 text-sm text-k-subtle cursor-pointer">
          {/* TODO: no primitive for type="checkbox" */}
          <input
            type="checkbox"
            checked={paymentValidated}
            onChange={(e) => {
              setPaymentValidated(e.target.checked);
              if (!e.target.checked) setActivateDirectly(false);
            }}
            className="h-4 w-4 rounded border-k-border accent-k-volt focus:ring-k-volt"
          />
          {t("formPaymentValidated")}
        </label>

        {paymentValidated && (
          <label className="flex items-center gap-3 text-sm text-k-subtle cursor-pointer ml-7">
            {/* TODO: no primitive for type="checkbox" */}
            <input
              type="checkbox"
              checked={activateDirectly}
              onChange={(e) => setActivateDirectly(e.target.checked)}
              className="h-4 w-4 rounded border-k-border accent-k-volt focus:ring-k-volt"
            />
            {t("formActivateDirectly")}
          </label>
        )}
      </div>

      {error && (
        <div className="rounded-md bg-red-50 p-3 text-sm text-red-700 border border-red-200">
          {error}
        </div>
      )}

      <div className="flex justify-end gap-3 pt-2">
        <Button variant="outline" type="button" onClick={onCancel} disabled={submitting}>
          {tCommon("cancel")}
        </Button>
        <Button variant="volt" type="submit" disabled={!isValid || submitting}>
          {submitting ? t("formCreatingBtn") : t("formCreateBtn")}
        </Button>
      </div>
    </form>
  );
}
