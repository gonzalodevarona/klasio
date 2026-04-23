import { render, RenderOptions } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import React from "react";

// Minimal en messages covering all namespaces used in tests
const testMessages = {
  common: {
    save: "Save",
    cancel: "Cancel",
    delete: "Delete",
    edit: "Edit",
    loading: "Loading...",
    noResults: "No results found",
    previous: "Previous",
    next: "Next",
    page: "Page",
    of: "of",
    totalCount: "{count} total",
    active: "Active",
    inactive: "Inactive",
    search: "Search",
    clear: "Clear",
    actions: "Actions",
    confirm: "Confirm",
    back: "Back",
    close: "Close",
    create: "Create",
    view: "View",
    deactivate: "Deactivate",
    reactivate: "Reactivate",
    deactivating: "Deactivating...",
    reactivating: "Reactivating...",
    saving: "Saving...",
    creating: "Creating...",
    all: "All",
    yes: "Yes",
    no: "No",
    unexpectedError: "An unexpected error occurred. Please try again.",
  },
  validation: {
    firstName: {
      required: "First name is required.",
      maxLength: "First name must be at most 100 characters.",
    },
    lastName: {
      required: "Last name is required.",
      maxLength: "Last name must be at most 100 characters.",
    },
    email: {
      required: "Email is required.",
      invalid: "Please enter a valid email address.",
      maxLength: "Email must be at most 255 characters.",
    },
    phone: {
      required: "Phone number is required.",
      invalid: "Enter a valid WhatsApp number in E.164 format, e.g. +573001234567",
      maxLength: "Phone number must be at most 20 characters.",
    },
    identityNumber: {
      required: "Identity number is required.",
      maxLength: "Identity number must be at most 30 characters.",
    },
    documentType: { required: "Document type is required." },
    documentNumber: {
      required: "Document number is required.",
      maxLength: "Document number must be at most 30 characters.",
    },
  },
  pagination: {
    previous: "Previous",
    next: "Next",
    summary: "Page {current} of {total} ({count} total)",
  },
  professors: {
    pageTitle: "Professors",
    filterAll: "All",
    filterActive: "Active",
    filterDeactivated: "Deactivated",
    listLoading: "Loading professors...",
    listEmpty: "No professors match the current filter.",
    addButton: "Add Professor",
    colName: "Name",
    colEmail: "Email",
    colPhone: "Phone",
    colDocument: "Document",
    colStatus: "Status",
    colCreated: "Created",
    colActions: "Actions",
    modalDeactivateTitle: "Deactivate Professor",
    modalDeactivateConfirm: "Are you sure you want to deactivate {name}?",
    modalDeactivateHint: "The account will be disabled immediately. You can re-activate it at any time.",
    modalCancelButton: "Cancel",
    modalDeactivateButton: "Deactivate",
    modalDeactivatingButton: "Deactivating...",
    formFirstNameLabel: "First Name *",
    formFirstNamePlaceholder: "e.g. Carlos",
    formLastNameLabel: "Last Name *",
    formLastNamePlaceholder: "e.g. Martinez",
    formEmailLabel: "Email *",
    formEmailPlaceholder: "e.g. carlos@example.com",
    formPhoneLabel: "Phone Number (must be valid for WhatsApp)",
    formPhonePlaceholder: "e.g. +573001234567",
    formSaveButton: "Save Changes",
    formCreateButton: "Create Professor",
    formSavingButton: "Saving...",
    formCreatingButton: "Creating...",
    detailFirstName: "First Name",
    detailLastName: "Last Name",
    detailEmail: "Email",
    detailPhone: "Phone Number",
    detailDocType: "Document Type",
    detailDocNumber: "Document Number",
    detailStatus: "Status",
    detailCreatedAt: "Created At",
    detailCreatedBy: "Created By",
    detailUpdatedAt: "Last Updated",
    detailUpdatedBy: "Updated By",
    detailEditButton: "Edit",
    detailDeactivateButton: "Deactivate Professor",
    detailDeactivatingButton: "Deactivating...",
    detailReactivateButton: "Reactivate Professor",
    detailReactivatingButton: "Reactivating...",
    detailDeactivateConfirm: "Are you sure you want to deactivate this professor?",
    detailDeactivateModalTitle: "Confirm Deactivation",
    detailCancelButton: "Cancel",
    detailSuccessFeedback: "Professor has been {action} successfully.",
    detailErrorFeedback: "Failed to {action} professor. Please try again.",
  },
  commonFields: {
    documentTypeLabel: "Document Type *",
    documentNumberLabel: "Document Number *",
    documentNumberPlaceholder: "e.g. 1234567890",
  },
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
