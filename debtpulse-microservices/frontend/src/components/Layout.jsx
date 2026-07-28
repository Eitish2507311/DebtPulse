import { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Dropdown } from 'react-bootstrap';
import { useAuth } from '../auth/AuthContext.jsx';
import { ACCESS, ROLE_LABELS, hasAny } from '../auth/roles.js';
import { notificationApi } from '../api/services.js';
import { initials } from '../utils/format.js';
import { PreferenceToggle } from './Preferences.jsx';

const NAV = [
  { to: '/dashboard', label: 'Dashboard', icon: 'speedometer2', key: 'dashboard' },
  { section: 'Collections' },
  { to: '/portfolio', label: 'Portfolio', icon: 'folder2-open', key: 'portfolio' },
  { to: '/contacts', label: 'Contact & PTP', icon: 'telephone', key: 'contact' },
  { to: '/field', label: 'Field Recovery', icon: 'geo-alt', key: 'field' },
  { section: 'Resolution' },
  { to: '/settlements', label: 'Settlements', icon: 'cash-coin', key: 'settlement' },
  { to: '/legal', label: 'Legal', icon: 'bank', key: 'legal' },
  { section: 'Insights' },
  { to: '/analytics', label: 'Analytics', icon: 'graph-up', key: 'analytics' },
  { to: '/notifications', label: 'Notifications', icon: 'bell', key: 'notifications' },
  { section: 'Administration' },
  { to: '/admin/users', label: 'User Management', icon: 'people', key: 'admin' },
  { to: '/admin/audit', label: 'Audit Trail', icon: 'shield-check', key: 'audit' },
  { to: '/admin/allocations', label: 'Allocation Rules', icon: 'diagram-3', key: 'allocations' },
];

const TITLES = {
  '/dashboard': 'Dashboard', '/portfolio': 'Delinquent Portfolio', '/contacts': 'Contact & Follow-Up',
  '/field': 'Field Recovery', '/settlements': 'Settlement & Restructuring', '/legal': 'Legal Proceedings',
  '/analytics': 'Recovery Analytics', '/notifications': 'Notifications', '/admin/users': 'User Management',
  '/admin/audit': 'Audit Trail', '/admin/allocations': 'Allocation Rules',
};

export default function Layout() {
  const { user, role, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);

  useEffect(() => { setOpen(false); }, [location.pathname]);

  useEffect(() => {
    let alive = true;
    const load = () => notificationApi.unreadCount()
      .then((r) => alive && setUnread(r.data.unreadCount || 0)).catch(() => {});
    load();
    const t = setInterval(load, 30000);
    return () => { alive = false; clearInterval(t); };
  }, [location.pathname]);

  const crumb = TITLES[location.pathname]
    || Object.entries(TITLES).find(([p]) => location.pathname.startsWith(p))?.[1]
    || 'DebtPulse';

  const visible = NAV.filter((n) => n.section || hasAny(role, ACCESS[n.key] || []));

  return (
    <div className="app-shell">
      <aside className={`sidebar ${open ? 'open' : ''}`}>
        <div className="brand"><span className="dot" /> DebtPulse</div>
        <nav className="nav-scroll">
          {visible.map((n, i) => n.section
            ? <div key={`s${i}`} className="nav-section">{n.section}</div>
            : (
              <NavLink key={n.to} to={n.to} className="nav-link">
                <i className={`bi bi-${n.icon}`} />{n.label}
              </NavLink>
            ))}
        </nav>
        <div className="side-foot">
          Signed in as<br /><span className="text-white">{ROLE_LABELS[role] || role}</span>
        </div>
      </aside>

      <div className="main-wrap">
        <header className="topbar">
          <button className="btn btn-sm btn-light d-lg-none" onClick={() => setOpen((o) => !o)}>
            <i className="bi bi-list" />
          </button>
          <span className="crumb">{crumb}</span>
          <span className="spacer" />

          <PreferenceToggle />

          <button className="btn btn-sm position-relative me-1" title="Notifications"
                  onClick={() => navigate('/notifications')}>
            <i className="bi bi-bell fs-5" />
            {unread > 0 && <span className="badge rounded-pill bg-danger notif-badge">{unread > 9 ? '9+' : unread}</span>}
          </button>

          <Dropdown align="end">
            <Dropdown.Toggle variant="light" size="sm" className="d-flex align-items-center gap-2 border-0">
              <span className="avatar">{initials(user?.name)}</span>
              <span className="d-none d-md-inline text-start">
                <span className="fw-semibold d-block" style={{ lineHeight: 1 }}>{user?.name}</span>
                <small className="text-muted">{ROLE_LABELS[role]}</small>
              </span>
            </Dropdown.Toggle>
            <Dropdown.Menu>
              <Dropdown.Header>
                {user?.email}<br /><small className="text-muted">Branch {user?.branchId || '—'} · {user?.userId}</small>
              </Dropdown.Header>
              <Dropdown.Item onClick={() => navigate('/change-password')}>
                <i className="bi bi-key me-2" />Change password
              </Dropdown.Item>
              <Dropdown.Divider />
              <Dropdown.Item onClick={() => { logout(); navigate('/login'); }} className="text-danger">
                <i className="bi bi-box-arrow-right me-2" />Sign out
              </Dropdown.Item>
            </Dropdown.Menu>
          </Dropdown>
        </header>

        <main className="page-body"><Outlet /></main>
      </div>
    </div>
  );
}
