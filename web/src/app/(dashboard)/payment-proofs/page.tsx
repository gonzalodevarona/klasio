import { getTranslations } from "next-intl/server";
import { ProofQueue } from "@/components/payment-proofs/ProofQueue";

export default async function PaymentProofsPage() {
  const t = await getTranslations("paymentProofs");

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">{t("pageTitle")}</h1>
        <p className="mt-1 text-sm text-gray-500">
          {t("pageSubtitle")}
        </p>
      </div>
      <ProofQueue />
    </div>
  );
}
