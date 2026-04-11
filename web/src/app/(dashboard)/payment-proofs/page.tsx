import { ProofQueue } from "@/components/payment-proofs/ProofQueue";

export default function PaymentProofsPage() {
  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Payment Proof Queue</h1>
        <p className="mt-1 text-sm text-gray-500">
          Review and validate pending payment proofs.
        </p>
      </div>
      <ProofQueue />
    </div>
  );
}
