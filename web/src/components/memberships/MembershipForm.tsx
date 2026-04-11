"use client";

import { useState } from "react";
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
      setError(err instanceof Error ? err.message : "Failed to create membership.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Plan</label>
        <select
          value={planId}
          onChange={(e) => setPlanId(e.target.value)}
          required
          className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">— Select a plan —</option>
          {plans.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name} — {p.hours} hours
            </option>
          ))}
        </select>
        {selectedPlan && (
          <div className="mt-2 rounded-lg border border-blue-100 bg-blue-50 px-4 py-3 space-y-1.5">
            <p className="text-sm font-semibold text-blue-900">{selectedPlan.name}</p>
            <div className="grid grid-cols-2 gap-x-6 gap-y-1 text-sm">
              <div>
                <span className="text-blue-600 font-medium">Modality: </span>
                <span className="text-blue-800">
                  {selectedPlan.modality === "HOURS_BASED" ? "Hours-based" : "Classes per week"}
                </span>
              </div>
              {selectedPlan.modality === "HOURS_BASED" && selectedPlan.hours != null && (
                <div>
                  <span className="text-blue-600 font-medium">Hours: </span>
                  <span className="text-blue-800">{selectedPlan.hours}h / month</span>
                </div>
              )}
              <div>
                <span className="text-blue-600 font-medium">Cost: </span>
                <span className="text-blue-800">${Number(selectedPlan.cost).toLocaleString()}</span>
              </div>
            </div>
          </div>
        )}
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          Start date
        </label>
        <input
          type="date"
          value={startDate}
          onChange={(e) => setStartDate(e.target.value)}
          required
          className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <p className="mt-1 text-xs text-gray-500">Membership expires on the last day of this month.</p>
      </div>

      <div className="space-y-2">
        <label className="flex items-center gap-3 text-sm text-gray-700 cursor-pointer">
          <input
            type="checkbox"
            checked={paymentValidated}
            onChange={(e) => {
              setPaymentValidated(e.target.checked);
              if (!e.target.checked) setActivateDirectly(false);
            }}
            className="h-4 w-4 rounded border-gray-300 text-blue-600"
          />
          Payment already validated
        </label>

        {paymentValidated && (
          <label className="flex items-center gap-3 text-sm text-gray-700 cursor-pointer ml-7">
            <input
              type="checkbox"
              checked={activateDirectly}
              onChange={(e) => setActivateDirectly(e.target.checked)}
              className="h-4 w-4 rounded border-gray-300 text-blue-600"
            />
            Activate directly (skip manager delegation)
          </label>
        )}
      </div>

      {error && (
        <div className="rounded-md bg-red-50 p-3 text-sm text-red-700 border border-red-200">
          {error}
        </div>
      )}

      <div className="flex justify-end gap-3 pt-2">
        <button
          type="button"
          onClick={onCancel}
          disabled={submitting}
          className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-50"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={!isValid || submitting}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {submitting ? "Creating..." : "Create Membership"}
        </button>
      </div>
    </form>
  );
}
