export const inr = (n: number | string | null | undefined): string => {
  if (n == null || n === '') return '—';
  const val = Number(n);
  if (Number.isNaN(val)) return String(n);
  return '₹' + val.toLocaleString('en-IN', { maximumFractionDigits: 2 });
};

export const num = (n: number | string | null | undefined): string =>
  n == null ? '—' : Number(n).toLocaleString('en-IN');

export const pct = (n: number | string | null | undefined): string =>
  n == null || n === '' ? '—' : `${Number(n).toFixed(1)}%`;

export const date = (d: string | null | undefined): string => {
  if (!d) return '—';
  const dt = new Date(d);
  if (Number.isNaN(dt.getTime())) return String(d);
  return dt.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
};

export const dateTime = (d: string | null | undefined): string => {
  if (!d) return '—';
  const dt = new Date(d);
  if (Number.isNaN(dt.getTime())) return String(d);
  return dt.toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
};

export const today = (): string => new Date().toISOString().slice(0, 10);

export const initials = (name?: string): string =>
  (name || '?').split(' ').map((w) => w[0]).slice(0, 2).join('').toUpperCase();

export const titleCase = (s?: string | null): string =>
  (s || '').toString().toLowerCase().replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
