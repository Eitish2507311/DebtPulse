import { useState, useCallback } from 'react';
import { Tabs, Tab, Button, Row, Col, Dropdown, Modal, Table } from 'react-bootstrap';
import { settlementApi, restructuringApi } from '../../api/services.js';
import { usePaged } from '../../hooks/usePaged.js';
import { useAuth } from '../../auth/AuthContext.jsx';
import { ROLES, APPROVERS } from '../../auth/roles.js';
import { PageHeader, ErrorNote, StatusBadge } from '../../components/ui.jsx';
import DataTable from '../../components/DataTable.jsx';
import FormModal from '../../components/FormModal.jsx';
import ConfirmDialog from '../../components/ConfirmDialog.jsx';
import Field from '../../components/Field.jsx';
import { useToast } from '../../components/ToastHost.jsx';
import { ENUMS } from '../../utils/enums.js';
import { inr, pct, date, titleCase, today } from '../../utils/format.js';

export default function SettlementWorkspace() {
  const { role } = useAuth();
  return (
    <>
      <PageHeader title="Settlement & Restructuring" subtitle="Evaluate one-time settlements and restructuring proposals through the approval chain" icon="cash-coin" />
      <Tabs defaultActiveKey="settlements" className="mb-3">
        <Tab eventKey="settlements" title="Settlements"><SettlementsTab role={role} /></Tab>
        <Tab eventKey="restructuring" title="Restructuring"><RestructuringTab role={role} /></Tab>
      </Tabs>
    </>
  );
}

function SettlementsTab({ role }) {
  const toast = useToast();
  const isOfficer = [ROLES.ADMIN, ROLES.SETTLEMENT_OFFICER].includes(role);
  const isApprover = [ROLES.ADMIN, ...APPROVERS].includes(role);
  const fetcher = useCallback((p) => settlementApi.list(p), []);
  const { page, loading, error, setPage, reload } = usePaged(fetcher);
  const [show, setShow] = useState(false);
  const [decide, setDecide] = useState(null);
  const [paid, setPaid] = useState(null);
  const [view, setView] = useState(null);

  const submit = async (r) => {
    try { await settlementApi.submit(r.proposalId); toast.success('Submitted for approval'); reload(); }
    catch { toast.error('Could not submit'); }
  };
  const doPaid = async () => {
    try { await settlementApi.markPaid(paid.proposalId); toast.success('Marked as paid'); setPaid(null); reload(); }
    catch { toast.error('Could not update'); }
  };

  const columns = [
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
        <Dropdown.Menu renderOnMount popperConfig={{ strategy: 'fixed' }}>
          <Dropdown.Item onClick={() => setView(r)}>View</Dropdown.Item>
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
    <DataTable columns={columns} page={page} loading={loading} onPageChange={setPage} emptyIcon="cash-coin" emptyTitle="No settlement proposals" />

    <FormModal show={show} title="New Settlement Proposal" submitLabel="Create"
      initial={{ accountId: '', totalOutstanding: '', settlementAmount: '', paymentDeadline: '', notes: '' }}
      onClose={() => setShow(false)} onSaved={reload}
      onSubmit={(v) => settlementApi.create({ ...v, totalOutstanding: Number(v.totalOutstanding), settlementAmount: Number(v.settlementAmount) })}>
      {(v, set, errs) => (<>
        <Field label="Account ID" name="accountId" value={v.accountId} onChange={set} error={errs.accountId} required />
        <Row>
          <Col md={6}><Field label="Total Outstanding" name="totalOutstanding" type="number" min="0" value={v.totalOutstanding} onChange={set} error={errs.totalOutstanding} required /></Col>
          <Col md={6}><Field label="Settlement Amount" name="settlementAmount" type="number" min="0" value={v.settlementAmount} onChange={set} error={errs.settlementAmount} help="Haircut & approval chain derived automatically" required /></Col>
          <Col md={6}><Field label="Payment Deadline" name="paymentDeadline" type="date" value={v.paymentDeadline} onChange={set} error={errs.paymentDeadline} help="Date by which the settlement must be paid" required /></Col>
          <Col md={12}><Field label="Remarks" name="notes" type="textarea" value={v.notes} onChange={set} error={errs.notes} help="The required approvers (L1→L2→L3) are set by the haircut — you don't choose the level." /></Col>
        </Row>
      </>)}
    </FormModal>

    <FormModal show={!!decide} title={`Record Decision — ${decide?.currentStep || ''} approval`} submitLabel="Submit decision"
      initial={{ level: decide?.currentStep || 'L1', decision: '', comments: '' }}
      onClose={() => setDecide(null)} onSaved={reload}
      onSubmit={(v) => settlementApi.decide(decide.proposalId, v.level, { decision: v.decision, comments: v.comments })}>
      {(v, set, errs) => (<>
        <p className="text-muted small mb-2">
          Proposal <span className="text-mono">{decide?.proposalId}</span> is awaiting
          {' '}<strong>{decide?.currentStep}</strong> approval (haircut {decide?.haircutPercent}%).
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

    <Modal show={!!view} onHide={() => setView(null)} centered>
      <Modal.Header closeButton><Modal.Title className="h6 mb-0">Settlement {view?.proposalId}</Modal.Title></Modal.Header>
      <Modal.Body>
        {view && (
          <Table borderless size="sm" className="mb-0">
            <tbody>
              <tr><td className="text-muted">Account</td><td className="text-end text-mono">{view.accountId}</td></tr>
              <tr><td className="text-muted">Total Outstanding</td><td className="text-end fw-semibold">{inr(view.totalOutstanding)}</td></tr>
              <tr><td className="text-muted">Settlement Offer</td><td className="text-end fw-semibold">{inr(view.settlementAmount)}</td></tr>
              <tr><td className="text-muted">Haircut</td><td className="text-end">{pct(view.haircutPercent)}</td></tr>
              <tr><td className="text-muted">Approval Chain</td><td className="text-end">{(view.requiredApprovalChain || []).join(' → ') || view.approvalLevel}</td></tr>
              <tr><td className="text-muted">Current Step</td><td className="text-end">{view.currentStep || '—'}</td></tr>
              <tr><td className="text-muted">Payment Deadline</td><td className="text-end">{date(view.paymentDeadline)}</td></tr>
              <tr><td className="text-muted">Status</td><td className="text-end"><StatusBadge value={view.status} /></td></tr>
              <tr><td className="text-muted">Remarks</td><td className="text-end">{view.notes || '—'}</td></tr>
            </tbody>
          </Table>
        )}
      </Modal.Body>
      <Modal.Footer><Button variant="light" onClick={() => setView(null)}>Close</Button></Modal.Footer>
    </Modal>
  </>);
}

function RestructuringTab({ role }) {
  const toast = useToast();
  const isOfficer = [ROLES.ADMIN, ROLES.SETTLEMENT_OFFICER].includes(role);
  const isApprover = [ROLES.ADMIN, ...APPROVERS].includes(role);
  const fetcher = useCallback((p) => restructuringApi.list(p), []);
  const { page, loading, error, setPage, reload } = usePaged(fetcher);
  const [show, setShow] = useState(false);

  const act = async (r, kind) => {
    try { await (kind === 'approve' ? restructuringApi.approve(r.restructureId) : restructuringApi.reject(r.restructureId));
      toast.success(`Proposal ${kind}d`); reload(); }
    catch { toast.error(`Could not ${kind}`); }
  };

  const columns = [
    { key: 'restructureId', header: 'Proposal', render: (r) => <span className="text-mono">{r.restructureId}</span> },
    { key: 'accountId', header: 'Account', render: (r) => <span className="text-mono">{r.accountId}</span> },
    { key: 'revisedTenure', header: 'Tenure (m)', className: 'text-center', render: (r) => r.revisedTenure },
    { key: 'revisedEmi', header: 'Revised EMI', className: 'text-end', render: (r) => inr(r.revisedEmi) },
    { key: 'waiverAmount', header: 'Waiver', className: 'text-end', render: (r) => inr(r.waiverAmount) },
    { key: 'startDate', header: 'Start', render: (r) => date(r.startDate) },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge value={r.status} /> },
    { key: '_a', header: '', render: (r) => isApprover && r.status === 'PENDING_APPROVAL' && (
      <div className="d-flex gap-1 justify-content-end">
        <Button size="sm" variant="outline-success" onClick={() => act(r, 'approve')}>Approve</Button>
        <Button size="sm" variant="outline-danger" onClick={() => act(r, 'reject')}>Reject</Button>
      </div>
    ) },
  ];
  return (<>
    <div className="d-flex justify-content-end mb-2">
      {isOfficer && <Button size="sm" onClick={() => setShow(true)}><i className="bi bi-plus-lg me-1" />New restructuring</Button>}
    </div>
    <ErrorNote error={error} />
    <DataTable columns={columns} page={page} loading={loading} onPageChange={setPage} emptyIcon="arrow-repeat" emptyTitle="No restructuring proposals" />
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
