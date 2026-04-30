"use client";

import { use, useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useProgramDetail } from "@/hooks/usePrograms";
import { useProgramPlanDetail } from "@/hooks/useProgramPlans";
import { api, ApiError } from "@/lib/api";
import { Button } from "@/components/ui";
import ProgramStatusBadge from "@/components/programs/ProgramStatusBadge";

interface PlanDetailPageProps {
  params: Promise<{ id: string; planId: string }>;
}

export default function PlanDetailPage({ params }: PlanDetailPageProps) {
  const t = useTranslations("programs");
  const tCommon = useTranslations("common");
  const { id, planId } = use(params);
  const { program } = useProgramDetail(id);
  const { plan, loading, error, refetch } = useProgramPlanDetail(id, planId);

  const [actionLoading, setActionLoading] = useState(false);
  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  async function handleStatusAction(action: "deactivate" | "reactivate") {
    setActionLoading(true);
    setFeedback(null);

    try {
      await api.post(`/programs/${id}/plans/${planId}/${action}`);
      const label = action === "deactivate" ? "deactivated" : "reactivated";
      setFeedback({
        type: "success",
        message: t("planDetailSuccessFeedback", { action: label }),
      });
      refetch();
    } catch (err) {
      const message =
        err instanceof ApiError
          ? err.message
          : t("planDetailErrorFeedback", { action });
      setFeedback({ type: "error", message });
    } finally {
      setActionLoading(false);
    }
  }

  function formatCost(cost: number): string {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "COP",
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(cost);
  }

  function formatDate(dateString: string | null): string {
    if (!dateString) return "-";
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "long",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  }

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/programs/${id}`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/programs" className="hover:text-k-subtle">{t("detailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <Link href={`/programs/${id}`} className="hover:text-k-subtle">{program?.name ?? id}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{plan?.name ?? t("planDetailBreadcrumb")}</span>
        </nav>
      </div>

      {loading && (
        <div className="text-center py-8 text-sm text-k-muted">
          {t("planDetailLoadingText")}
        </div>
      )}

      {error && (
        <div
          className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text"
          role="alert"
        >
          {error}
        </div>
      )}

      {feedback && (
        <div
          className={`rounded-k-sm p-4 text-sm border mb-4 ${
            feedback.type === "success"
              ? "bg-k-volt/10 text-k-volt-text border-k-volt/30"
              : "bg-k-danger-bg border-k-danger-text/30 text-k-danger-text"
          }`}
          role="alert"
        >
          {feedback.message}
        </div>
      )}

      {plan && (
        <div className="space-y-6">
          <div className="bg-k-surface border border-k-border rounded-k-lg overflow-hidden">
            <div className="px-6 py-5 border-b border-k-line flex items-center justify-between">
              <div>
                <h2 className="text-xl font-semibold text-k-dark">
                  {plan.name}
                </h2>
                <p className="text-sm text-k-muted mt-1">
                  {formatCost(plan.cost)}
                </p>
              </div>
              <div className="flex items-center gap-3">
                <ProgramStatusBadge status={plan.status} />
                {plan.status === "ACTIVE" && (
                  <Button variant="outline" size="sm" asChild>
                    <Link href={`/programs/${id}/plans/${planId}/edit`}>
                      {t("planDetailEditButton")}
                    </Link>
                  </Button>
                )}
              </div>
            </div>

            <div className="px-6 py-5">
              <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-4">
                <div>
                  <dt className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">{t("planDetailModality")}</dt>
                  <dd className="mt-1 text-sm font-medium text-k-dark">
                    {plan.modality === "HOURS_BASED"
                      ? t("modalityHoursBased")
                      : plan.modality === "CLASSES_PER_WEEK"
                        ? t("modalityClassesPerWeek")
                        : plan.modality}
                  </dd>
                </div>

                <div>
                  <dt className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">{t("planDetailManager")}</dt>
                  <dd className="mt-1 text-sm font-medium text-k-dark">
                    {plan.managerName ?? plan.managerId}
                  </dd>
                </div>

                {plan.hours != null && (
                  <div>
                    <dt className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">{t("planDetailHours")}</dt>
                    <dd className="mt-1 text-sm font-medium text-k-dark">
                      {plan.hours}
                    </dd>
                  </div>
                )}

                <div>
                  <dt className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
                    {t("planDetailCreatedAt")}
                  </dt>
                  <dd className="mt-1 text-sm font-medium text-k-dark">
                    {formatDate(plan.createdAt)}
                  </dd>
                </div>

                <div>
                  <dt className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
                    {t("planDetailCreatedBy")}
                  </dt>
                  <dd className="mt-1 text-sm font-medium text-k-dark">
                    {plan.createdBy}
                  </dd>
                </div>

                {plan.updatedAt && (
                  <>
                    <div>
                      <dt className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
                        {t("planDetailLastUpdated")}
                      </dt>
                      <dd className="mt-1 text-sm font-medium text-k-dark">
                        {formatDate(plan.updatedAt)}
                      </dd>
                    </div>
                    <div>
                      <dt className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
                        {t("planDetailUpdatedBy")}
                      </dt>
                      <dd className="mt-1 text-sm font-medium text-k-dark">
                        {plan.updatedBy}
                      </dd>
                    </div>
                  </>
                )}
              </dl>
            </div>

            {plan.scheduleEntries.length > 0 && (
              <div className="px-6 py-5 border-t border-k-line">
                <h3 className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted mb-3">
                  {t("planDetailScheduleTitle")}
                </h3>
                <div className="space-y-2">
                  {plan.scheduleEntries.map((entry, index) => (
                    <div
                      key={index}
                      className="flex items-center gap-3 text-sm text-k-dark bg-k-bg rounded-k-sm px-3 py-2"
                    >
                      <span className="font-medium w-24">
                        {{
                          MONDAY: t("dayMonday"),
                          TUESDAY: t("dayTuesday"),
                          WEDNESDAY: t("dayWednesday"),
                          THURSDAY: t("dayThursday"),
                          FRIDAY: t("dayFriday"),
                          SATURDAY: t("daySaturday"),
                          SUNDAY: t("daySunday"),
                        }[entry.dayOfWeek] ?? entry.dayOfWeek}
                      </span>
                      <span>
                        {entry.startTime} - {entry.endTime}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <div className="px-6 py-4 border-t border-k-line bg-k-bg">
              {plan.status === "ACTIVE" && (
                <Button
                  variant="danger"
                  onClick={() => handleStatusAction("deactivate")}
                  disabled={actionLoading}
                >
                  {actionLoading ? t("planDetailDeactivatingButton") : t("planDetailDeactivateButton")}
                </Button>
              )}
              {plan.status === "INACTIVE" && (
                <Button
                  variant="volt"
                  onClick={() => handleStatusAction("reactivate")}
                  disabled={actionLoading}
                >
                  {actionLoading ? t("planDetailReactivatingButton") : t("planDetailReactivateButton")}
                </Button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
