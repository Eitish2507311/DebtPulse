import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { useScrollSpy, useScrolled } from '../hooks/useScrollSpy.js';
import { PreferenceToggle } from '../components/Preferences.jsx';
import SpotlightCarousel from '../components/SpotlightCarousel.jsx';
import '../styles/landing.css';

/* ---- content (data-driven so the JSX stays declarative) ---- */
const SECTIONS = [
  { id: 'platform', label: 'Platform' },
  { id: 'spotlight', label: 'Spotlight' },
  { id: 'workflows', label: 'Workflows' },
  { id: 'roles', label: 'Roles' },
  { id: 'impact', label: 'Impact' },
];

const FEATURES = [
  { icon: 'folder2-open', title: 'Delinquent Portfolio', text: 'Ingest loans and auto-classify DPD buckets (X30 → NPA), tracking ageing and exposure.' },
  { icon: 'diagram-3', title: 'Allocation Engine', text: 'Rule-driven assignment and escalation with least-loaded, round-robin and branch strategies.' },
  { icon: 'telephone', title: 'Contact & PTP', text: 'Log every contact attempt and manage promise-to-pay commitments to closure.' },
  { icon: 'geo-alt', title: 'Field Recovery', text: 'Schedule on-site visits and capture outcomes, borrower contact and asset sightings.' },
  { icon: 'cash-coin', title: 'Settlements', text: 'Multi-level haircut approvals (L1–L3) with a complete, auditable decision trail.' },
  { icon: 'bank', title: 'Legal Proceedings', text: 'Raise and track legal cases, hearings and notices from filing to resolution.' },
  { icon: 'graph-up', title: 'Recovery Analytics', text: 'Live dashboards on recovery rate, bucket ageing and agent performance.' },
  { icon: 'shield-lock', title: 'Bank-grade Security', text: 'JWT auth with refresh-token rotation and role-based access across nine roles.' },
];

const STEPS = [
  { icon: 'upload', title: 'Import', text: 'Onboard delinquent accounts via API or CSV.' },
  { icon: 'diagram-3', title: 'Allocate', text: 'Rules assign each account to the right agent.' },
  { icon: 'telephone', title: 'Engage', text: 'Contact borrowers and secure promises-to-pay.' },
  { icon: 'geo-alt', title: 'Field', text: 'Escalate to on-site visits where needed.' },
  { icon: 'cash-coin', title: 'Resolve', text: 'Settle with approvals or move to legal.' },
  { icon: 'check2-circle', title: 'Recover', text: 'Close the loop and measure recovery.' },
];

const ROLES = [
  { icon: 'shield-check', name: 'Administrator', text: 'Full platform control, user management and audit oversight.' },
  { icon: 'briefcase', name: 'Portfolio Manager', text: 'Owns allocation rules, escalations and portfolio health.' },
  { icon: 'person-badge', name: 'Collections Agent', text: 'Works assigned accounts, logging contacts and PTPs.' },
  { icon: 'geo-alt', name: 'Field Officer', text: 'Conducts field visits and records outcomes.' },
  { icon: 'cash-coin', name: 'Settlement Officer', text: 'Negotiates and processes settlement offers.' },
  { icon: 'bank', name: 'Legal Officer', text: 'Handles legal cases, hearings and notices.' },
];

const STATS = [
  { value: '9', label: 'Collections roles' },
  { value: '6', label: 'Recovery workflows' },
  { value: 'X30–NPA', label: 'DPD bucket ageing' },
  { value: 'L1–L3', label: 'Settlement approvals' },
];

function scrollToId(id) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

export default function LandingPage() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const active = useScrollSpy(['home', ...SECTIONS.map((s) => s.id)]);
  const scrolled = useScrolled(40);
  const [menuOpen, setMenuOpen] = useState(false);

  const primaryCta = isAuthenticated
    ? { label: 'Open app', to: '/dashboard' }
    : { label: 'Sign in', to: '/login' };

  const go = (id) => { setMenuOpen(false); scrollToId(id); };

  return (
    <div className="lp">
      {/* ---- top navigation (transparent over hero, solid on scroll) ---- */}
      <header className={`lp-nav ${scrolled ? 'is-solid' : ''}`}>
        <div className="lp-container lp-nav-inner">
          <button className="lp-brand" onClick={() => go('home')} aria-label="DebtPulse home">
            <span className="lp-brand-mark"><i className="bi bi-graph-down-arrow" /></span>
            Debt<span className="lp-brand-accent">Pulse</span>
          </button>

          <nav className={`lp-links ${menuOpen ? 'open' : ''}`} aria-label="Primary">
            {SECTIONS.map((s) => (
              <button
                key={s.id}
                className={`lp-link ${active === s.id ? 'active' : ''}`}
                aria-current={active === s.id ? 'page' : undefined}
                onClick={() => go(s.id)}
              >
                {s.label}
              </button>
            ))}
            <span className="lp-nav-pref"><PreferenceToggle /></span>
            <button className="btn btn-primary lp-cta-sm" onClick={() => navigate(primaryCta.to)}>
              {primaryCta.label}
            </button>
          </nav>

          <button className="lp-burger" aria-label="Toggle menu" onClick={() => setMenuOpen((o) => !o)}>
            <i className={`bi bi-${menuOpen ? 'x-lg' : 'list'}`} />
          </button>
        </div>
      </header>

      {/* ---- hero ---- */}
      <section id="home" className="lp-hero">
        <div className="lp-container lp-hero-grid">
          <div className="lp-hero-copy">
            <span className="lp-eyebrow"><i className="bi bi-shield-lock me-2" />Debt Recovery &amp; Collections Platform</span>
            <h1 className="lp-title">Recover more, <span className="lp-title-accent">faster</span> — across the entire collections lifecycle.</h1>
            <p className="lp-lead">
              DebtPulse unifies portfolio management, rule-based allocation, field recovery, settlements
              and legal proceedings into one bank-grade platform with real-time recovery analytics.
            </p>
            <div className="lp-hero-actions">
              <button className="btn btn-primary btn-lg" onClick={() => navigate(primaryCta.to)}>
                {primaryCta.label}<i className="bi bi-arrow-right ms-2" />
              </button>
              <button className="btn btn-outline-primary btn-lg" onClick={() => go('platform')}>
                Explore the platform
              </button>
            </div>
            <div className="lp-hero-stats">
              {STATS.map((s) => (
                <div key={s.label} className="lp-hero-stat">
                  <div className="v">{s.value}</div>
                  <div className="l">{s.label}</div>
                </div>
              ))}
            </div>
          </div>

          {/* decorative product preview (illustrative, not live data) */}
          <div className="lp-hero-visual" aria-hidden="true">
            <div className="lp-card-float lp-card-1">
              <div className="lp-mini-head"><span className="dot" /> Recovery Dashboard</div>
              <div className="lp-bars">
                <span style={{ height: '42%' }} /><span style={{ height: '66%' }} />
                <span style={{ height: '54%' }} /><span style={{ height: '80%' }} />
                <span style={{ height: '61%' }} /><span style={{ height: '92%' }} />
              </div>
            </div>
            <div className="lp-card-float lp-card-2">
              <i className="bi bi-diagram-3" />
              <div><strong>Allocation run</strong><small>128 accounts assigned</small></div>
            </div>
            <div className="lp-card-float lp-card-3">
              <i className="bi bi-cash-coin" />
              <div><strong>Settlement L2</strong><small>Approved · 18% haircut</small></div>
            </div>
          </div>
        </div>
      </section>

      {/* ---- platform / features ---- */}
      <Section id="platform" eyebrow="Platform" title="Everything collections, in one place"
        lead="Eight tightly-integrated modules that carry an account from onboarding to recovery.">
        <div className="lp-feature-grid">
          {FEATURES.map((f) => (
            <article key={f.title} className="lp-feature">
              <span className="lp-feature-icon"><i className={`bi bi-${f.icon}`} /></span>
              <h3>{f.title}</h3>
              <p>{f.text}</p>
            </article>
          ))}
        </div>
      </Section>

      {/* ---- spotlight carousel ---- */}
      <Section id="spotlight" eyebrow="Spotlight" title="See the platform in action"
        lead="A rotating look at the modules that carry an account from onboarding to recovery.">
        <SpotlightCarousel />
      </Section>

      {/* ---- workflows ---- */}
      <Section id="workflows" eyebrow="Workflows" title="A guided recovery lifecycle"
        lead="Each account flows through a clear, auditable sequence — automated where it counts."
        tone="tint">
        <ol className="lp-steps">
          {STEPS.map((s, i) => (
            <li key={s.title} className="lp-step">
              <span className="lp-step-num">{i + 1}</span>
              <span className="lp-step-icon"><i className={`bi bi-${s.icon}`} /></span>
              <h4>{s.title}</h4>
              <p>{s.text}</p>
            </li>
          ))}
        </ol>
      </Section>

      {/* ---- roles ---- */}
      <Section id="roles" eyebrow="Access control" title="Built around your collections roles"
        lead="Role-based access keeps every user focused on exactly what they own.">
        <div className="lp-role-grid">
          {ROLES.map((r) => (
            <article key={r.name} className="lp-role">
              <span className="lp-role-icon"><i className={`bi bi-${r.icon}`} /></span>
              <div>
                <h4>{r.name}</h4>
                <p>{r.text}</p>
              </div>
            </article>
          ))}
        </div>
      </Section>

      {/* ---- impact / CTA band ---- */}
      <section id="impact" className="lp-impact">
        <div className="lp-container">
          <h2>Turn delinquency into recovery.</h2>
          <p>Sign in to your DebtPulse workspace and pick up right where your portfolio needs you.</p>
          <button className="btn btn-accent btn-lg" onClick={() => navigate(primaryCta.to)}>
            {primaryCta.label}<i className="bi bi-arrow-right ms-2" />
          </button>
        </div>
      </section>

      {/* ---- footer ---- */}
      <footer className="lp-footer">
        <div className="lp-container lp-footer-inner">
          <div>
            <div className="lp-brand lp-brand--footer">
              <span className="lp-brand-mark"><i className="bi bi-graph-down-arrow" /></span>
              Debt<span className="lp-brand-accent">Pulse</span>
            </div>
            <p className="lp-foot-tag">Debt Recovery &amp; Collections Management</p>
          </div>
          <nav className="lp-foot-links" aria-label="Footer">
            {SECTIONS.map((s) => (
              <button key={s.id} className="lp-foot-link" onClick={() => go(s.id)}>{s.label}</button>
            ))}
            <button className="lp-foot-link" onClick={() => navigate('/login')}>Sign in</button>
          </nav>
        </div>
        <div className="lp-copy">© {new Date().getFullYear()} DebtPulse · For authorised collections staff only.</div>
      </footer>
    </div>
  );
}

/** Reusable section shell — keeps every content block consistent. */
function Section({ id, eyebrow, title, lead, tone, children }) {
  return (
    <section id={id} className={`lp-section ${tone === 'tint' ? 'lp-section--tint' : ''}`}>
      <div className="lp-container">
        <header className="lp-section-head">
          <span className="lp-eyebrow lp-eyebrow--muted">{eyebrow}</span>
          <h2>{title}</h2>
          {lead && <p className="lp-section-lead">{lead}</p>}
        </header>
        {children}
      </div>
    </section>
  );
}
