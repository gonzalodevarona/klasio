"use client";

import { useRef, useState } from "react";

const MAX_FILE_SIZE = 5 * 1024 * 1024;
const ACCEPTED_TYPES = ["image/jpeg", "image/png"];

interface LogoUploadProps {
  onFileSelect: (file: File | null) => void;
  error?: string;
}

export default function LogoUpload({ onFileSelect, error }: LogoUploadProps) {
  const [preview, setPreview] = useState<string | null>(null);
  const [localError, setLocalError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  function handleChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null;

    if (!file) {
      setPreview(null);
      setLocalError(null);
      onFileSelect(null);
      return;
    }

    if (!ACCEPTED_TYPES.includes(file.type)) {
      setPreview(null);
      setLocalError("Only JPEG and PNG images are allowed.");
      onFileSelect(null);
      return;
    }

    if (file.size > MAX_FILE_SIZE) {
      setPreview(null);
      setLocalError("File size must not exceed 5 MB.");
      onFileSelect(null);
      return;
    }

    setLocalError(null);
    setPreview(URL.createObjectURL(file));
    onFileSelect(file);
  }

  function handleRemove() {
    setPreview(null);
    setLocalError(null);
    onFileSelect(null);
    if (inputRef.current) {
      inputRef.current.value = "";
    }
  }

  const displayError = localError ?? error;

  return (
    <div>
      <label
        htmlFor="logo-upload"
        className="block text-sm font-medium text-gray-700 mb-1"
      >
        Logo
      </label>

      {preview ? (
        <div className="flex items-center gap-4">
          <img
            src={preview}
            alt="Logo preview"
            className="h-20 w-20 rounded-lg object-cover border border-gray-200"
          />
          <button
            type="button"
            onClick={handleRemove}
            className="text-sm text-red-600 hover:text-red-800"
          >
            Remove
          </button>
        </div>
      ) : (
        <input
          ref={inputRef}
          id="logo-upload"
          type="file"
          accept="image/jpeg,image/png"
          onChange={handleChange}
          className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-medium file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
        />
      )}

      {displayError && (
        <p className="mt-1 text-sm text-red-600" role="alert">
          {displayError}
        </p>
      )}

      <p className="mt-1 text-xs text-gray-400">
        JPEG or PNG, max 5 MB.
      </p>
    </div>
  );
}
