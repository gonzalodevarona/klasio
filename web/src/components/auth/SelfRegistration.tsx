"use client";
import StudentForm from "@/components/students/StudentForm";
import { useTranslations } from "next-intl";
import type { CreateStudentRequest } from "@/lib/types/student";

export default function SelfRegistration({ tenantSlug }: { tenantSlug: string }) {
  const t = useTranslations("auth.register");
  async function submit(payload: CreateStudentRequest) {
    const res = await fetch("/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json", "x-tenant-slug": tenantSlug },
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const data = await res.json().catch(() => null);
      throw new Error(data?.error?.message ?? t("errorNetwork"));
    }
  }
  return <StudentForm mode="self" onSubmit={submit} submitLabel={t("submit")} />;
}
