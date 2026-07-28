import { useState } from 'react';
import { Modal, Button } from 'react-bootstrap';
import { toAppError } from '../api/client.js';
import { useToast } from './ToastHost.jsx';

/**
 * A create/edit modal that manages form state, maps backend fieldErrors onto inputs,
 * and calls `onSubmit(values)`. `fields(values, setField, errors)` renders the body.
 */
export default function FormModal({
  show, title, initial = {}, onClose, onSubmit, onSaved,
  submitLabel = 'Save', size, children,
}) {
  const toast = useToast();
  const [values, setValues] = useState(initial);
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);

  // Re-seed when the modal is (re)opened for a different record.
  const [seed, setSeed] = useState(null);
  if (show && seed !== JSON.stringify(initial)) {
    setSeed(JSON.stringify(initial));
    setValues(initial);
    setErrors({});
  }
  if (!show && seed !== null) setSeed(null);

  const setField = (name, value) => {
    setValues((v) => ({ ...v, [name]: value }));
    if (errors[name]) setErrors((e) => ({ ...e, [name]: undefined }));
  };

  const submit = async (e) => {
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
        <Modal.Body>{typeof children === 'function' ? children(values, setField, errors) : children}</Modal.Body>
        <Modal.Footer>
          <Button variant="light" type="button" onClick={onClose} disabled={busy}>Cancel</Button>
          <Button variant="primary" type="submit" disabled={busy}>
            {busy ? 'Saving…' : submitLabel}
          </Button>
        </Modal.Footer>
      </form>
    </Modal>
  );
}
