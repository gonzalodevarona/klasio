import { render, RenderOptions } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import React from "react";

// Minimal en messages covering all badge namespaces used in tests
const testMessages = {
  badges: {
    membershipStatus: {
      EXPIRED: "Expired",
      INACTIVE: "Inactive",
      PENDING_PAYMENT: "Pending Payment",
      PENDING_PAYMENT_VALIDATION: "Under Review",
      PENDING_MANAGER_ACTIVATION: "Pending Activation",
      ACTIVE: "Active",
    },
    proofStatus: {
      PENDING: "Pending Review",
      APPROVED: "Approved",
      REJECTED: "Rejected",
      SUPERSEDED: "Superseded",
    },
    registrationStatus: {
      REGISTERED: "Registered",
      CANCELLED_BY_STUDENT: "Cancelled",
      CANCELLED_BY_SYSTEM: "Cancelled (System)",
      SESSION_CANCELLED: "Cancelled by league",
      PRESENT: "Present",
      PRESENT_NO_HOURS: "Present (No Hours)",
      ABSENT: "Absent",
    },
    sessionStatus: {
      SCHEDULED: "Scheduled",
      ALERTED: "Scheduled",
      CANCELLED: "Cancelled",
    },
    classLevel: {
      BEGINNER: "Beginner",
      INTERMEDIATE: "Intermediate",
      ADVANCED: "Advanced",
    },
    classType: {
      RECURRING: "Recurring",
      ONE_TIME: "One-Time",
    },
    enrollmentLevel: {
      BEGINNER: "Beginner",
      INTERMEDIATE: "Intermediate",
      ADVANCED: "Advanced",
    },
    professorStatus: {
      INVITED: "Invited",
      ACTIVE: "Active",
      DEACTIVATED: "Deactivated",
    },
    studentStatus: {
      ACTIVE: "Active",
      INACTIVE: "Inactive",
    },
    hourTransactionType: {
      ATTENDANCE_DEDUCTION: "Attendance",
      MANUAL_ADDITION: "Manual +",
      MANUAL_SUBTRACTION: "Manual −",
    },
  },
};

function Wrapper({ children }: { children: React.ReactNode }) {
  return (
    <NextIntlClientProvider locale="en" messages={testMessages}>
      {children}
    </NextIntlClientProvider>
  );
}

export function renderWithIntl(ui: React.ReactElement, options?: Omit<RenderOptions, "wrapper">) {
  return render(ui, { wrapper: Wrapper, ...options });
}
