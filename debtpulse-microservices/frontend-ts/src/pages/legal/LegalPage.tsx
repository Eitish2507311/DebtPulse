import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Tabs, Tab, Button, Row, Col, Table, Form, InputGroup } from 'react-bootstrap';
import { legalApi } from '../../api/services';
import { usePaged, useAsync } from '../../hooks/usePaged';
import { useAuth } from '../../auth/AuthContext';
import { PageHeader, ErrorNote, StatusBadge, Loading, EmptyState } from '../../components/ui';
import DataTable from '../../components/DataTable';
import FormModal from '../../components/FormModal';
import ConfirmDialog from '../../components/ConfirmDialog';
import Field from '../../components/Field';
import { useToast } from '../../components/ToastHost';
import { ENUMS } from '../../utils/enums';
import { date, titleCase, today } from '../../utils/format';
import type { LegalCase, CourtHearing, RecoveryOrder, Column } from '../../types';

/** Small reusable filter box (client-side) with a clear button. */
function SearchBox({ value, onChange, placeholder }: { value: string; onChange: (v: string) => void; placeholder: string }) {
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
  const canWrite = role === 'ADMIN' || role === 'LEGAL_OFFICER';
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

function CasesTab({ canWrite }: { canWrite: boolean }) {
  const navigate = useNavigate();
  const fetcher = useCallback((p: Record<string, unknown>) => legalApi.listCases(p), []);
  const { page, loading, error, setPage, reload } = usePaged<LegalCase>(fetcher);
  const [show, setShow] = useState(false);
  const [editing, setEditing] = useState<LegalCase | null>(null);
  const [search, setSearch] = useState('');

  const goToCase = (id: string) => { const v = id.trim(); if (v) navigate(`/legal/${v}`); };

  const columns: Column<LegalCase>[] = [
    { key: 'caseId', header: 'Case', render: (r) => <span className="text-mono">{r.caseId}</span> },
    { key: 'accountId', header: 'Account', render: (r) => <span className="text-mono">{r.accountId}</span> },
    { key: 'caseType', header: 'Type', render: (r) => titleCase(r.caseType) },
    { key: 'courtName', header: 'Court', render: (r) => r.courtName },
    { key: 'caseNumber', header: 'Case No.', render: (r) => r.caseNumber },
    { key: 'filingDate', header: 'Filed', render: (r) => date(r.filingDate) },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge value={r.status} /> },
    { key: '_a', header: '', render: (r) => canWrite ? (
      <Button size="sm" variant="light" title="Edit case"
        onClick={(e) => { e.stopPropagation(); setEditing(r); }}><i className="bi bi-pencil" /></Button>
    ) : null },
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
    <DataTable<LegalCase> columns={columns} page={page} loading={loading} onPageChange={setPage}
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
      onSubmit={(v) => legalApi.updateCase(editing!.caseId, v)}>
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

function HearingsTab({ canWrite }: { canWrite: boolean }) {
  const { data, loading, error, reload } = useAsync<CourtHearing[]>(() => legalApi.listAllHearings(), []);
  const [show, setShow] = useState(false);
  const [q, setQ] = useState('');

  const rows = (data || []).filter((h) => {
    if (!q) return true;
    const s = q.toLowerCase();
    return (h.hearingId || '').toLowerCase().includes(s) || (h.caseId || '').toLowerCase().includes(s);
  });

  if (loading) return <Loading />;
  return (<>
    <div className="d-flex justify-content-between align-items-center gap-2 mb-2 flex-wrap">
      <SearchBox value={q} onChange={setQ} placeholder="Filter by hearing / case ID…" />
      {canWrite && <Button size="sm" onClick={() => setShow(true)}><i className="bi bi-plus-lg me-1" />Record hearing</Button>}
    </div>
    <ErrorNote error={error} />
    <div className="card">
      {!rows.length ? <div className="card-body"><EmptyState icon="calendar-event" title="No hearings" /></div> : (
        <Table hover className="mb-0"><thead><tr>
          <th>Hearing</th><th>Case</th><th>Date</th><th>Outcome</th><th>Next Hearing</th><th>Notes</th>
        </tr></thead><tbody>
          {rows.map((h) => (
            <tr key={h.hearingId}>
              <td className="text-mono">{h.hearingId}</td>
              <td className="text-mono">{h.caseId}</td>
              <td>{date(h.hearingDate)}</td>
              <td><StatusBadge value={h.hearingOutcome} /></td>
              <td>{date(h.nextHearingDate)}</td>
              <td>{h.notes}</td>
            </tr>
          ))}
        </tbody></Table>
      )}
    </div>
    <FormModal show={show} title="Record Court Hearing" submitLabel="Record hearing"
      initial={{ caseId: '', hearingDate: today(), hearingOutcome: '', nextHearingDate: '', notes: '' }}
      onClose={() => setShow(false)} onSaved={reload}
      onSubmit={(v) => legalApi.addHearing({ ...v, nextHearingDate: v.nextHearingDate || null })}>
      {(v, set, errs) => (<Row>
        <Col md={6}><Field label="Case ID" name="caseId" value={v.caseId} onChange={set} error={errs.caseId} required /></Col>
        <Col md={6}><Field label="Hearing Date" name="hearingDate" type="date" value={v.hearingDate} onChange={set} error={errs.hearingDate} required /></Col>
        <Col md={6}><Field label="Outcome" name="hearingOutcome" type="select" options={ENUMS.HearingOutcome} value={v.hearingOutcome} onChange={set} error={errs.hearingOutcome} required /></Col>
        <Col md={6}><Field label="Next Hearing" name="nextHearingDate" type="date" value={v.nextHearingDate} onChange={set} error={errs.nextHearingDate} help="Sets the case to Hearing Scheduled" /></Col>
        <Col md={12}><Field label="Notes" name="notes" value={v.notes} onChange={set} error={errs.notes} /></Col>
      </Row>)}
    </FormModal>
  </>);
}

function OrdersTab({ canWrite }: { canWrite: boolean }) {
  const toast = useToast();
  const { data, loading, error, reload } = useAsync<RecoveryOrder[]>(() => legalApi.listOrders(), []);
  const [show, setShow] = useState(false);
  const [del, setDel] = useState<RecoveryOrder | null>(null);
  const [q, setQ] = useState('');

  const rows = (data || []).filter((o) => {
    if (!q) return true;
    const s = q.toLowerCase();
    return (o.orderId || '').toLowerCase().includes(s) || (o.caseId || '').toLowerCase().includes(s);
  });

  const doDelete = async () => {
    if (!del) return;
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
        <Table hover className="mb-0"><thead><tr>
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
        <Col md={6}><Field label="Case ID" name="caseId" value={v.caseId} onChange={set} error={errs.caseId} required /></Col>
        <Col md={6}><Field label="Order Type" name="orderType" type="select" options={ENUMS.OrderType} value={v.orderType} onChange={set} error={errs.orderType} required /></Col>
        <Col md={6}><Field label="Issued Date" name="issuedDate" type="date" value={v.issuedDate} onChange={set} error={errs.issuedDate} required /></Col>
        <Col md={6}><Field label="Execution Deadline" name="executionDeadline" type="date" value={v.executionDeadline} onChange={set} error={errs.executionDeadline} required /></Col>
      </Row>)}
    </FormModal>
    <ConfirmDialog show={!!del} title="Delete recovery order" variant="danger" confirmLabel="Delete"
      body={<>Delete order <strong>{del?.orderId}</strong>?</>} onCancel={() => setDel(null)} onConfirm={doDelete} />
  </>);
}
