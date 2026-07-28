import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Tabs, Tab, Button, Row, Col, Table } from 'react-bootstrap';
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
import type { LegalCase, RecoveryOrder, Column } from '../../types';

export default function LegalPage() {
  const { role } = useAuth();
  const canWrite = role === 'ADMIN' || role === 'LEGAL_OFFICER';
  return (
    <>
      <PageHeader title="Legal Proceedings" subtitle="Track court filings, hearings and recovery orders" icon="bank" />
      <Tabs defaultActiveKey="cases" className="mb-3">
        <Tab eventKey="cases" title="Cases"><CasesTab canWrite={canWrite} /></Tab>
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

  const columns: Column<LegalCase>[] = [
    { key: 'caseId', header: 'Case', render: (r) => <span className="text-mono">{r.caseId}</span> },
    { key: 'accountId', header: 'Account', render: (r) => <span className="text-mono">{r.accountId}</span> },
    { key: 'caseType', header: 'Type', render: (r) => titleCase(r.caseType) },
    { key: 'courtName', header: 'Court', render: (r) => r.courtName },
    { key: 'caseNumber', header: 'Case No.', render: (r) => r.caseNumber },
    { key: 'filingDate', header: 'Filed', render: (r) => date(r.filingDate) },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge value={r.status} /> },
  ];
  return (<>
    <div className="d-flex justify-content-end mb-2">
      {canWrite && <Button size="sm" onClick={() => setShow(true)}><i className="bi bi-plus-lg me-1" />File case</Button>}
    </div>
    <ErrorNote error={error} />
    <DataTable<LegalCase> columns={columns} page={page} loading={loading} onPageChange={setPage}
      onRowClick={(r) => navigate(`/legal/${r.caseId}`)} emptyIcon="bank" emptyTitle="No legal cases" />
    <FormModal show={show} title="File Legal Case" submitLabel="File case"
      initial={{ accountId: '', caseType: '', filingDate: today(), courtName: '', caseNumber: '' }}
      onClose={() => setShow(false)} onSaved={reload} onSubmit={(v) => legalApi.createCase(v)}>
      {(v, set, errs) => (<>
        <Row>
          <Col md={6}><Field label="Account ID" name="accountId" value={v.accountId} onChange={set} error={errs.accountId} required /></Col>
          <Col md={6}><Field label="Case Type" name="caseType" type="select" options={ENUMS.CaseType} value={v.caseType} onChange={set} error={errs.caseType} required /></Col>
          <Col md={6}><Field label="Court Name" name="courtName" value={v.courtName} onChange={set} error={errs.courtName} required /></Col>
          <Col md={6}><Field label="Case Number" name="caseNumber" value={v.caseNumber} onChange={set} error={errs.caseNumber} required /></Col>
          <Col md={6}><Field label="Filing Date" name="filingDate" type="date" value={v.filingDate} onChange={set} error={errs.filingDate} required /></Col>
        </Row>
      </>)}
    </FormModal>
  </>);
}

function OrdersTab({ canWrite }: { canWrite: boolean }) {
  const toast = useToast();
  const { data, loading, error, reload } = useAsync<RecoveryOrder[]>(() => legalApi.listOrders(), []);
  const [show, setShow] = useState(false);
  const [del, setDel] = useState<RecoveryOrder | null>(null);

  const doDelete = async () => {
    if (!del) return;
    try { await legalApi.deleteOrder(del.orderId); toast.success('Order deleted'); setDel(null); reload(); }
    catch { toast.error('Could not delete order'); }
  };

  if (loading) return <Loading />;
  return (<>
    <div className="d-flex justify-content-end mb-2">
      {canWrite && <Button size="sm" onClick={() => setShow(true)}><i className="bi bi-plus-lg me-1" />Issue order</Button>}
    </div>
    <ErrorNote error={error} />
    <div className="card">
      {!data?.length ? <div className="card-body"><EmptyState icon="file-earmark-text" title="No recovery orders" /></div> : (
        <Table hover className="mb-0"><thead><tr>
          <th>Order</th><th>Case</th><th>Type</th><th>Issued</th><th>Deadline</th><th>Status</th><th></th>
        </tr></thead><tbody>
          {data.map((o) => (
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
      {(v, set, errs) => (<>
        <Row>
          <Col md={6}><Field label="Case ID" name="caseId" value={v.caseId} onChange={set} error={errs.caseId} required /></Col>
          <Col md={6}><Field label="Order Type" name="orderType" type="select" options={ENUMS.OrderType} value={v.orderType} onChange={set} error={errs.orderType} required /></Col>
          <Col md={6}><Field label="Issued Date" name="issuedDate" type="date" value={v.issuedDate} onChange={set} error={errs.issuedDate} required /></Col>
          <Col md={6}><Field label="Execution Deadline" name="executionDeadline" type="date" value={v.executionDeadline} onChange={set} error={errs.executionDeadline} required /></Col>
        </Row>
      </>)}
    </FormModal>
    <ConfirmDialog show={!!del} title="Delete recovery order" variant="danger" confirmLabel="Delete"
      body={<>Delete order <strong>{del?.orderId}</strong>?</>} onCancel={() => setDel(null)} onConfirm={doDelete} />
  </>);
}
