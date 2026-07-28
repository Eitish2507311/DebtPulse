import { useState, type ReactNode } from 'react';
import { Modal, Button } from 'react-bootstrap';
import { toAppError } from '../api/client';
import { useToast } from './ToastHost';

// Form state is inherently dynamic (heterogeneous field types), so values are loosely typed.
// The API layer and domain models remain strongly typed at the call site.
export type FormValues = Record<string, any>;
type Errors = Record<string, string | undefined>;
type SetField = (name: string, value: string) => void;

interface FormModalProps {
  show: boolean;
  title: string;
  initial?: FormValues;
  onClose: () => void;
  onSubmit: (values: FormValues) => Promise<unknown>;
  onSaved?: () => void;
  submitLabel?: string;
  size?: 'sm' | 'lg' | 'xl';
  children: (values: FormValues, setField: SetField, errors: Errors) => ReactNode;
}

export default function FormModal({
  show, title, initial = {}, onClose, onSubmit, onSaved, submitLabel = 'Save', size, children,
}: FormModalProps) {
  const toast = useToast();
  const [values, setValues] = useState<FormValues>(initial);
  const [errors, setErrors] = useState<Errors>({});
  const [busy, setBusy] = useState(false);
  const [seed, setSeed] = useState<string | null>(null);

  // Re-seed when the modal is (re)opened for a different record.
  if (show && seed !== JSON.stringify(initial)) {
    setSeed(JSON.stringify(initial));
    setValues(initial);
    setErrors({});
  }
  if (!show && seed !== null) setSeed(null);

  const setField: SetField = (name, value) => {
    setValues((v) => ({ ...v, [name]: value }));
    if (errors[name]) setErrors((e) => ({ ...e, [name]: undefined }));
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true); setErrors({});
    try {
      await onSubmit(values);
      toast.success(`${title} saved`);
      onSaved?.();
      onClose();
    } catch (err) {
      const app = toAppError(err);
      if (Object.keys(app.fieldErrors).length) setErrors(app.fieldErrors);
      else toast.error(app.message, 'Could not save');
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal show={show} onHide={onClose} centered size={size} backdrop="static">
      <form onSubmit={submit} noValidate>
        <Modal.Header closeButton><Modal.Title className="h6 mb-0">{title}</Modal.Title></Modal.Header>
        <Modal.Body>{children(values, setField, errors)}</Modal.Body>
        <Modal.Footer>
          <Button variant="light" type="button" onClick={onClose} disabled={busy}>Cancel</Button>
          <Button variant="primary" type="submit" disabled={busy}>{busy ? 'Saving…' : submitLabel}</Button>
        </Modal.Footer>
      </form>
    </Modal>
  );
}
