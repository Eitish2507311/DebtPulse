import { Form } from 'react-bootstrap';
import { titleCase } from '../utils/format';
import type { FieldOption } from '../types';

type FieldType = 'text' | 'number' | 'date' | 'email' | 'password' | 'textarea' | 'select';

interface FieldProps {
  label?: string;
  name: string;
  type?: FieldType;
  value: string | number | undefined | null;
  onChange: (name: string, value: string) => void;
  options?: FieldOption[];
  error?: string;
  required?: boolean;
  placeholder?: string;
  min?: string | number;
  step?: string | number;
  help?: string;
  disabled?: boolean;
  autoFocus?: boolean;
}

export default function Field({
  label, name, type = 'text', value, onChange, options, error, required,
  placeholder, min, step, help, disabled, autoFocus,
}: FieldProps) {
  const invalid = !!error;
  const handle = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
    onChange(name, e.target.value);
  const common = { name, value: value ?? '', disabled, autoFocus, isInvalid: invalid, onChange: handle, placeholder };

  return (
    <Form.Group className="mb-3">
      {label && <Form.Label className={required ? 'required' : ''}>{label}</Form.Label>}
      {type === 'select' ? (
        <Form.Select {...common}>
          <option value="">— select —</option>
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
