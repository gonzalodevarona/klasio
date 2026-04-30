import { getTranslations } from "next-intl/server";
import { ProofQueue } from "@/components/payment-proofs/ProofQueue";

export default async function PaymentProofsPage() {
  const t = await getTranslations("paymentProofs");

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
        <p className="font-[var(--font-mono)] text-xs text-k-muted mt-1">
          {t("pageSubtitle")}
        </p>
      </div>
      <ProofQueue />
    </div>
  );
}
