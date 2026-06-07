const RESERVED = new Set(["www", "app"]);

export function tenantSlugFromHost(host: string | null, rootDomain: string): string | null {
  if (!host) return null;
  const hostname = host.split(":")[0].toLowerCase().trim();
  const root = rootDomain.toLowerCase().trim();
  if (!hostname || hostname === root) return null;
  if (!hostname.endsWith(`.${root}`)) return null;

  const sub = hostname.slice(0, hostname.length - root.length - 1);
  if (!sub || sub.includes(".")) return null;
  if (RESERVED.has(sub)) return null;
  return sub;
}
