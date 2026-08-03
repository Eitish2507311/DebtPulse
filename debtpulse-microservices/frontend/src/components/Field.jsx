import { Form } from 'react-bootstrap';
import { titleCase } from '../utils/format.js';

/**
 * A labelled form control bound to a value + onChange, that surfaces backend field errors.
 * type: text | number | date | email | password | textarea | select
 */
export default function Field({
  label, name, type = 'text', value, onChange, options, error, required,
  placeholder, min, step, help, disabled, autoFocus, blankLabel = '— select —',
}) {
  const invalid = !!error;
  const common = {
    name, value: value ?? '', disabled, autoFocus, isInvalid: invalid,
    onChange: (e) => onChange(name, e.target.value),
    placeholder,
  };

  return (
    <Form.Group className="mb-3">
      {label && <Form.Label className={required ? 'required' : ''}>{label}</Form.Label>}
      {type === 'select' ? (
        <Form.Select {...common}>
          <option value="">{blankLabel}</option>
          {(options || []).map((o) => {
            const val = typeof o === 'string' ? o : o.value;
            const lbl = typeof o === 'string' ? titleCase(o) : o.label;
            return <option key={val} value={val}>{lbl}</option>;
          })}
        </Form.Select>
      ) : type === 'textarea' ? (
        <Form.Control as="textarea" rows={3} {...common} />
      ) : (
        <Form.Control type={type} min={min} step={step} {...common} />
      )}
      {help && !invalid && <Form.Text className="text-muted">{help}</Form.Text>}
      <Form.Control.Feedback type="invalid">{error}</Form.Control.Feedback>
    </Form.Group>
  );
}
