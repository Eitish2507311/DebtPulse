import { useState, useCallback } from 'react';
import { Tabs, Tab, Button, Row, Col, Dropdown, InputGroup, Form } from 'react-bootstrap';
import { visitApi, assetVerificationApi } from '../../api/services.js';
import { usePaged } from '../../hooks/usePaged.js';
import { useAuth } from '../../auth/AuthContext.jsx';
import { ROLES } from '../../auth/roles.js';
import { PageHeader, ErrorNote, StatusBadge } from '../../components/ui.jsx';
import DataTable from '../../components/DataTable.jsx';
import FormModal from '../../components/FormModal.jsx';
import Field from '../../components/Field.jsx';
import { useToast } from '../../components/ToastHost.jsx';
import { ENUMS } from '../../utils/enums.js';
import { inr, date, titleCase, today } from '../../utils/format.js';

/** Client-side search box (filters the loaded page) with a clear button. */
function SearchBar({ value, onChange, placeholder }) {
  return (
    <InputGroup size="sm" style={{ maxWidth: 340 }} className="filter-bar">
      <InputGroup.Text><i className="bi bi-search" /></InputGroup.Text>
      <Form.Control value={value} placeholder={placeholder} onChange={(e) => onChange(e.target.value)} />
      {value && <Button variant="outline-secondary" onClick={() => onChange('')}><i className="bi bi-x" /></Button>}
    </InputGroup>
  );
}

const matches = (q, ...fields) => {
  const s = q.trim().toLowerCase();
  return !s || fields.some((f) => (f || '').toString().toLowerCase().includes(s));
};

export default function FieldWorkspace() {
  const { role } = useAuth();
  return (
    <>
      <PageHeader title="Field Recovery" subtitle="Schedule visits, record outcomes and verify collateral assets" icon="geo-alt" />
      <Tabs defaultActiveKey="visits" className="mb-3">
        <Tab eventKey="visits" title="Field Visits"><VisitsTab canWrite={[ROLES.ADMIN, ROLES.FIELD_OFFICER, ROLES.PORTFOLIO_MANAGER].includes(role)} /></Tab>
        <Tab eventKey="assets" title="Asset Verification"><AssetTab canWrite={[ROLES.ADMIN, ROLES.FIELD_OFFICER].includes(role)} /></Tab>
      </Tabs>
    </>
  );
}

function VisitsTab({ canWrite }) {
  const toast = useToast();
  const fetcher = useCallback((p) => visitApi.list(p), []);
  const { page, loading, error, setPage, reload } = usePaged(fetcher);
  const [show, setShow] = useState(false);
  const [complete, setComplete] = useState(null);
  const [q, setQ] = useState('');
  const shown = page ? { ...page, content: (page.content || []).filter((r) => matches(q, r.visitId, r.accountId, r.officerId, r.status)) } : page;

  const markMissed = async (r) => {
    try { await visitApi.markMissed(r.visitId); toast.success('Visit marked missed'); reload(); }
    catch { toast.error('Could not update visit'); }
  };

  const columns = [
    { key: 'visitId', header: 'Visit', render: (r) => <span className="text-mono">{r.visitId}</span> },
    { key: 'accountId', header: 'Account', render: (r) => <span className="text-mono">{r.accountId}</span> },
    { key: 'officerId', header: 'Officer', render: (r) => <span className="text-mono">{r.officerId}</span> },
    { key: 'scheduledDate', header: 'Scheduled', render: (r) => date(r.scheduledDate) },
    { key: 'visitDate', header: 'Visited', render: (r) => date(r.visitDate) },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge value={r.status} /> },
    { key: '_a', header: '', render: (r) => canWrite && r.status === 'SCHEDULED' && (
      <Dropdown align="end"><Dropdown.Toggle size="sm" variant="light">Actions</Dropdown.Toggle>
        <Dropdown.Menu renderOnMount popperConfig={{ strategy: 'fixed' }}>
          <Dropdown.Item onClick={() => setComplete(r)}>Complete visit</Dropdown.Item>
          <Dropdown.Item className="text-danger" onClick={() => markMissed(r)}>Mark missed</Dropdown.Item>
        </Dropdown.Menu>
      </Dropdown>
    ) },
  ];
  return (<>
    <div className="d-flex justify-content-between align-items-center gap-2 mb-2 flex-wrap">
      <SearchBar value={q} onChange={setQ} placeholder="Search by visit / account / officer ID or status…" />
      {canWrite && <Button size="sm" onClick={() => setShow(true)}><i className="bi bi-plus-lg me-1" />Schedule visit</Button>}
    </div>
    <ErrorNote error={error} />
    <DataTable columns={columns} page={shown} loading={loading} onPageChange={setPage} emptyIcon="geo-alt" emptyTitle="No field visits" />

    <FormModal show={show} title="Schedule Field Visit" submitLabel="Schedule"
      initial={{ accountId: '', officerId: '', scheduledDate: today(), nextActionRequired: '' }}
      onClose={() => setShow(false)} onSaved={reload} onSubmit={(v) => visitApi.schedule(v)}>
      {(v, set, errs) => (<>
        <Row>
          <Col md={6}><Field label="Account ID" name="accountId" value={v.accountId} onChange={set} error={errs.accountId} required /></Col>
          <Col md={6}><Field label="Officer ID" name="officerId" value={v.officerId} onChange={set} error={errs.officerId} required /></Col>
        </Row>
        <Field label="Scheduled Date" name="scheduledDate" type="date" value={v.scheduledDate} onChange={set} error={errs.scheduledDate} help="Today or later" required />
        <Field label="Next Action Required" name="nextActionRequired" value={v.nextActionRequired} onChange={set} error={errs.nextActionRequired} />
      </>)}
    </FormModal>

    <FormModal show={!!complete} title="Complete Field Visit" submitLabel="Complete"
      initial={{ visitDate: today(), borrowerMet: 'true', assetSighted: 'false', outcomeSummary: '', nextActionRequired: '' }}
      onClose={() => setComplete(null)} onSaved={reload}
      onSubmit={(v) => visitApi.complete(complete.visitId, {
        visitDate: v.visitDate, borrowerMet: v.borrowerMet === 'true', assetSighted: v.assetSighted === 'true',
        outcomeSummary: v.outcomeSummary, nextActionRequired: v.nextActionRequired })}>
      {(v, set, errs) => (<>
        <Field label="Visit Date" name="visitDate" type="date" value={v.visitDate} onChange={set} error={errs.visitDate} />
        <Row>
          <Col md={6}><Field label="Borrower Met" name="borrowerMet" type="select" options={[{ value: 'true', label: 'Yes' }, { value: 'false', label: 'No' }]} value={v.borrowerMet} onChange={set} /></Col>
          <Col md={6}><Field label="Asset Sighted" name="assetSighted" type="select" options={[{ value: 'true', label: 'Yes' }, { value: 'false', label: 'No' }]} value={v.assetSighted} onChange={set} /></Col>
        </Row>
        <Field label="Outcome Summary" name="outcomeSummary" type="textarea" value={v.outcomeSummary} onChange={set} error={errs.outcomeSummary} />
        <Field label="Next Action Required" name="nextActionRequired" value={v.nextActionRequired} onChange={set} error={errs.nextActionRequired} />
      </>)}
    </FormModal>
  </>);
}

function AssetTab({ canWrite }) {
  const fetcher = useCallback((p) => assetVerificationApi.list(p), []);
  const { page, loading, error, setPage, reload } = usePaged(fetcher);
  const [show, setShow] = useState(false);
  const [q, setQ] = useState('');
  const shown = page ? { ...page, content: (page.content || []).filter((r) => matches(q, r.reportId, r.visitId, r.assetId)) } : page;

  const columns = [
    { key: 'reportId', header: 'Report', render: (r) => <span className="text-mono">{r.reportId}</span> },
    { key: 'visitId', header: 'Visit', render: (r) => <span className="text-mono">{r.visitId}</span> },
    { key: 'assetId', header: 'Asset', render: (r) => <span className="text-mono">{r.assetId}</span> },
    { key: 'physicalCondition', header: 'Condition', render: (r) => <StatusBadge value={r.physicalCondition || r.condition} /> },
    { key: 'estimatedRealisableValue', header: 'Realisable', className: 'text-end', render: (r) => inr(r.estimatedRealisableValue ?? r.realisableValue) },
    { key: 'verificationDate', header: 'Verified', render: (r) => date(r.verificationDate) },
  ];
  return (<>
    <div className="d-flex justify-content-between align-items-center gap-2 mb-2 flex-wrap">
      <SearchBar value={q} onChange={setQ} placeholder="Search by report / visit / asset ID…" />
      {canWrite && <Button size="sm" onClick={() => setShow(true)}><i className="bi bi-plus-lg me-1" />New report</Button>}
    </div>
    <ErrorNote error={error} />
    <DataTable columns={columns} page={shown} loading={loading} onPageChange={setPage} emptyIcon="clipboard-check" emptyTitle="No verification reports" />
    <FormModal show={show} title="Asset Verification Report" submitLabel="Create report"
      initial={{ visitId: '', assetId: '', condition: '', currentLocation: '', realisableValue: '', remarks: '', verificationDate: today() }}
      onClose={() => setShow(false)} onSaved={reload}
      onSubmit={(v) => assetVerificationApi.create({ ...v, realisableValue: v.realisableValue === '' ? null : Number(v.realisableValue) })}>
      {(v, set, errs) => (<>
        <Row>
          <Col md={6}><Field label="Visit ID" name="visitId" value={v.visitId} onChange={set} error={errs.visitId} required /></Col>
          <Col md={6}><Field label="Asset ID" name="assetId" value={v.assetId} onChange={set} error={errs.assetId} required /></Col>
        </Row>
        <Row>
          <Col md={6}><Field label="Condition" name="condition" type="select" options={ENUMS.AssetCondition} value={v.condition} onChange={set} error={errs.condition} required /></Col>
          <Col md={6}><Field label="Realisable Value" name="realisableValue" type="number" min="0" value={v.realisableValue} onChange={set} error={errs.realisableValue} /></Col>
        </Row>
        <Field label="Current Location" name="currentLocation" value={v.currentLocation} onChange={set} error={errs.currentLocation} />
        <Field label="Verification Date" name="verificationDate" type="date" value={v.verificationDate} onChange={set} error={errs.verificationDate} />
        <Field label="Remarks" name="remarks" type="textarea" value={v.remarks} onChange={set} error={errs.remarks} />
      </>)}
    </FormModal>
  </>);
}
