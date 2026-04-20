"use client";

import { useState } from "react";
import { Edit3 } from "lucide-react";
import {
  useRaiseSessionAlert,
  useUpdateSessionAlert,
  useCancelSession,
} from "@/hooks/useSessionActions";
import SessionReasonModal from "./SessionReasonModal";

interface Props {
  classId: string;
  /** ISO YYYY-MM-DD */
  sessionDate: string;
  status: string;
  alertReason?: string | null;
  isFuture: boolean;
  canManage: boolean;
  onActionCompleted: () => void;
}

type ActiveModal = "raise" | "update" | "cancel" | null;

export default function SessionActionsPanel({
  classId,
  sessionDate,
  status,
  alertReason,
  isFuture,
  canManage,
  onActionCompleted,
}: Props) {
  const [activeModal, setActiveModal] = useState<ActiveModal>(null);

  const { raiseAlert, loading: raisingLoading, error: raisingError, clearError: clearRaisingError } = useRaiseSessionAlert();
  const { updateAlert, loading: updatingLoading, error: updatingError, clearError: clearUpdatingError } = useUpdateSessionAlert();
  const { cancelSession, loading: cancellingLoading, error: cancellingError, clearError: clearCancellingError } = useCancelSession();

  // Guard: only render for authorized users on future sessions with actionable status.
  if (!canManage || !isFuture) return null;
  if (status !== "SCHEDULED" && status !== "ALERTED") return null;

  const closeModal = () => {
    setActiveModal(null);
    clearRaisingError();
    clearUpdatingError();
    clearCancellingError();
  };

  const handleRaise = async (reason: string) => {
    await raiseAlert({ classId, sessionDate, reason });
    closeModal();
    onActionCompleted();
  };

  const handleUpdate = async (reason: string) => {
    await updateAlert({ classId, sessionDate, reason });
    closeModal();
    onActionCompleted();
  };

  const handleCancel = async (reason: string) => {
    await cancelSession({ classId, sessionDate, reason });
    closeModal();
    onActionCompleted();
  };

  return (
    <>
      <div className="flex items-center gap-2">
        {status === "SCHEDULED" && (
          <button
            type="button"
            onClick={() => setActiveModal("raise")}
            className="inline-flex items-center gap-1.5 rounded-md bg-amber-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-amber-700 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:ring-offset-2"
          >
            Raise alert
          </button>
        )}

        {status === "ALERTED" && (
          <button
            type="button"
            onClick={() => setActiveModal("update")}
            className="inline-flex items-center gap-1.5 rounded-md bg-amber-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-amber-700 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:ring-offset-2"
          >
            <Edit3 className="w-3.5 h-3.5" />
            Update alert
          </button>
        )}

        <button
          type="button"
          onClick={() => setActiveModal("cancel")}
          className="inline-flex items-center gap-1.5 rounded-md bg-red-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2"
        >
          Cancel session
        </button>
      </div>

      {/* Raise alert modal */}
      <SessionReasonModal
        open={activeModal === "raise"}
        title="Raise Session Alert"
        description="Students registered for this session will be notified."
        submitLabel="Raise Alert"
        submitVariant="amber"
        onClose={closeModal}
        onSubmit={handleRaise}
        submitting={raisingLoading}
        error={raisingError}
      />

      {/* Update alert modal */}
      <SessionReasonModal
        open={activeModal === "update"}
        title="Update Session Alert"
        description="The updated alert will be sent to all registered students."
        submitLabel="Update Alert"
        submitVariant="amber"
        initialReason={alertReason ?? ""}
        onClose={closeModal}
        onSubmit={handleUpdate}
        submitting={updatingLoading}
        error={updatingError}
      />

      {/* Cancel session modal */}
      <SessionReasonModal
        open={activeModal === "cancel"}
        title="Cancel Session"
        description="This action cannot be undone. All registered students will be notified."
        submitLabel="Cancel Session"
        submitVariant="red"
        onClose={closeModal}
        onSubmit={handleCancel}
        submitting={cancellingLoading}
        error={cancellingError}
      />
    </>
  );
}
