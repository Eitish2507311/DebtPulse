import { useState, useCallback } from 'react';
import { Button, Row, Col, Form, InputGroup } from 'react-bootstrap';
import { allocationApi } from '../../api/services.js';
import { usePaged } from '../../hooks/usePaged.js';
import { PageHeader, ErrorNote, StatusBadge } from '../../components/ui.jsx';
import DataTable from '../../components/DataTable.jsx';
import FormModal from '../../components/FormModal.jsx';
import ConfirmDialog from '../../components/ConfirmDialog.jsx';
import Field from '../../components/Field.jsx';
import { useToast } from '../../components/ToastHost.jsx';
import { ENUMS } from '../../utils/enums.js';
import { titleCase, num } from '../../utils/format.js';
import { ROLES } from '../../auth/roles.js';
import { useAuth } from '../../auth/AuthContext.jsx';

export default function AllocationsPage() {
  const toast = useToast();
  const { role } = useAuth();
  const canWrite = [ROLES.ADMIN, ROLES.PORTFOLIO_MANAGER, ROLES.COLLECTIONS_AGENT].includes(role);
  const fetcher = useCallback((p) => allocationApi.list(p), []);
  const { page, loading, error, setPage, reload } = usePaged(fetcher);
  const [editing, setEditing] = useState(null);
  const [remove, setRemove] = useState(null);
  const [busy, setBusy] = useState(false);
  const [q, setQ] = useState('');

  // Client-side search over the loaded rules (id / name / role / strategy / bucket).
  const shown = page ? { ...page, content: (page.content || []).filter((r) => {
    if (!q.trim()) return true;
    const s = q.toLowerCase();
    return [r.ruleId, r.name, r.targetRole, r.strategy, r.bucket]
      .some((x) => (x || '').toString().toLowerCase().includes(s));
  }) } : page;

  const runExecute = async () => {
    try { const { data } = await allocationApi.execute(); toast.success(`Allocated ${data.allocated ?? 0} account(s)`); }
    catch { toast.error('Allocation run failed'); }
  };

  const doDelete = async () => {
    setBusy(true);
    try { await allocationApi.remove(remove.ruleId); toast.success('Rule deleted'); setRemove(null); reload(); }
    catch { toast.error('Could not delete rule'); } finally { setBusy(false); }
  };

  const columns = [
    { key: 'name', header: 'Rule', render: (r) => <span className="fw-semibold">{r.name}</span> },
    { key: 'strategy', header: 'Strategy', render: (r) => titleCase(r.strategy) },
    { key: 'bucket', header: 'Bucket', render: (r) => r.bucket || 'Any' },
    { key: 'targetRole', header: 'Target Role', render: (r) => titleCase(r.targetRole) },
    { key: 'autoEscalate', header: 'Kind', render: (r) => (r.autoEscalate ? 'Escalation' : 'Allocation') },
    { key: 'priority', header: 'Priority', className: 'text-center', render: (r) => num(r.priority) },
    { key: 'active', header: 'State', render: (r) => <StatusBadge value={r.active ? 'ACTIVE' : 'INACTIVE'} /> },
    { key: '_a', header: '', render: (r) => canWrite && (
      <div className="d-flex gap-1 justify-content-end">
        <Button size="sm" variant="light" onClick={() => setEditing(r)}><i className="bi bi-pencil" /></Button>
        <Button size="sm" variant="light" className="text-danger" onClick={() => setRemove(r)}><i className="bi bi-trash" /></Button>
      </div>
    ) },
  ];

  const empty = { name: '', strategy: '', bucket: '', targetRole: '', daysInBucketThreshold: '', minDpd: '', gracePeriodDays: '', capacityLimit: '', branchId: '', priority: '', autoEscalate: false, active: true };
  const editInitial = editing && editing.ruleId
    ? { ...editing, active: editing.active ?? true } : empty;

  return (
    <>
      <PageHeader title="Allocation Rules" subtitle="Configure how delinquent accounts are distributed to agents" icon="diagram-3"
        actions={canWrite && <>
          <Button variant="outline-primary" onClick={runExecute}><i className="bi bi-play-circle me-1" />Run allocation</Button>
          <Button onClick={() => setEditing({})}><i className="bi bi-plus-lg me-1" />New rule</Button>
        </>} />
      <ErrorNote error={error} />
      <Row className="g-2 mb-3"><Col sm={6} md={4}>
        <InputGroup size="sm">
          <InputGroup.Text><i className="bi bi-search" /></InputGroup.Text>
          <Form.Control placeholder="Search rules by id, name, role, strategy…" value={q}
            onChange={(e) => setQ(e.target.value)} />
          {q && <Button variant="outline-secondary" onClick={() => setQ('')}><i className="bi bi-x" /></Button>}
        </InputGroup>
      </Col></Row>
      <DataTable columns={columns} page={shown} loading={loading} onPageChange={setPage}
        emptyIcon="diagram-3" emptyTitle="No allocation rules" />

      <FormModal show={!!editing} title={editing?.ruleId ? 'Edit Allocation Rule' : 'New Allocation Rule'}
        submitLabel={editing?.ruleId ? 'Save changes' : 'Create rule'}
        initial={editInitial} onClose={() => setEditing(null)} onSaved={reload}
        onSubmit={(v) => {
          const numOrNull = (x) => (x === '' || x == null ? null : Number(x));
          const body = { ...v,
            daysInBucketThreshold: numOrNull(v.daysInBucketThreshold),
            minDpd: numOrNull(v.minDpd),
            gracePeriodDays: numOrNull(v.gracePeriodDays),
            capacityLimit: numOrNull(v.capacityLimit),
            priority: numOrNull(v.priority),
            autoEscalate: v.autoEscalate === true || v.autoEscalate === 'true',
            active: v.active === true || v.active === 'true' };
          return editing?.ruleId ? allocationApi.update(editing.ruleId, body) : allocationApi.create(body);
        }}>
        {(v, set, errs) => (
          <Row>
            <Col md={6}><Field label="Rule Name" name="name" value={v.name} onChange={set} error={errs.name} required /></Col>
            <Col md={6}><Field label="Strategy" name="strategy" type="select" options={ENUMS.AllocationStrategy} value={v.strategy} onChange={set} error={errs.strategy} required /></Col>
            <Col md={6}><Field label="Bucket" name="bucket" type="select" options={ENUMS.DpdBucket} value={v.bucket} onChange={set} error={errs.bucket} /></Col>
            <Col md={6}><Field label="Target Role" name="targetRole" type="select" options={ENUMS.Role} value={v.targetRole} onChange={set} error={errs.targetRole} required /></Col>
            <Col md={4}><Field label="Days in Bucket" name="daysInBucketThreshold" type="number" min="0" value={v.daysInBucketThreshold} onChange={set} error={errs.daysInBucketThreshold} help="Stagnation threshold" /></Col>
            <Col md={4}><Field label="Min DPD" name="minDpd" type="number" min="0" value={v.minDpd} onChange={set} error={errs.minDpd} /></Col>
            <Col md={4}><Field label="Grace Period (DPD)" name="gracePeriodDays" type="number" min="0" value={v.gracePeriodDays} onChange={set} error={errs.gracePeriodDays} /></Col>
            <Col md={4}><Field label="Capacity Limit" name="capacityLimit" type="number" min="0" value={v.capacityLimit} onChange={set} error={errs.capacityLimit} help="Max accounts / user" /></Col>
            <Col md={4}><Field label="Branch ID" name="branchId" value={v.branchId} onChange={set} error={errs.branchId} /></Col>
            <Col md={4}><Field label="Priority" name="priority" type="number" value={v.priority} onChange={set} error={errs.priority} help="Higher wins" /></Col>
            <Col md={6}><Field label="Rule Kind" name="autoEscalate" type="select" options={[{ value: 'false', label: 'Allocation (assign to agent)' }, { value: 'true', label: 'Escalation (move to higher role)' }]} value={String(v.autoEscalate)} onChange={set} help="Allocation runs on import/execute; escalation runs on the escalation job" /></Col>
            <Col md={6}><Field label="Active" name="active" type="select" options={[{ value: 'true', label: 'Active' }, { value: 'false', label: 'Inactive' }]} value={String(v.active)} onChange={set} /></Col>
          </Row>
        )}
      </FormModal>

      <ConfirmDialog show={!!remove} title="Delete allocation rule" variant="danger" confirmLabel="Delete" busy={busy}
        body={<>Delete rule <strong>{remove?.name}</strong>? This cannot be undone.</>}
        onCancel={() => setRemove(null)} onConfirm={doDelete} />
    </>
  );
}
