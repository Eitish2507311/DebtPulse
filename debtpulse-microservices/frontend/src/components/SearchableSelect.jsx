import { useState, useEffect, useRef } from 'react';
import { Form, ListGroup } from 'react-bootstrap';

/**
 * Type-to-search autocomplete dropdown backed by a fixed option list. A value can only be set by
 * PICKING an option (free text is never committed), so callers can't submit an invalid id.
 *
 * Props:
 *  - value, onChange(value)   the selected option value
 *  - loadOptions()            async () => [{ value, label }]  (loaded once on mount)
 *  - options                  static [{ value, label }] alternative to loadOptions
 *  - placeholder, size
 */
export default function SearchableSelect({ value, onChange, loadOptions, options: staticOptions, placeholder = 'Search…', size = 'sm' }) {
  const [options, setOptions] = useState(staticOptions || []);
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const boxRef = useRef(null);

  useEffect(() => {
    if (staticOptions) { setOptions(staticOptions); return undefined; }
    if (!loadOptions) return undefined;
    let alive = true;
    loadOptions().then((o) => { if (alive) setOptions(o || []); }).catch(() => {});
    return () => { alive = false; };
    // Load once on mount — the option set is stable for the lifetime of the field.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const onDoc = (e) => { if (boxRef.current && !boxRef.current.contains(e.target)) setOpen(false); };
    document.addEventListener('mousedown', onDoc);
    return () => document.removeEventListener('mousedown', onDoc);
  }, []);

  const q = query.toLowerCase();
  const filtered = options.filter((o) =>
    !q || o.value.toLowerCase().includes(q) || (o.label || '').toLowerCase().includes(q)).slice(0, 8);
  const selectedLabel = options.find((o) => o.value === value)?.label;

  return (
    <div className="position-relative" ref={boxRef}>
      <Form.Control
        size={size}
        placeholder={placeholder}
        value={open ? query : (value ? (selectedLabel ? `${value} — ${selectedLabel}` : value) : query)}
        onFocus={() => { setOpen(true); setQuery(''); }}
        onChange={(e) => { setQuery(e.target.value); setOpen(true); }}
      />
      {open && filtered.length > 0 && (
        <ListGroup className="position-absolute w-100 shadow-sm"
          style={{ zIndex: 1060, maxHeight: 240, overflowY: 'auto' }}>
          {filtered.map((o) => (
            <ListGroup.Item action key={o.value} className="py-1"
              onMouseDown={() => { onChange(o.value); setQuery(''); setOpen(false); }}>
              <span className="text-mono">{o.value}</span>
              {o.label && o.label !== o.value ? <span className="text-muted"> — {o.label}</span> : null}
            </ListGroup.Item>
          ))}
        </ListGroup>
      )}
      {open && filtered.length === 0 && (
        <ListGroup className="position-absolute w-100 shadow-sm" style={{ zIndex: 1060 }}>
          <ListGroup.Item className="py-1 text-muted small">No matching ids</ListGroup.Item>
        </ListGroup>
      )}
    </div>
  );
}
