"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { api, ApiError } from "@/lib/api";
import {
  ProgramDetail,
  CreateProgramRequest,
  UpdateProgramRequest,
} from "@/lib/types/program";
import { Input, Button } from "@/components/ui";

interface FieldErrors {
  name?: string;
  [key: string]: string | undefined;
}

interface ProgramFormProps {
  program?: ProgramDetail;
}

export default function ProgramForm({ program }: ProgramFormProps) {
  const t = useTranslations("programs");
  const tCommon = useTranslations("common");
  const router = useRouter();
  const isEdit = !!program;

  const [name, setName] = useState(program?.name ?? "");
  const [dropInPrice, setDropInPrice] = useState(program?.dropInPrice ?? "");

  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [apiError, setApiError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function validate(): FieldErrors {
    const errors: FieldErrors = {};

    if (!name.trim()) {
      errors.name = t("formNameProgramRequired");
    } else if (name.trim().length > 150) {
      errors.name = t("formNameProgramMaxLength");
    }

    if (dropInPrice.trim() !== "") {
      const val = parseFloat(dropInPrice);
      if (isNaN(val) || val <= 0) {
        errors.dropInPrice = t("formDropInPriceInvalid");
      }
    }

    return errors;
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
      const parsedDropInPrice = dropInPrice.trim() !== "" ? dropInPrice.trim() : null;

      if (isEdit) {
        const body: UpdateProgramRequest = {
          name: name.trim(),
          dropInPrice: parsedDropInPrice,
        };
        await api.put<ProgramDetail>(`/programs/${program.id}`, body);
        router.push(`/programs/${program.id}`);
      } else {
        const body: CreateProgramRequest = {
          name: name.trim(),
          dropInPrice: parsedDropInPrice,
        };
        const created = await api.post<ProgramDetail>("/programs", body);
        router.push(`/programs/${created.id}`);
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
        label={t("formNameProgramLabel")}
        type="text"
        value={name}
        onChange={(e) => setName(e.target.value)}
        required
        placeholder={t("formNameProgramPlaceholder")}
        error={fieldErrors.name}
      />

      {/* Drop-in price */}
      <div>
        <Input
          label={t("formDropInPriceLabel")}
          type="number"
          min="0.01"
          step="1"
          value={dropInPrice}
          onChange={(e) => setDropInPrice(e.target.value)}
          placeholder={t("formDropInPricePlaceholder")}
          error={fieldErrors.dropInPrice}
        />
        <p className="mt-1 text-xs text-gray-500">{t("formDropInPriceHint")}</p>
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
              : t("formCreateProgramButton")}
        </Button>
      </div>
    </form>
  );
}
