"use client";

import { useState, useRef, useEffect } from "react";
import { COUNTRIES, PINNED_COUNTRIES, Country } from "@/lib/countries";

interface CountrySelectProps {
  value: Country | null;
  onChange: (country: Country) => void;
  error?: string;
}

export default function CountrySelect({ value, onChange, error }: CountrySelectProps) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
        setSearch("");
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const trimmed = search.trim().toLowerCase();
  const filtered = trimmed
    ? COUNTRIES.filter((c) => c.name.toLowerCase().includes(trimmed))
    : null;

  function handleSelect(country: Country) {
    onChange(country);
    setOpen(false);
    setSearch("");
  }

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className={`flex w-full items-center justify-between rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white ${
          error ? "border-red-500" : "border-gray-300"
        } ${value ? "text-gray-900" : "text-gray-400"}`}
      >
        <span>{value ? `${value.flag} ${value.name}` : "Select country..."}</span>
        <span className="ml-2 text-gray-400">▾</span>
      </button>

      {open && (
        <div className="absolute z-10 mt-1 w-full rounded-md border border-gray-200 bg-white shadow-lg">
          <div className="border-b border-gray-100 px-3 py-2">
            <input
              autoFocus
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search country..."
              className="w-full text-sm outline-none"
            />
          </div>
          <ul className="max-h-52 overflow-auto py-1">
            {filtered ? (
              filtered.length === 0 ? (
                <li className="px-3 py-2 text-sm text-gray-400">No results</li>
              ) : (
                filtered.map((c) => (
                  <li
                    key={c.name}
                    onClick={() => handleSelect(c)}
                    className="cursor-pointer px-3 py-1.5 text-sm hover:bg-blue-50"
                  >
                    {c.flag} {c.name}
                  </li>
                ))
              )
            ) : (
              <>
                {PINNED_COUNTRIES.map((c) => (
                  <li
                    key={`pinned-${c.name}`}
                    onClick={() => handleSelect(c)}
                    className="cursor-pointer px-3 py-1.5 text-sm font-medium text-blue-700 hover:bg-blue-50"
                  >
                    {c.flag} {c.name}
                  </li>
                ))}
                <li className="mx-3 my-1 border-t border-gray-100" />
                {COUNTRIES.map((c) => (
                  <li
                    key={c.name}
                    onClick={() => handleSelect(c)}
                    className="cursor-pointer px-3 py-1.5 text-sm hover:bg-blue-50"
                  >
                    {c.flag} {c.name}
                  </li>
                ))}
              </>
            )}
          </ul>
        </div>
      )}

      {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
    </div>
  );
}
