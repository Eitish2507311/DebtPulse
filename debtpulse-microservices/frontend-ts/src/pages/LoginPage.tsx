import { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { Button, Form, Alert } from 'react-bootstrap';
import { useAuth } from '../auth/AuthContext';
import Field from '../components/Field';

interface LocationState { from?: { pathname?: string }; }

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [values, setValues] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState<Record<string, string | undefined>>({});
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);

  const setField = (name: string, value: string) => {
    setValues((v) => ({ ...v, [name]: value }));
    setErrors((e) => ({ ...e, [name]: undefined }));
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true); setFormError(''); setErrors({});
    const res = await login(values.email.trim(), values.password);
    setBusy(false);
    if (res.ok) {
      const from = (location.state as LocationState | null)?.from?.pathname;
      navigate(from || '/dashboard', { replace: true });
    } else if (res.error && Object.keys(res.error.fieldErrors || {}).length) {
      setErrors(res.error.fieldErrors);
    } else {
      setFormError(res.error?.message || 'Sign in failed');
    }
  };

  return (
    <div className="auth-shell">
      <div className="auth-brand">
        <div className="logo" role="button" tabIndex={0} title="Back to home"
             onClick={() => navigate('/')}
             onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') navigate('/'); }}>
          <i className="bi bi-graph-down-arrow me-2" />DebtPulse
        </div>
        <p className="tag mt-2">Debt Recovery &amp; Collections Management. Track delinquent portfolios,
          run field recovery, negotiate settlements and manage legal proceedings — end to end.</p>
        <div className="mt-4">
          <div className="feature"><i className="bi bi-shield-lock" /><span>Bank-grade RBAC across nine collections roles</span></div>
          <div className="feature"><i className="bi bi-diagram-3" /><span>Portfolio, contact, field, settlement &amp; legal workflows</span></div>
          <div className="feature"><i className="bi bi-graph-up" /><span>Real-time recovery analytics &amp; bucket ageing</span></div>
        </div>
      </div>

      <div className="auth-panel">
        <div className="auth-card">
          <h4 className="fw-bold mb-1" style={{ color: 'var(--dp-navy)' }}>Sign in</h4>
          <p className="text-muted mb-4">Use your DebtPulse corporate credentials.</p>
          {formError && <Alert variant="danger" className="py-2">{formError}</Alert>}
          <Form onSubmit={submit} noValidate>
            <Field label="Email" name="email" type="email" value={values.email} onChange={setField}
                   error={errors.email} placeholder="you@dp.com" autoFocus required />
            <Field label="Password" name="password" type="password" value={values.password} onChange={setField}
                   error={errors.password} placeholder="••••••••" required />
            <div className="d-flex justify-content-end mb-3">
              <Link to="/forgot-password" className="small">Forgot password?</Link>
            </div>
            <Button type="submit" className="w-100" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</Button>
          </Form>
          <p className="text-center text-muted small mt-4 mb-0">Demo: <code>admin@dp.com</code> / <code>password</code></p>
        </div>
      </div>
    </div>
  );
}
