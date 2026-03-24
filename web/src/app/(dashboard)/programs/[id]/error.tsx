"use client";

export default function ProgramDetailError({
  error,
  reset,
}: {
  error: Error;
  reset: () => void;
}) {
  return (
    <div className="rounded-md bg-red-50 p-4 border border-red-200">
      <h2 className="text-sm font-medium text-red-800">
        Failed to load program
      </h2>
      <p className="mt-1 text-sm text-red-700">{error.message}</p>
      <button
        type="button"
        onClick={reset}
        className="mt-3 inline-flex items-center rounded-md bg-red-100 px-3 py-2 text-sm font-medium text-red-800 hover:bg-red-200"
      >
        Try again
      </button>
    </div>
  );
}
