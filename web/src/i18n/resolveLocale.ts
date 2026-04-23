type Locale = 'en' | 'es';

export function resolveLocale(
  cookieValue: string | undefined,
  acceptLanguage: string
): Locale {
  if (cookieValue === 'en' || cookieValue === 'es') return cookieValue;

  const normalized = acceptLanguage.toLowerCase();
  // Check in order: if the header contains es before en → 'es'
  const esIndex = normalized.search(/\bes\b/);
  const enIndex = normalized.search(/\ben\b/);

  if (esIndex !== -1 && (enIndex === -1 || esIndex < enIndex)) return 'es';
  if (enIndex !== -1) return 'en';

  return 'es'; // platform default
}
