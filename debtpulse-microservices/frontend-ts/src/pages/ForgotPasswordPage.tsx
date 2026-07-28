import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button, Form, Alert } from 'react-bootstrap';
import { authApi } from '../api/services';
import { toAppError } from '../api/client';
import Field from '../components/Field';

export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [errors, setErrors] = useState<Record<string, string | undefined>>({});
  const [msg, setMsg] = useState('');
  const [token, setToken] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true); setErrors({}); setMsg('');
    try {
      const { data } = await authApi.forgotPassword(email.trim());
      setMsg(data.message || 'If the account exists, a reset link has been sent.');
      if (data.token) setToken(data.token);
    } catch (err) {
      const app = toAppError(err);
      if (Object.keys(app.fieldErrors).length) setErrors(app.fieldErrors);
      else setMsg(app.message);
    } finally { setBusy(false); }
  };

  return (
    <div className="auth-shell">
      <div className="auth-brand">
        <div className="logo"><i className="bi bi-graph-down-arrow me-2" />DebtPulse</div>
        <p className="tag mt-2">Reset your account password securely.</p>
      </div>
      <div className="auth-panel">
        <div className="auth-card">
          <h4 className="fw-bold mb-1" style={{ color: 'var(--dp-navy)' }}>Forgot password</h4>
          <p className="text-muted mb-4">Enter your corporate email to receive a reset token.</p>
          {msg && <Alert variant="info" className="py-2">{msg}
            {token && <div className="mt-2 small">Reset token: <code>{token}</code>{' '}
              <Link to={`/reset-password?token=${encodeURIComponent(token)}`}>Reset now →</Link></div>}
          </Alert>}
          <Form onSubmit={submit} noValidate>
            <Field label="Email" name="email" type="email" value={email}
                   onChange={(_, v) => setEmail(v)} error={errors.email} placeholder="you@dp.com" required autoFocus />
            <Button type="submit" className="w-100" disabled={busy}>{busy ? 'Sending…' : 'Send reset token'}</Button>
          </Form>
          <div className="text-center mt-3">
            <Button variant="link" size="sm" onClick={() => navigate('/login')}>Back to sign in</Button>
          </div>
        </div>
      </div>
    </div>
  );
}
