export type Locale = 'en' | 'es';

export function resolveLocale(
  cookieValue: string | undefined,
  acceptLanguage: string
): Locale {
  if (cookieValue === 'en' || cookieValue === 'es') return cookieValue;

  if (!acceptLanguage) return 'es';

  // Parse RFC 7231 Accept-Language header, sort by q-value descending
  const preferred = acceptLanguage
    .split(',')
    .map((part) => {
      const [lang, q] = part.trim().split(';q=');
      return { lang: lang.trim().toLowerCase(), q: q ? parseFloat(q) : 1.0 };
    })
    .sort((a, b) => b.q - a.q);

  for (const { lang } of preferred) {
    if (lang === 'es' || lang.startsWith('es-')) return 'es';
    if (lang === 'en' || lang.startsWith('en-')) return 'en';
  }

  return 'es'; // platform default
}
