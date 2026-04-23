import { resolveLocale } from '@/i18n/resolveLocale';

describe('resolveLocale', () => {
  it('returns cookie value when cookie is "en"', () => {
    expect(resolveLocale('en', '')).toBe('en');
  });

  it('returns cookie value when cookie is "es"', () => {
    expect(resolveLocale('es', 'en-US,en')).toBe('es');
  });

  it('ignores invalid cookie value and falls back to Accept-Language', () => {
    expect(resolveLocale('fr', 'en-US,en')).toBe('en');
  });

  it('returns "es" when Accept-Language contains es', () => {
    expect(resolveLocale(undefined, 'es-CO,es')).toBe('es');
  });

  it('returns "en" when Accept-Language contains en and no es', () => {
    expect(resolveLocale(undefined, 'en-US,en;q=0.9')).toBe('en');
  });

  it('returns default "es" when no cookie and no recognized language', () => {
    expect(resolveLocale(undefined, 'zh-CN,zh')).toBe('es');
  });

  it('returns default "es" when cookie is undefined and Accept-Language is empty', () => {
    expect(resolveLocale(undefined, '')).toBe('es');
  });

  it('prefers "es" over "en" when both appear in Accept-Language (es listed first)', () => {
    expect(resolveLocale(undefined, 'es-CO,es;q=0.9,en;q=0.8')).toBe('es');
  });

  it('respects q-values: returns "es" when es has higher weight even if en appears first', () => {
    expect(resolveLocale(undefined, 'en;q=0.9,es;q=1.0')).toBe('es');
  });
});
