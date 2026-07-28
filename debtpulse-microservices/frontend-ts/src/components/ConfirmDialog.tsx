import type { ReactNode } from 'react';
import { Modal, Button } from 'react-bootstrap';

interface ConfirmDialogProps {
  show: boolean;
  title?: string;
  body?: ReactNode;
  confirmLabel?: string;
  variant?: string;
  onConfirm: () => void;
  onCancel: () => void;
  busy?: boolean;
}

export default function ConfirmDialog({
  show, title = 'Please confirm', body, confirmLabel = 'Confirm',
  variant = 'primary', onConfirm, onCancel, busy,
}: ConfirmDialogProps) {
  return (
    <Modal show={show} onHide={onCancel} centered>
      <Modal.Header closeButton><Modal.Title className="h6 mb-0">{title}</Modal.Title></Modal.Header>
      <Modal.Body>{body}</Modal.Body>
      <Modal.Footer>
        <Button variant="light" onClick={onCancel} disabled={busy}>Cancel</Button>
        <Button variant={variant} onClick={onConfirm} disabled={busy}>{busy ? 'Working…' : confirmLabel}</Button>
      </Modal.Footer>
    </Modal>
  );
}
