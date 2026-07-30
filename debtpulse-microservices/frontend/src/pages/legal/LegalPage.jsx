import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Tabs, Tab, Button, Row, Col, Table, Form, InputGroup } from 'react-bootstrap';
import { legalApi } from '../../api/services.js';
import { usePaged, useAsync } from '../../hooks/usePaged.js';
import { useAuth } from '../../auth/AuthContext.jsx';
import { ROLES } from '../../auth/roles.js';
import { PageHeader, ErrorNote, StatusBadge, Loading, EmptyState } from '../../components/ui.jsx';
import DataTable from '../../components/DataTable.jsx';
import FormModal from '../../components/FormModal.jsx';
import ConfirmDialog from '../../components/ConfirmDialog.jsx';
import Field from '../../components/Field.jsx';
import { useToast } from '../../components/ToastHost.jsx';
import { ENUMS } from '../../utils/enums.js';
import { date, titleCase, today } from '../../utils/format.js';

/** Small reusable filter box (client-side) with a clear button. */
function SearchBox({ value, onChange, placeholder }) {
  return (
    <InputGroup size="sm" style={{ maxWidth: 320 }}>
      <InputGroup.Text><i className="bi bi-search" /></InputGroup.Text>
      <Form.Control value={value} placeholder={placeholder} onChange={(e) => onChange(e.target.value)} />
      {value && <Button variant="outline-secondary" onClick={() => onChange('')}><i className="bi bi-x" /></Button>}
    </InputGroup>
  );
}

export default function LegalPage() {
  const { role } = useAuth();
  const canWrite = [ROLES.ADMIN, ROLES.LEGAL_OFFICER].includes(role);
  return (
    <>
      <PageHeader title="Legal Proceedings" subtitle="Track court filings, hearings and recovery orders" icon="bank" />
      <Tabs defaultActiveKey="cases" className="mb-3">
        <Tab eventKey="cases" title="Cases"><CasesTab canWrite={canWrite} /></Tab>
        <Tab eventKey="hearings" title="Hearings"><HearingsTab canWrite={canWrite} /></Tab>
        <Tab eventKey="orders" title="Recovery Orders"><OrdersTab canWrite={canWrite} /></Tab>
      </Tabs>
    </>
  );
}

function CasesTab({ canWrite }) {
  const navigate = useNavigate();
  const fetcher = useCallback((p) => legalApi.listCases(p), []);
  const { page, loading, error, setPage, reload } = usePaged(fetcher);
  const [show, setShow] = useState(false);
  const [editing, setEditing] = useState(null);
  const [search, setSearch] = useState('');

  const goToCase = (id) => { const v = id.trim(); if (v) navigate(`/legal/${v}`); };

  const columns = [
    { key: 'caseId', header: 'Case', render: (r) => <span className="text-mono">{r.caseId}</span> },
    { key: 'accountId', header: 'Account', render: (r) => <span className="text-mono">{r.accountId}</span> },
    { key: 'caseType', header: 'Type', render: (r) => titleCase(r.caseType) },
    { key: 'courtName', header: 'Court', render: (r) => r.courtName },
    { key: 'caseNumber', header: 'Case No.', render: (r) => r.caseNumber },
    { key: 'filingDate', header: 'Filed', render: (r) => date(r.filingDate) },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge value={r.status} /> },
    { key: '_a', header: '', render: (r) => canWrite && (
      <Button size="sm" variant="light" title="Edit case"
        onClick={(e) => { e.stopPropagation(); setEditing(r); }}><i className="bi bi-pencil" /></Button>
    ) },
  ];
  return (<>
    <div className="d-flex justify-content-between align-items-center gap-2 mb-2 flex-wrap">
      <InputGroup size="sm" style={{ maxWidth: 340 }}>
        <InputGroup.Text><i className="bi bi-search" /></InputGroup.Text>
        <Form.Control placeholder="Open case by ID…" value={search}
          onChange={(e) => setSearch(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') goToCase(search); }} />
        <Button variant="outline-primary" onClick={() => goToCase(search)}>Open</Button>
      </InputGroup>
      {canWrite && <Button size="sm" onClick={() => setShow(true)}><i className="bi bi-plus-lg me-1" />File case</Button>}
    </div>
    <ErrorNote error={error} />
    <DataTable columns={columns} page={page} loading={loading} onPageChange={setPage}
      onRowClick={(r) => navigate(`/legal/${r.caseId}`)} emptyIcon="bank" emptyTitle="No legal cases" />

    <FormModal show={show} title="File Legal Case" submitLabel="File case"
      initial={{ accountId: '', caseType: '', filingDate: today(), courtName: '', caseNumber: '' }}
      onClose={() => setShow(false)} onSaved={reload} onSubmit={(v) => legalApi.createCase(v)}>
      {(v, set, errs) => (<Row>
        <Col md={6}><Field label="Account ID" name="accountId" value={v.accountId} onChange={set} error={errs.accountId} required /></Col>
        <Col md={6}><Field label="Case Type" name="caseType" type="select" options={ENUMS.CaseType} value={v.caseType} onChange={set} error={errs.caseType} required /></Col>
        <Col md={6}><Field label="Court Name" name="courtName" value={v.courtName} onChange={set} error={errs.courtName} required /></Col>
        <Col md={6}><Field label="Case Number" name="caseNumber" value={v.caseNumber} onChange={set} error={errs.caseNumber} required /></Col>
        <Col md={6}><Field label="Filing Date" name="filingDate" type="date" value={v.filingDate} onChange={set} error={errs.filingDate} required /></Col>
      </Row>)}
    </FormModal>

    <FormModal show={!!editing} title={`Edit Case ${editing?.caseId || ''}`} submitLabel="Save changes"
      initial={editing ? { caseType: editing.caseType, courtName: editing.courtName, caseNumber: editing.caseNumber,
        filingDate: editing.filingDate, status: editing.status } : {}}
      onClose={() => setEditing(null)} onSaved={reload}
      onSubmit={(v) => legalApi.updateCase(editing.caseId, v)}>
      {(v, set, errs) => (<Row>
        <Col md={6}><Field label="Case Type" name="caseType" type="select" options={ENUMS.CaseType} value={v.caseType} onChange={set} error={errs.caseType} /></Col>
        <Col md={6}><Field label="Status" name="status" type="select" options={ENUMS.CaseStatus} value={v.status} onChange={set} error={errs.status} help="Only lawful transitions are accepted" /></Col>
        <Col md={6}><Field label="Court Name" name="courtName" value={v.courtName} onChange={set} error={errs.courtName} /></Col>
        <Col md={6}><Field label="Case Number" name="caseNumber" value={v.caseNumber} onChange={set} error={errs.caseNumber} /></Col>
        <Col md={6}><Field label="Filing Date" name="filingDate" type="date" value={v.filingDate} onChange={set} error={errs.filingDate} /></Col>
      </Row>)}
    </FormModal>
  </>);
}

const needsNextDate = (o) => o === 'ADJOURNED' || o === 'PARTIALLY_HEARD';
const passesOrder = (o) => o === 'ORDER_PASSED';

/** Modal to record the outcome of a scheduled hearing; reveals order fields when an order is passed. */
function RecordOutcomeModal({ legalCase, onClose, onSaved }) {
  return (
    <FormModal show={!!legalCase} title={`Record Hearing Outcome — ${legalCase?.caseId || ''}`} submitLabel="Save outcome"
      initial={{ hearingDate: today(), hearingOutcome: '', nextHearingDate: '', notes: '', orderType: '', executionDeadline: '' }}
      onClose={onClose} onSaved={onSaved}
      onSubmit={(v) => legalApi.addHearing({
        caseId: legalCase.caseId,
        hearingDate: v.hearingDate,
        hearingOutcome: v.hearingOutcome,
        nextHearingDate: needsNextDate(v.hearingOutcome) ? (v.nextHearingDate || null) : null,
        notes: v.notes || null,
        orderType: passesOrder(v.hearingOutcome) ? (v.orderType || null) : null,
        executionDeadline: passesOrder(v.hearingOutcome) ? (v.executionDeadline || null) : null,
      })}>
      {(v, set, errs) => (<Row>
        <Col md={6}><Field label="Hearing Date" name="hearingDate" type="date" value={v.hearingDate} onChange={set} error={errs.hearingDate} required /></Col>
        <Col md={6}><Field label="Outcome" name="hearingOutcome" type="select" options={ENUMS.HearingOutcome} value={v.hearingOutcome} onChange={set} error={errs.hearingOutcome} required /></Col>
        {needsNextDate(v.hearingOutcome) && (
          <Col md={6}><Field label="Next Hearing Date" name="nextHearingDate" type="date" value={v.nextHearingDate} onChange={set} error={errs.nextHearingDate} help="Case stays Hearing Scheduled" /></Col>
        )}
        {passesOrder(v.hearingOutcome) && (<>
          <Col md={6}><Field label="Order Type" name="orderType" type="select" options={ENUMS.OrderType} value={v.orderType} onChange={set} error={errs.orderType} required help="A recovery order is issued automatically" /></Col>
          <Col md={6}><Field label="Execution Deadline" name="executionDeadline" type="date" value={v.executionDeadline} onChange={set} error={errs.executionDeadline} required /></Col>
        </>)}
        <Col md={12}><Field label="Notes" name="notes" value={v.notes} onChange={set} error={errs.notes} /></Col>
      </Row>)}
    </FormModal>
  );
}

function HearingsTab({ canWrite }) {
  // Cases currently in HEARING_SCHEDULED — these are awaiting a recorded outcome.
  const scheduled = useAsync(() => legalApi.listCases({ status: 'HEARING_SCHEDULED', size: 100 }), []);
  // Full hearing history across every case.
  const history = useAsync(() => legalApi.listAllHearings(), []);
  const [schedule, setSchedule] = useState(false);
  const [outcomeFor, setOutcomeFor] = useState(null);
  const [q, setQ] = useState('');

  const reloadAll = () => { scheduled.reload(); history.reload(); };

  const scheduledRows = scheduled.data?.content || [];
  const historyRows = (history.data || []).filter((h) => {
    if (!q) return true;
    const s = q.toLowerCase();
    return (h.hearingId || '').toLowerCase().includes(s) || (h.caseId || '').toLowerCase().includes(s);
  });
  // Latest scheduled next-hearing date per case, for the awaiting-outcome list.
  const nextByCase = {};
  (history.data || []).forEach((h) => { if (h.nextHearingDate) nextByCase[h.caseId] = h.nextHearingDate; });

  return (<>
    <div className="d-flex justify-content-between align-items-center gap-2 mb-2 flex-wrap">
      <h6 className="mb-0">Cases awaiting outcome <span className="text-muted fw-normal">(Hearing Scheduled)</span></h6>
      {canWrite && <Button size="sm" onClick={() => setSchedule(true)}><i className="bi bi-calendar-plus me-1" />Schedule hearing</Button>}
    </div>
    <ErrorNote error={scheduled.error} />
    <div className="card mb-4">
      {scheduled.loading ? <div className="card-body"><Loading /></div>
        : !scheduledRows.length ? <div className="card-body"><EmptyState icon="calendar-event" title="No cases awaiting a hearing outcome" /></div> : (
        <Table responsive hover className="mb-0"><thead><tr>
          <th>Case</th><th>Account</th><th>Next Hearing</th><th></th>
        </tr></thead><tbody>
          {scheduledRows.map((c) => (
            <tr key={c.caseId}>
              <td className="text-mono">{c.caseId}</td>
              <td className="text-mono">{c.accountId}</td>
              <td>{date(nextByCase[c.caseId])}</td>
              <td className="text-end">{canWrite
                && <Button size="sm" onClick={() => setOutcomeFor(c)}><i className="bi bi-clipboard-check me-1" />Record outcome</Button>}</td>
            </tr>
          ))}
        </tbody></Table>
      )}
    </div>

    <div className="d-flex justify-content-between align-items-center gap-2 mb-2 flex-wrap">
      <h6 className="mb-0">Hearing history</h6>
      <SearchBox value={q} onChange={setQ} placeholder="Filter by hearing / case ID…" />
    </div>
    <ErrorNote error={history.error} />
    <div className="card">
      {history.loading ? <div className="card-body"><Loading /></div>
        : !historyRows.length ? <div className="card-body"><EmptyState icon="calendar-event" title="No hearings" /></div> : (
        <Table responsive hover className="mb-0"><thead><tr>
          <th>Hearing</th><th>Case</th><th>Date</th><th>Outcome</th><th>Next Hearing</th><th>Notes</th>
        </tr></thead><tbody>
          {historyRows.map((h) => (
            <tr key={h.hearingId}>
              <td className="text-mono">{h.hearingId}</td>
              <td className="text-mono">{h.caseId}</td>
              <td>{date(h.hearingDate)}</td>
              <td>{h.hearingOutcome ? <StatusBadge value={h.hearingOutcome} /> : <span className="text-muted small">Scheduled</span>}</td>
              <td>{date(h.nextHearingDate)}</td>
              <td>{h.notes}</td>
            </tr>
          ))}
        </tbody></Table>
      )}
    </div>

    {/* Schedule a hearing (no outcome yet) — moves the case to Hearing Scheduled. */}
    <FormModal show={schedule} title="Schedule Court Hearing" submitLabel="Schedule hearing"
      initial={{ caseId: '', hearingDate: today(), notes: '' }}
      onClose={() => setSchedule(false)} onSaved={reloadAll}
      onSubmit={(v) => legalApi.addHearing({ caseId: v.caseId, hearingDate: v.hearingDate,
        hearingOutcome: null, nextHearingDate: v.hearingDate, notes: v.notes || null })}>
      {(v, set, errs) => (<Row>
        <Col md={6}><Field label="Case ID" name="caseId" value={v.caseId} onChange={set} error={errs.caseId} required help="Case must be open (not settled/decreed/withdrawn)" /></Col>
        <Col md={6}><Field label="Hearing Date" name="hearingDate" type="date" value={v.hearingDate} onChange={set} error={errs.hearingDate} required /></Col>
        <Col md={12}><Field label="Notes" name="notes" value={v.notes} onChange={set} error={errs.notes} /></Col>
      </Row>)}
    </FormModal>

    <RecordOutcomeModal legalCase={outcomeFor} onClose={() => setOutcomeFor(null)} onSaved={reloadAll} />
  </>);
}

function OrdersTab({ canWrite }) {
  const toast = useToast();
  const { data, loading, error, reload } = useAsync(() => legalApi.listOrders(), []);
  const [show, setShow] = useState(false);
  const [del, setDel] = useState(null);
  const [q, setQ] = useState('');

  const rows = (data || []).filter((o) => {
    if (!q) return true;
    const s = q.toLowerCase();
    return (o.orderId || '').toLowerCase().includes(s) || (o.caseId || '').toLowerCase().includes(s);
  });

  const doDelete = async () => {
    try { await legalApi.deleteOrder(del.orderId); toast.success('Order deleted'); setDel(null); reload(); }
    catch { toast.error('Could not delete order'); }
  };

  if (loading) return <Loading />;
  return (<>
    <div className="d-flex justify-content-between align-items-center gap-2 mb-2 flex-wrap">
      <SearchBox value={q} onChange={setQ} placeholder="Filter by order / case ID…" />
      {canWrite && <Button size="sm" onClick={() => setShow(true)}><i className="bi bi-plus-lg me-1" />Issue order</Button>}
    </div>
    <ErrorNote error={error} />
    <div className="card">
      {!rows.length ? <div className="card-body"><EmptyState icon="file-earmark-text" title="No recovery orders" /></div> : (
        <Table responsive hover className="mb-0"><thead><tr>
          <th>Order</th><th>Case</th><th>Type</th><th>Issued</th><th>Deadline</th><th>Status</th><th></th>
        </tr></thead><tbody>
          {rows.map((o) => (
            <tr key={o.orderId}>
              <td className="text-mono">{o.orderId}</td><td className="text-mono">{o.caseId}</td>
              <td>{titleCase(o.orderType)}</td><td>{date(o.issuedDate)}</td><td>{date(o.executionDeadline)}</td>
              <td><StatusBadge value={o.status} /></td>
              <td className="text-end">{canWrite && <Button size="sm" variant="light" className="text-danger" onClick={() => setDel(o)}><i className="bi bi-trash" /></Button>}</td>
            </tr>
          ))}
        </tbody></Table>
      )}
    </div>
    <FormModal show={show} title="Issue Recovery Order" submitLabel="Issue order"
      initial={{ caseId: '', orderType: '', issuedDate: today(), executionDeadline: '' }}
      onClose={() => setShow(false)} onSaved={reload} onSubmit={(v) => legalApi.issueOrder(v)}>
      {(v, set, errs) => (<Row>
        <Col md={6}><Field label="Case ID" name="caseId" value={v.caseId} onChange={set} error={errs.caseId} required help="Case must be DECREED (an order was passed at a hearing)" /></Col>
        <Col md={6}><Field label="Order Type" name="orderType" type="select" options={ENUMS.OrderType} value={v.orderType} onChange={set} error={errs.orderType} required /></Col>
        <Col md={6}><Field label="Issued Date" name="issuedDate" type="date" value={v.issuedDate} onChange={set} error={errs.issuedDate} required /></Col>
        <Col md={6}><Field label="Execution Deadline" name="executionDeadline" type="date" value={v.executionDeadline} onChange={set} error={errs.executionDeadline} required /></Col>
      </Row>)}
    </FormModal>
    <ConfirmDialog show={!!del} title="Delete recovery order" variant="danger" confirmLabel="Delete"
      body={<>Delete order <strong>{del?.orderId}</strong>?</>} onCancel={() => setDel(null)} onConfirm={doDelete} />
  </>);
}
