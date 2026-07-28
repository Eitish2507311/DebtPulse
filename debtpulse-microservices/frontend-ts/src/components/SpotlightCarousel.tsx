import { type KeyboardEvent } from 'react';
import { useCarousel } from '../hooks/useCarousel';

interface Slide {
  icon: string;
  tag: string;
  title: string;
  text: string;
  points: string[];
}

/** Module spotlights — real DebtPulse capabilities, one per slide. */
const SLIDES: Slide[] = [
  {
    icon: 'folder2-open', tag: 'Portfolio',
    title: 'Delinquent portfolio & DPD bucketing',
    text: 'Onboard loans in bulk and let DebtPulse auto-classify each account into DPD buckets, tracking ageing and exposure in real time.',
    points: ['CSV & API import', 'Auto X30 → NPA bucketing', 'Live exposure & ageing'],
  },
  {
    icon: 'diagram-3', tag: 'Allocation',
    title: 'Rule-based allocation & escalation',
    text: 'A configurable rule engine assigns every account to the right agent and escalates stagnating cases up the chain — automatically.',
    points: ['Least-loaded / round-robin / branch', 'PTP-aware auto-escalation', 'Per-agent capacity limits'],
  },
  {
    icon: 'cash-coin', tag: 'Settlements',
    title: 'Settlements with L1–L3 approvals',
    text: 'Negotiate haircuts with a haircut-driven, multi-level approval chain and a complete, tamper-evident audit trail.',
    points: ['Sequential L1 → L2 → L3 sign-off', 'Approval level as a guarded step', 'Full decision audit trail'],
  },
  {
    icon: 'geo-alt', tag: 'Field & Contact',
    title: 'Field recovery & borrower contact',
    text: 'Schedule on-site visits, capture outcomes and asset sightings, and manage every contact attempt and promise-to-pay to closure.',
    points: ['Scheduled field visits', 'Promise-to-pay tracking', 'Asset sighting & outcomes'],
  },
  {
    icon: 'graph-up', tag: 'Analytics',
    title: 'Real-time recovery analytics',
    text: 'Live dashboards surface recovery rate, bucket ageing and agent performance so managers can act on what is happening now.',
    points: ['Recovery-rate trends', 'Bucket ageing distribution', 'Agent performance'],
  },
];

const INTERVAL_MS = 5500;

export default function SpotlightCarousel() {
  const { index, next, prev, goTo, paused, pause, resume } = useCarousel(SLIDES.length, { intervalMs: INTERVAL_MS });

  const onKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    if (e.key === 'ArrowRight') { e.preventDefault(); next(); }
    if (e.key === 'ArrowLeft') { e.preventDefault(); prev(); }
  };

  return (
    <div
      className="lp-spot"
      role="group"
      aria-roledescription="carousel"
      aria-label="Platform spotlight"
      tabIndex={0}
      onKeyDown={onKeyDown}
      onMouseEnter={pause}
      onMouseLeave={resume}
      onFocus={pause}
      onBlur={resume}
    >
      <div className="lp-car-viewport">
        <div className="lp-car-track" style={{ transform: `translateX(-${index * 100}%)` }}>
          {SLIDES.map((s, i) => (
            <article
              key={s.tag}
              className="lp-car-slide"
              role="group"
              aria-roledescription="slide"
              aria-label={`${i + 1} of ${SLIDES.length}: ${s.title}`}
              aria-hidden={i !== index}
            >
              <div className="lp-car-copy">
                <span className="lp-eyebrow lp-eyebrow--muted">{s.tag}</span>
                <h3>{s.title}</h3>
                <p>{s.text}</p>
                <ul className="lp-car-points">
                  {s.points.map((p) => (
                    <li key={p}><i className="bi bi-check2-circle" />{p}</li>
                  ))}
                </ul>
              </div>
              <div className="lp-car-visual" aria-hidden="true">
                <span className="lp-car-glyph"><i className={`bi bi-${s.icon}`} /></span>
                <span className="lp-car-index">{String(i + 1).padStart(2, '0')}<small>/ {String(SLIDES.length).padStart(2, '0')}</small></span>
              </div>
            </article>
          ))}
        </div>
      </div>

      <div className="lp-car-progress" aria-hidden="true">
        <span
          key={index}
          className="lp-car-progress-fill"
          style={{ animationDuration: `${INTERVAL_MS}ms`, animationPlayState: paused ? 'paused' : 'running' }}
        />
      </div>

      <div className="lp-car-controls">
        <button className="lp-car-arrow" onClick={prev} aria-label="Previous slide"><i className="bi bi-arrow-left" /></button>
        <div className="lp-car-dots" role="tablist" aria-label="Choose slide">
          {SLIDES.map((s, i) => (
            <button
              key={s.tag}
              className={`lp-car-dot ${i === index ? 'active' : ''}`}
              role="tab"
              aria-selected={i === index}
              aria-label={`Go to ${s.tag}`}
              onClick={() => goTo(i)}
            />
          ))}
        </div>
        <button className="lp-car-arrow" onClick={next} aria-label="Next slide"><i className="bi bi-arrow-right" /></button>
      </div>
    </div>
  );
}
