import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Button, Form, Alert } from 'react-bootstrap';
import { authApi } from '../api/services.js';
import { toAppError } from '../api/client.js';
import Field from '../components/Field.jsx';

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const [sp] = useSearchParams();
  const [values, setValues] = useState({ token: sp.get('token') || '', newPassword: '', confirm: '' });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);
  const setField = (name, value) => { setValues((v) => ({ ...v, [name]: value })); setErrors((e) => ({ ...e, [name]: undefined })); };

  const submit = async (e) => {
    e.preventDefault();
    setFormError('');
    if (values.newPassword !== values.confirm) { setErrors({ confirm: 'Passwords do not match' }); return; }
    setBusy(true); setErrors({});
    try {
      await authApi.resetPassword(values.token.trim(), values.newPassword);
      navigate('/login', { replace: true, state: { reset: true } });
    } catch (err) {
      const app = toAppError(err);
      if (Object.keys(app.fieldErrors).length) setErrors(app.fieldErrors);
      else setFormError(app.message);
    } finally { setBusy(false); }
  };

  return (
    <div className="auth-shell">
      <div className="auth-brand">
        <div className="logo"><i className="bi bi-graph-down-arrow me-2" />DebtPulse</div>
        <p className="tag mt-2">Choose a strong new password.</p>
      </div>
      <div className="auth-panel">
        <div className="auth-card">
          <h4 className="fw-bold mb-1" style={{ color: 'var(--dp-navy)' }}>Reset password</h4>
          <p className="text-muted mb-4">Min 8 chars incl. upper, lower, digit &amp; special character.</p>
          {formError && <Alert variant="danger" className="py-2">{formError}</Alert>}
          <Form onSubmit={submit} noValidate>
            <Field label="Reset token" name="token" value={values.token} onChange={setField} error={errors.token} required />
            <Field label="New password" name="newPassword" type="password" value={values.newPassword}
                   onChange={setField} error={errors.newPassword} required />
            <Field label="Confirm password" name="confirm" type="password" value={values.confirm}
                   onChange={setField} error={errors.confirm} required />
            <Button type="submit" className="w-100" disabled={busy}>{busy ? 'Updating…' : 'Update password'}</Button>
          </Form>
          <div className="text-center mt-3">
            <Button variant="link" size="sm" onClick={() => navigate('/login')}>Back to sign in</Button>
          </div>
        </div>
      </div>
    </div>
  );
}
