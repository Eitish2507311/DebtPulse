import { useState } from 'react';
import { Button, Form, Card } from 'react-bootstrap';
import { authApi } from '../api/services.js';
import { toAppError } from '../api/client.js';
import Field from '../components/Field.jsx';
import { PageHeader } from '../components/ui.jsx';
import { useToast } from '../components/ToastHost.jsx';

export default function ChangePasswordPage() {
  const toast = useToast();
  const [values, setValues] = useState({ currentPassword: '', newPassword: '', confirm: '' });
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const setField = (name, value) => { setValues((v) => ({ ...v, [name]: value })); setErrors((e) => ({ ...e, [name]: undefined })); };

  const submit = async (e) => {
    e.preventDefault();
    if (values.newPassword !== values.confirm) { setErrors({ confirm: 'Passwords do not match' }); return; }
    setBusy(true); setErrors({});
    try {
      await authApi.changePassword(values.currentPassword, values.newPassword);
      toast.success('Password changed successfully');
      setValues({ currentPassword: '', newPassword: '', confirm: '' });
    } catch (err) {
      const app = toAppError(err);
      if (Object.keys(app.fieldErrors).length) setErrors(app.fieldErrors);
      else toast.error(app.message, 'Could not change password');
    } finally { setBusy(false); }
  };

  return (
    <>
      <PageHeader title="Change Password" subtitle="Update the password for your account" icon="key" />
      <Card style={{ maxWidth: 480 }}>
        <Card.Body>
          <Form onSubmit={submit} noValidate>
            <Field label="Current password" name="currentPassword" type="password"
                   value={values.currentPassword} onChange={setField} error={errors.currentPassword} required />
            <Field label="New password" name="newPassword" type="password"
                   value={values.newPassword} onChange={setField} error={errors.newPassword}
                   help="Min 8 chars incl. upper, lower, digit & special." required />
            <Field label="Confirm new password" name="confirm" type="password"
                   value={values.confirm} onChange={setField} error={errors.confirm} required />
            <Button type="submit" disabled={busy}>{busy ? 'Updating…' : 'Update password'}</Button>
          </Form>
        </Card.Body>
      </Card>
    </>
  );
}
