import { useState, useCallback } from 'react';
import { Tabs, Tab, Button, Row, Col, Dropdown } from 'react-bootstrap';
import { settlementApi, restructuringApi } from '../../api/services';
import { usePaged } from '../../hooks/usePaged';
import { useAuth } from '../../auth/AuthContext';
import { APPROVERS } from '../../auth/roles';
import { PageHeader, ErrorNote, StatusBadge } from '../../components/ui';
import DataTable from '../../components/DataTable';
import FormModal from '../../components/FormModal';
import ConfirmDialog from '../../components/ConfirmDialog';
import Field from '../../components/Field';
import { useToast } from '../../components/ToastHost';
import { ENUMS } from '../../utils/enums';
import { inr, pct, date, today } from '../../utils/format';
import type { Settlement, Restructuring, Column, Role } from '../../types';

export default function SettlementWorkspace() {
  const { role } = useAuth();
  return (
    <>
      <PageHeader title="Settlement & Restructuring" subtitle="Evaluate one-time settlements and restructuring proposals through the approval chain" icon="cash-coin" />
      <Tabs defaultActiveKey="settlements" className="mb-3">
        <Tab eventKey="settlements" title="Settlements"><SettlementsTab role={role!} /></Tab>
        <Tab eventKey="restructuring" title="Restructuring"><RestructuringTab role={role!} /></Tab>
      </Tabs>
    </>
  );
}

function SettlementsTab({ role }: { role: Role }) {
  const toast = useToast();
  const isOfficer = role === 'ADMIN' || role === 'SETTLEMENT_OFFICER';
  const isApprover = role === 'ADMIN' || APPROVERS.includes(role);
  const fetcher = useCallback((p: Record<string, unknown>) => settlementApi.list(p), []);
  const { page, loading, error, setPage, reload } = usePaged<Settlement>(fetcher);
  const [show, setShow] = useState(false);
  const [decide, setDecide] = useState<Settlement | null>(null);
  const [paid, setPaid] = useState<Settlement | null>(null);

  const submit = async (r: Settlement) => {
    try { await settlementApi.submit(r.proposalId); toast.success('Submitted for approval'); reload(); }
    catch { toast.error('Could not submit'); }
  };
  const doPaid = async () => {
    if (!paid) return;
    try { await settlementApi.markPaid(paid.proposalId); toast.success('Marked as paid'); setPaid(null); reload(); }
    catch { toast.error('Could not update'); }
  };

  const columns: Column<Settlement>[] = [
    { key: 'proposalId', header: 'Proposal', render: (r) => <span className="text-mono">{r.proposalId}</span> },
    { key: 'accountId', header: 'Account', render: (r) => <span className="text-mono">{r.accountId}</span> },
    { key: 'totalOutstanding', header: 'Outstanding', className: 'text-end', render: (r) => inr(r.totalOutstanding) },
    { key: 'settlementAmount', header: 'Offer', className: 'text-end', render: (r) => inr(r.settlementAmount) },
    { key: 'haircutPercent', header: 'Haircut', className: 'text-center', render: (r) => pct(r.haircutPercent) },
    { key: 'approvalLevel', header: 'Chain / Step', className: 'text-center', render: (r) => (
      <span>{(r.requiredApprovalChain || []).join('→') || r.approvalLevel}{r.currentStep ? ` (@${r.currentStep})` : ''}</span>
    ) },
    { key: 'paymentDeadline', header: 'Deadline', render: (r) => date(r.paymentDeadline) },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge value={r.status} /> },
    { key: '_a', header: '', render: (r) => (
      <Dropdown align="end"><Dropdown.Toggle size="sm" variant="light">Actions</Dropdown.Toggle>
        <Dropdown.Menu>
          {isOfficer && r.status === 'DRAFT' && <Dropdown.Item onClick={() => submit(r)}>Submit for approval</Dropdown.Item>}
          {isApprover && r.status === 'PENDING_APPROVAL' && <Dropdown.Item onClick={() => setDecide(r)}>Record decision</Dropdown.Item>}
          {(isOfficer || isApprover) && r.status === 'APPROVED' && <Dropdown.Item onClick={() => setPaid(r)}>Mark paid</Dropdown.Item>}
        </Dropdown.Menu>
      </Dropdown>
    ) },
  ];
  return (<>
    <div className="d-flex justify-content-end mb-2">
      {isOfficer && <Button size="sm" onClick={() => setShow(true)}><i className="bi bi-plus-lg me-1" />New settlement</Button>}
    </div>
    <ErrorNote error={error} />
    <DataTable<Settlement> columns={columns} page={page} loading={loading} onPageChange={setPage} emptyIcon="cash-coin" emptyTitle="No settlement proposals" />

    <FormModal show={show} title="New Settlement Proposal" submitLabel="Create"
      initial={{ accountId: '', totalOutstanding: '', settlementAmount: '', paymentDeadline: '', notes: '' }}
      onClose={() => setShow(false)} onSaved={reload}
      onSubmit={(v) => settlementApi.create({ ...v, totalOutstanding: Number(v.totalOutstanding), settlementAmount: Number(v.settlementAmount) })}>
      {(v, set, errs) => (<>
        <Field label="Account ID" name="accountId" value={v.accountId} onChange={set} error={errs.accountId} required />
        <Row>
          <Col md={6}><Field label="Total Outstanding" name="totalOutstanding" type="number" min="0" value={v.totalOutstanding} onChange={set} error={errs.totalOutstanding} required /></Col>
          <Col md={6}><Field label="Settlement Amount" name="settlementAmount" type="number" min="0" value={v.settlementAmount} onChange={set} error={errs.settlementAmount} help="Haircut & approval chain derived automatically" required /></Col>
          <Col md={12}><Field label="Remarks" name="notes" type="textarea" value={v.notes} onChange={set} error={errs.notes} help="The required approvers (L1→L2→L3) are set by the haircut — you don't choose the level." /></Col>
        </Row>
      </>)}
    </FormModal>

    <FormModal show={!!decide} title={`Record Decision — ${decide?.currentStep || ''} approval`} submitLabel="Submit decision"
      initial={{ level: decide?.currentStep || 'L1', decision: '', comments: '' }}
      onClose={() => setDecide(null)} onSaved={reload}
      onSubmit={(v) => settlementApi.decide(decide!.proposalId, v.level, { decision: v.decision, comments: v.comments })}>
      {(v, set, errs) => (<>
        <p className="text-muted small mb-2">
          Proposal <span className="text-mono">{decide?.proposalId}</span> is awaiting{' '}
          <strong>{decide?.currentStep}</strong> approval (haircut {decide?.haircutPercent}%).
          Pick the level you are approving as — it must match the pending step.
        </p>
        <Row>
          <Col md={6}><Field label="Approving as (level)" name="level" type="select" options={ENUMS.ApprovalLevel} value={v.level} onChange={set} error={errs.level} required /></Col>
          <Col md={6}><Field label="Decision" name="decision" type="select" options={ENUMS.ApprovalDecision} value={v.decision} onChange={set} error={errs.decision} required /></Col>
        </Row>
        <Field label="Comments" name="comments" type="textarea" value={v.comments} onChange={set} error={errs.comments} />
      </>)}
    </FormModal>

    <ConfirmDialog show={!!paid} title="Confirm settlement payment" confirmLabel="Mark paid"
      body={<>Confirm that settlement <strong>{paid?.proposalId}</strong> has been paid in full?</>}
      onCancel={() => setPaid(null)} onConfirm={doPaid} />
  </>);
}

function RestructuringTab({ role }: { role: Role }) {
  const toast = useToast();
  const isOfficer = role === 'ADMIN' || role === 'SETTLEMENT_OFFICER';
  const isApprover = role === 'ADMIN' || APPROVERS.includes(role);
  const fetcher = useCallback((p: Record<string, unknown>) => restructuringApi.list(p), []);
  const { page, loading, error, setPage, reload } = usePaged<Restructuring>(fetcher);
  const [show, setShow] = useState(false);

  const act = async (r: Restructuring, kind: 'approve' | 'reject') => {
    try { await (kind === 'approve' ? restructuringApi.approve(r.restructureId) : restructuringApi.reject(r.restructureId));
      toast.success(`Proposal ${kind}d`); reload(); }
    catch { toast.error(`Could not ${kind}`); }
  };

  const columns: Column<Restructuring>[] = [
    { key: 'restructureId', header: 'Proposal', render: (r) => <span className="text-mono">{r.restructureId}</span> },
    { key: 'accountId', header: 'Account', render: (r) => <span className="text-mono">{r.accountId}</span> },
    { key: 'revisedTenure', header: 'Tenure (m)', className: 'text-center', render: (r) => r.revisedTenure },
    { key: 'revisedEmi', header: 'Revised EMI', className: 'text-end', render: (r) => inr(r.revisedEmi) },
    { key: 'waiverAmount', header: 'Waiver', className: 'text-end', render: (r) => inr(r.waiverAmount) },
    { key: 'startDate', header: 'Start', render: (r) => date(r.startDate) },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge value={r.status} /> },
    { key: '_a', header: '', render: (r) => (isApprover && r.status === 'PENDING_APPROVAL') ? (
      <div className="d-flex gap-1 justify-content-end">
        <Button size="sm" variant="outline-success" onClick={() => act(r, 'approve')}>Approve</Button>
        <Button size="sm" variant="outline-danger" onClick={() => act(r, 'reject')}>Reject</Button>
      </div>
    ) : null },
  ];
  return (<>
    <div className="d-flex justify-content-end mb-2">
      {isOfficer && <Button size="sm" onClick={() => setShow(true)}><i className="bi bi-plus-lg me-1" />New restructuring</Button>}
    </div>
    <ErrorNote error={error} />
    <DataTable<Restructuring> columns={columns} page={page} loading={loading} onPageChange={setPage} emptyIcon="arrow-repeat" emptyTitle="No restructuring proposals" />
    <FormModal show={show} title="New Restructuring Proposal" submitLabel="Create"
      initial={{ accountId: '', revisedTenure: '', revisedEmi: '', waiverAmount: '', startDate: today() }}
      onClose={() => setShow(false)} onSaved={reload}
      onSubmit={(v) => restructuringApi.create({ ...v, revisedTenure: Number(v.revisedTenure), revisedEmi: Number(v.revisedEmi), waiverAmount: Number(v.waiverAmount) })}>
      {(v, set, errs) => (<>
        <Field label="Account ID" name="accountId" value={v.accountId} onChange={set} error={errs.accountId} required />
        <Row>
          <Col md={4}><Field label="Revised Tenure (months)" name="revisedTenure" type="number" min="1" value={v.revisedTenure} onChange={set} error={errs.revisedTenure} required /></Col>
          <Col md={4}><Field label="Revised EMI" name="revisedEmi" type="number" min="0" value={v.revisedEmi} onChange={set} error={errs.revisedEmi} required /></Col>
          <Col md={4}><Field label="Waiver Amount" name="waiverAmount" type="number" min="0" value={v.waiverAmount} onChange={set} error={errs.waiverAmount} required /></Col>
        </Row>
        <Field label="Start Date" name="startDate" type="date" value={v.startDate} onChange={set} error={errs.startDate} required />
      </>)}
    </FormModal>
  </>);
}
