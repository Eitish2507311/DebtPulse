import { Spinner } from 'react-bootstrap';
import { statusClass } from '../utils/enums.js';
import { titleCase } from '../utils/format.js';

export function PageHeader({ title, subtitle, actions, icon }) {
  return (
    <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
      <div>
        <h1 className="page-title">{icon && <i className={`bi bi-${icon} me-2`} />}{title}</h1>
        {subtitle && <div className="page-sub">{subtitle}</div>}
      </div>
      {actions && <div className="d-flex gap-2 flex-wrap">{actions}</div>}
    </div>
  );
}

export function StatusBadge({ value }) {
  if (!value) return <span className="text-muted">—</span>;
  return <span className={`badge-pill ${statusClass(value)}`}>{titleCase(value)}</span>;
}

export function Pill({ value, className }) {
  return <span className={`badge-pill ${className || 's-gray'}`}>{titleCase(value)}</span>;
}

export function StatCard({ label, value, icon, tone = 'blue', hint }) {
  const tones = {
    blue: { bg: '#e8f1fb', color: 'var(--dp-blue-600)' },
    green: { bg: '#e6f4ec', color: 'var(--dp-success)' },
    amber: { bg: '#fdf3e0', color: 'var(--dp-warning)' },
    red: { bg: '#fdeceb', color: 'var(--dp-danger)' },
    navy: { bg: '#e7ecf4', color: 'var(--dp-navy)' },
  };
  const t = tones[tone] || tones.blue;
  return (
    <div className="stat-card d-flex align-items-center gap-3">
      {icon && <div className="icon" style={{ background: t.bg, color: t.color }}><i className={`bi bi-${icon}`} /></div>}
      <div className="flex-grow-1">
        <div className="label">{label}</div>
        <div className="value">{value}</div>
        {hint && <div className="page-sub">{hint}</div>}
      </div>
    </div>
  );
}

export function Loading({ label = 'Loading…' }) {
  return (
    <div className="text-center text-muted py-5">
      <Spinner animation="border" size="sm" className="me-2" />{label}
    </div>
  );
}

export function EmptyState({ icon = 'inbox', title = 'Nothing here yet', message }) {
  return (
    <div className="empty-state">
      <div><i className={`bi bi-${icon}`} /></div>
      <div className="fw-semibold mt-2">{title}</div>
      {message && <div className="page-sub">{message}</div>}
    </div>
  );
}

export function ErrorNote({ error }) {
  if (!error) return null;
  return (
    <div className="alert alert-danger d-flex align-items-center gap-2 py-2">
      <i className="bi bi-exclamation-octagon-fill" /><span>{error.message || 'Something went wrong.'}</span>
    </div>
  );
}
