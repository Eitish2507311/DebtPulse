import { useState, useCallback } from 'react';
import { Tabs, Tab, Button, Row, Col, Form, Dropdown } from 'react-bootstrap';
import { contactApi, ptpApi, borrowerContactApi } from '../../api/services.js';
import { usePaged } from '../../hooks/usePaged.js';
import { useAuth } from '../../auth/AuthContext.jsx';
import { ROLES } from '../../auth/roles.js';
import { PageHeader, ErrorNote, StatusBadge } from '../../components/ui.jsx';
import DataTable from '../../components/DataTable.jsx';
import FormModal from '../../components/FormModal.jsx';
import ConfirmDialog from '../../components/ConfirmDialog.jsx';
import Field from '../../components/Field.jsx';
import { useToast } from '../../components/ToastHost.jsx';
import { ENUMS } from '../../utils/enums.js';
import { inr, date, dateTime, titleCase, today } from '../../utils/format.js';

export default function ContactWorkspace() {
  const { role } = useAuth();
  const canAgent = [ROLES.ADMIN, ROLES.COLLECTIONS_AGENT].includes(role);
  return (
    <>
      <PageHeader title="Contact & Follow-Up" subtitle="Log contact attempts, track promises-to-pay and manage borrower contacts" icon="telephone" />
      <Tabs defaultActiveKey="contacts" className="mb-3">
        <Tab eventKey="contacts" title="Contact Attempts"><ContactsTab canWrite={[ROLES.ADMIN, ROLES.PORTFOLIO_MANAGER, ROLES.COLLECTIONS_AGENT].includes(role)} /></Tab>
        <Tab eventKey="ptp" title="Promises to Pay"><PtpTab canWrite={canAgent} /></Tab>
        <Tab eventKey="borrower" title="Borrower Contacts"><BorrowerTab canWrite={canAgent} /></Tab>
      </Tabs>
    </>
  );
}

function AccountFilter({ value, onChange }) {
  return (
    <Row className="g-2 mb-3"><Col sm={5} md={4}>
      <Form.Control size="sm" placeholder="Filter by Account ID…" value={value} onChange={(e) => onChange(e.target.value)} />
    </Col></Row>
  );
}

function ContactsTab({ canWrite }) {
  const [f, setF] = useState({ accountId: '', agentId: '' });
  const onFilter = (name, value) => setF((s) => ({ ...s, [name]: value }));
  const filters = Object.fromEntries(Object.entries(f).filter(([, v]) => v && v.trim()));
  const fetcher = useCallback((p) => contactApi.list(p), []);
  const { page, loading, error, setPage, reload } = usePaged(fetcher, { filters });
  const [show, setShow] = useState(false);

  const columns = [
    { key: 'contactId', header: 'Contact', render: (r) => <span className="text-mono">{r.contactId}</span> },
    { key: 'accountId', header: 'Account', render: (r) => <span className="text-mono">{r.accountId}</span> },
    { key: 'channel', header: 'Channel', render: (r) => titleCase(r.channel) },
    { key: 'outcome', header: 'Outcome', render: (r) => <StatusBadge value={r.outcome} /> },
    { key: 'contactDate', header: 'When', render: (r) => dateTime(r.contactDate) },
    { key: 'notes', header: 'Notes', render: (r) => r.notes || '—' },
  ];
  return (<>
    <Row className="g-2 mb-3 align-items-end">
      <Col sm={6} md={4}><Field label="Filter by Account ID" name="accountId" value={f.accountId} onChange={onFilter} /></Col>
      <Col sm={6} md={4}><Field label="Filter by Agent ID" name="agentId" value={f.agentId} onChange={onFilter} /></Col>
      <Col md={4} className="text-md-end">
        {canWrite && <Button size="sm" onClick={() => setShow(true)}><i className="bi bi-plus-lg me-1" />Log contact</Button>}
      </Col>
    </Row>
    <ErrorNote error={error} />
    <DataTable columns={columns} page={page} loading={loading} onPageChange={setPage} emptyIcon="telephone" emptyTitle="No contact attempts" />
    <FormModal show={show} title="Log Contact Attempt" submitLabel="Log contact"
      initial={{ accountId: f.accountId || '', channel: '', outcome: '', notes: '', contactDate: '' }}
      onClose={() => setShow(false)} onSaved={reload} onSubmit={(v) => contactApi.create(v)}>
      {(v, set, errs) => (<>
        <Field label="Account ID" name="accountId" value={v.accountId} onChange={set} error={errs.accountId} required />
        <Row>
          <Col md={6}><Field label="Channel" name="channel" type="select" options={ENUMS.ContactChannel} value={v.channel} onChange={set} error={errs.channel} required /></Col>
          <Col md={6}><Field label="Outcome" name="outcome" type="select" options={ENUMS.ContactOutcome} value={v.outcome} onChange={set} error={errs.outcome} required /></Col>
        </Row>
        <Field label="Notes" name="notes" type="textarea" value={v.notes} onChange={set} error={errs.notes} />
      </>)}
    </FormModal>
  </>);
}

function PtpTab({ canWrite }) {
  const toast = useToast();
  const [acc, setAcc] = useState('');
  const filters = acc ? { accountId: acc } : {};
  const fetcher = useCallback((p) => ptpApi.list(p), []);
  const { page, loading, error, setPage, reload } = usePaged(fetcher, { filters });
  const [show, setShow] = useState(false);
  const [pay, setPay] = useState(null);
  const [resched, setResched] = useState(null);
  const [edit, setEdit] = useState(null);

  const columns = [
    { key: 'ptpId', header: 'PTP', render: (r) => <span className="text-mono">{r.ptpId}</span> },
    { key: 'accountId', header: 'Account', render: (r) => <span className="text-mono">{r.accountId}</span> },
    { key: 'ptpAmount', header: 'Amount', className: 'text-end', render: (r) => inr(r.ptpAmount) },
    { key: 'actualPaidAmount', header: 'Paid', className: 'text-end', render: (r) => inr(r.actualPaidAmount) },
    { key: 'commitmentDate', header: 'Commitment', render: (r) => date(r.commitmentDate) },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge value={r.status} /> },
    { key: '_a', header: '', render: (r) => canWrite && (
      <Dropdown align="end"><Dropdown.Toggle size="sm" variant="light">Actions</Dropdown.Toggle>
        <Dropdown.Menu>
          <Dropdown.Item onClick={() => setEdit(r)}>Edit</Dropdown.Item>
          <Dropdown.Item onClick={() => setPay(r)}>Record payment</Dropdown.Item>
          <Dropdown.Item onClick={() => setResched(r)}>Reschedule</Dropdown.Item>
        </Dropdown.Menu>
      </Dropdown>
    ) },
  ];
  return (<>
    <div className="d-flex justify-content-between align-items-center">
      <AccountFilter value={acc} onChange={setAcc} />
      {canWrite && <Button size="sm" onClick={() => setShow(true)}><i className="bi bi-plus-lg me-1" />New PTP</Button>}
    </div>
    <ErrorNote error={error} />
    <DataTable columns={columns} page={page} loading={loading} onPageChange={setPage} emptyIcon="hand-thumbs-up" emptyTitle="No promises to pay" />

    <FormModal show={show} title="Record Promise-to-Pay" submitLabel="Create PTP"
      initial={{ accountId: acc || '', ptpDate: today(), ptpAmount: '', commitmentDate: '' }}
      onClose={() => setShow(false)} onSaved={reload}
      onSubmit={(v) => ptpApi.create({ ...v, ptpAmount: Number(v.ptpAmount) })}>
      {(v, set, errs) => (<>
        <Field label="Account ID" name="accountId" value={v.accountId} onChange={set} error={errs.accountId} required />
        <Row>
          <Col md={4}><Field label="PTP Date" name="ptpDate" type="date" value={v.ptpDate} onChange={set} error={errs.ptpDate} required /></Col>
          <Col md={4}><Field label="Amount" name="ptpAmount" type="number" min="0" value={v.ptpAmount} onChange={set} error={errs.ptpAmount} required /></Col>
          <Col md={4}><Field label="Commitment Date" name="commitmentDate" type="date" value={v.commitmentDate} onChange={set} error={errs.commitmentDate} required /></Col>
        </Row>
      </>)}
    </FormModal>

    <FormModal show={!!pay} title="Record Payment" submitLabel="Record"
      initial={{ actualPaidAmount: '' }} onClose={() => setPay(null)} onSaved={reload}
      onSubmit={async (v) => { await ptpApi.recordPayment(pay.ptpId, Number(v.actualPaidAmount)); toast.success('Payment recorded'); }}>
      {(v, set, errs) => <Field label="Amount Paid" name="actualPaidAmount" type="number" min="0" value={v.actualPaidAmount} onChange={set} error={errs.actualPaidAmount} required />}
    </FormModal>

    <FormModal show={!!resched} title="Reschedule PTP" submitLabel="Reschedule"
      initial={{ commitmentDate: '' }} onClose={() => setResched(null)} onSaved={reload}
      onSubmit={async (v) => { await ptpApi.reschedule(resched.ptpId, v.commitmentDate); toast.success('PTP rescheduled'); }}>
      {(v, set, errs) => <Field label="New Commitment Date" name="commitmentDate" type="date" value={v.commitmentDate} onChange={set} error={errs.commitmentDate} required />}
    </FormModal>

    <FormModal show={!!edit} title={`Edit PTP ${edit?.ptpId || ''}`} submitLabel="Save changes"
      initial={edit ? { ptpDate: edit.ptpDate, ptpAmount: edit.ptpAmount, commitmentDate: edit.commitmentDate } : {}}
      onClose={() => setEdit(null)} onSaved={reload}
      onSubmit={(v) => ptpApi.update(edit.ptpId, { accountId: edit.accountId, agentId: edit.agentId, ...v, ptpAmount: Number(v.ptpAmount) })}>
      {(v, set, errs) => (<Row>
        <Col md={4}><Field label="PTP Date" name="ptpDate" type="date" value={v.ptpDate} onChange={set} error={errs.ptpDate} required /></Col>
        <Col md={4}><Field label="Amount" name="ptpAmount" type="number" min="0" value={v.ptpAmount} onChange={set} error={errs.ptpAmount} required /></Col>
        <Col md={4}><Field label="Commitment Date" name="commitmentDate" type="date" value={v.commitmentDate} onChange={set} error={errs.commitmentDate} required /></Col>
      </Row>)}
    </FormModal>
  </>);
}

function BorrowerTab({ canWrite }) {
  const toast = useToast();
  const fetcher = useCallback((p) => borrowerContactApi.list(p), []);
  const { page, loading, error, setPage, reload } = usePaged(fetcher);
  const [show, setShow] = useState(false);
  const [edit, setEdit] = useState(null);
  const [del, setDel] = useState(null);

  const doDelete = async () => {
    try { await borrowerContactApi.remove(del.contactRecordId); toast.success('Contact deleted'); setDel(null); reload(); }
    catch { toast.error('Could not delete contact'); }
  };

  const columns = [
    { key: 'accountId', header: 'Account', render: (r) => <span className="text-mono">{r.accountId}</span> },
    { key: 'name', header: 'Name', render: (r) => <span className="fw-semibold">{r.name}</span> },
    { key: 'contactType', header: 'Type', render: (r) => titleCase(r.contactType) },
    { key: 'phone', header: 'Phone' },
    { key: 'relationship', header: 'Relationship', render: (r) => r.relationship || '—' },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge value={r.status} /> },
    { key: '_a', header: '', render: (r) => canWrite && (
      <div className="d-flex gap-1 justify-content-end">
        <Button size="sm" variant="light" title="Edit" onClick={() => setEdit(r)}><i className="bi bi-pencil" /></Button>
        <Button size="sm" variant="light" className="text-danger" title="Delete" onClick={() => setDel(r)}><i className="bi bi-trash" /></Button>
      </div>
    ) },
  ];
  return (<>
    <div className="d-flex justify-content-end mb-2">
      {canWrite && <Button size="sm" onClick={() => setShow(true)}><i className="bi bi-plus-lg me-1" />Add contact</Button>}
    </div>
    <ErrorNote error={error} />
    <DataTable columns={columns} page={page} loading={loading} onPageChange={setPage} emptyIcon="person-lines-fill" emptyTitle="No borrower contacts" />
    <FormModal show={show} title="Add Borrower Contact" submitLabel="Add contact"
      initial={{ accountId: '', contactType: '', name: '', phone: '', relationship: '' }}
      onClose={() => setShow(false)} onSaved={reload} onSubmit={(v) => borrowerContactApi.create(v)}>
      {(v, set, errs) => (<>
        <Field label="Account ID" name="accountId" value={v.accountId} onChange={set} error={errs.accountId} required />
        <Row>
          <Col md={6}><Field label="Contact Type" name="contactType" type="select" options={ENUMS.BorrowerContactType} value={v.contactType} onChange={set} error={errs.contactType} required /></Col>
          <Col md={6}><Field label="Name" name="name" value={v.name} onChange={set} error={errs.name} required /></Col>
          <Col md={6}><Field label="Phone" name="phone" value={v.phone} onChange={set} error={errs.phone} help="10 digits" required /></Col>
          <Col md={6}><Field label="Relationship" name="relationship" value={v.relationship} onChange={set} error={errs.relationship} /></Col>
        </Row>
      </>)}
    </FormModal>

    <FormModal show={!!edit} title={`Edit Borrower Contact`} submitLabel="Save changes"
      initial={edit ? { contactType: edit.contactType, name: edit.name, phone: edit.phone,
        relationship: edit.relationship, status: edit.status } : {}}
      onClose={() => setEdit(null)} onSaved={reload}
      onSubmit={(v) => borrowerContactApi.update(edit.contactRecordId, v)}>
      {(v, set, errs) => (<Row>
        <Col md={6}><Field label="Contact Type" name="contactType" type="select" options={ENUMS.BorrowerContactType} value={v.contactType} onChange={set} error={errs.contactType} required /></Col>
        <Col md={6}><Field label="Name" name="name" value={v.name} onChange={set} error={errs.name} required /></Col>
        <Col md={6}><Field label="Phone" name="phone" value={v.phone} onChange={set} error={errs.phone} help="10 digits" required /></Col>
        <Col md={6}><Field label="Relationship" name="relationship" value={v.relationship} onChange={set} error={errs.relationship} /></Col>
        <Col md={6}><Field label="Status" name="status" type="select" options={ENUMS.BorrowerContactStatus} value={v.status} onChange={set} error={errs.status} /></Col>
      </Row>)}
    </FormModal>

    <ConfirmDialog show={!!del} title="Delete borrower contact" variant="danger" confirmLabel="Delete"
      body={<>Delete contact <strong>{del?.name}</strong> for account {del?.accountId}?</>}
      onCancel={() => setDel(null)} onConfirm={doDelete} />
  </>);
}
