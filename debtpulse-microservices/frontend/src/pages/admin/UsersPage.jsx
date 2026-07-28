import { useState, useCallback } from 'react';
import { Button, Row, Col, Dropdown } from 'react-bootstrap';
import { userApi } from '../../api/services.js';
import { usePaged } from '../../hooks/usePaged.js';
import { PageHeader, ErrorNote, StatusBadge } from '../../components/ui.jsx';
import DataTable from '../../components/DataTable.jsx';
import FormModal from '../../components/FormModal.jsx';
import ConfirmDialog from '../../components/ConfirmDialog.jsx';
import Field from '../../components/Field.jsx';
import { useToast } from '../../components/ToastHost.jsx';
import { ENUMS } from '../../utils/enums.js';
import { ROLE_LABELS } from '../../auth/roles.js';
import { dateTime, initials } from '../../utils/format.js';

export default function UsersPage() {
  const toast = useToast();
  const fetcher = useCallback((p) => userApi.list(p), []);
  const { page, loading, error, setPage, reload } = usePaged(fetcher);
  const [create, setCreate] = useState(false);
  const [edit, setEdit] = useState(null);
  const [remove, setRemove] = useState(null);
  const [busy, setBusy] = useState(false);

  const setStatus = async (u, status) => {
    try { await userApi.setStatus(u.userId, status); toast.success(`Status set to ${status}`); reload(); }
    catch { toast.error('Could not update status'); }
  };
  const doDelete = async () => {
    setBusy(true);
    try { await userApi.remove(remove.userId); toast.success('User deactivated'); setRemove(null); reload(); }
    catch { toast.error('Could not deactivate'); } finally { setBusy(false); }
  };

  const columns = [
    { key: 'user', header: 'User', render: (r) => (
      <div className="d-flex align-items-center gap-2">
        <span className="avatar">{initials(r.fullName)}</span>
        <div><div className="fw-semibold">{r.fullName}</div><small className="text-muted">{r.email}</small></div>
      </div>) },
    { key: 'userId', header: 'ID', render: (r) => <span className="text-mono">{r.userId}</span> },
    { key: 'role', header: 'Role', render: (r) => ROLE_LABELS[r.role] || r.role },
    { key: 'phone', header: 'Phone', render: (r) => r.phone || '—' },
    { key: 'branchId', header: 'Branch', render: (r) => r.branchId || '—' },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge value={r.status} /> },
    { key: 'createdAt', header: 'Created', render: (r) => dateTime(r.createdAt) },
    { key: '_a', header: '', render: (r) => (
      <Dropdown align="end"><Dropdown.Toggle size="sm" variant="light">Manage</Dropdown.Toggle>
        <Dropdown.Menu>
          <Dropdown.Item onClick={() => setEdit(r)}>Edit</Dropdown.Item>
          <Dropdown.Divider />
          {ENUMS.UserStatus.map((s) => <Dropdown.Item key={s} onClick={() => setStatus(r, s)}>Set {s}</Dropdown.Item>)}
          <Dropdown.Divider />
          <Dropdown.Item className="text-danger" onClick={() => setRemove(r)}>Deactivate</Dropdown.Item>
        </Dropdown.Menu>
      </Dropdown>
    ) },
  ];

  return (
    <>
      <PageHeader title="User Management" subtitle="Register collections staff and manage role-based access" icon="people"
        actions={<Button onClick={() => setCreate(true)}><i className="bi bi-person-plus me-1" />New user</Button>} />
      <ErrorNote error={error} />
      <DataTable columns={columns} page={page} loading={loading} onPageChange={setPage} emptyIcon="people" emptyTitle="No users" />

      <FormModal show={create} title="Register User" submitLabel="Create user"
        initial={{ fullName: '', email: '', password: '', role: '', phone: '', branchId: '' }}
        onClose={() => setCreate(false)} onSaved={reload} onSubmit={(v) => userApi.create(v)}>
        {(v, set, errs) => (
          <Row>
            <Col md={6}><Field label="Full Name" name="fullName" value={v.fullName} onChange={set} error={errs.fullName} required /></Col>
            <Col md={6}><Field label="Email" name="email" type="email" value={v.email} onChange={set} error={errs.email} help="Must be a @dp.com address" required /></Col>
            <Col md={6}><Field label="Password" name="password" type="password" value={v.password} onChange={set} error={errs.password} help="Min 8, upper/lower/digit/special" required /></Col>
            <Col md={6}><Field label="Role" name="role" type="select" options={ENUMS.Role} value={v.role} onChange={set} error={errs.role} required /></Col>
            <Col md={6}><Field label="Phone" name="phone" value={v.phone} onChange={set} error={errs.phone} help="10 digits" required /></Col>
            <Col md={6}><Field label="Branch ID" name="branchId" value={v.branchId} onChange={set} error={errs.branchId} /></Col>
          </Row>
        )}
      </FormModal>

      <FormModal show={!!edit} title="Edit User" submitLabel="Save changes"
        initial={edit ? { fullName: edit.fullName, email: edit.email, phone: edit.phone || '', role: edit.role, branchId: edit.branchId || '' } : {}}
        onClose={() => setEdit(null)} onSaved={reload} onSubmit={(v) => userApi.update(edit.userId, v)}>
        {(v, set, errs) => (
          <Row>
            <Col md={6}><Field label="Full Name" name="fullName" value={v.fullName} onChange={set} error={errs.fullName} /></Col>
            <Col md={6}><Field label="Email" name="email" type="email" value={v.email} onChange={set} error={errs.email} /></Col>
            <Col md={6}><Field label="Phone" name="phone" value={v.phone} onChange={set} error={errs.phone} /></Col>
            <Col md={6}><Field label="Role" name="role" type="select" options={ENUMS.Role} value={v.role} onChange={set} error={errs.role} /></Col>
            <Col md={6}><Field label="Branch ID" name="branchId" value={v.branchId} onChange={set} error={errs.branchId} /></Col>
          </Row>
        )}
      </FormModal>

      <ConfirmDialog show={!!remove} title="Deactivate user" variant="danger" confirmLabel="Deactivate" busy={busy}
        body={<>Deactivate <strong>{remove?.fullName}</strong>? They will be marked INACTIVE and unable to sign in.</>}
        onCancel={() => setRemove(null)} onConfirm={doDelete} />
    </>
  );
}
