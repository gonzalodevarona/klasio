"use client";

export default function TenantDetailError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="rounded-md bg-red-50 p-6 border border-red-200">
      <h2 className="text-lg font-semibold text-red-800 mb-2">
        Failed to load tenant details
      </h2>
      <p className="text-sm text-red-700 mb-4">{error.message}</p>
      <button
        type="button"
        onClick={reset}
        className="inline-flex items-center rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2"
      >
        Try again
      </button>
    </div>
  );
}
