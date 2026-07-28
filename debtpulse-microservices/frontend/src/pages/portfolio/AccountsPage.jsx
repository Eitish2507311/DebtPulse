import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Row, Col, Form } from 'react-bootstrap';
import { accountApi } from '../../api/services.js';
import { usePaged } from '../../hooks/usePaged.js';
import { useAuth } from '../../auth/AuthContext.jsx';
import { ROLES } from '../../auth/roles.js';
import { PageHeader, ErrorNote } from '../../components/ui.jsx';
import DataTable from '../../components/DataTable.jsx';
import FormModal from '../../components/FormModal.jsx';
import Field from '../../components/Field.jsx';
import { ENUMS, bucketClass, statusClass } from '../../utils/enums.js';
import { inr, num, titleCase } from '../../utils/format.js';

export default function AccountsPage() {
  const navigate = useNavigate();
  const { role } = useAuth();
  const canWrite = [ROLES.ADMIN, ROLES.COLLECTIONS_AGENT].includes(role);
  const [filters, setFilters] = useState({});
  const [showCreate, setShowCreate] = useState(false);

  const fetcher = useCallback((p) => accountApi.list(p), []);
  const { page, loading, error, setPage, reload } = usePaged(fetcher, { filters });

  const cleanFilters = (next) => {
    const f = { ...filters, ...next };
    Object.keys(f).forEach((k) => !f[k] && delete f[k]);
    setFilters(f);
  };

  const columns = [
    { key: 'accountId', header: 'Account', render: (r) => <span className="text-mono">{r.accountId}</span> },
    { key: 'loanRef', header: 'Loan Ref', render: (r) => <span className="text-mono">{r.loanRef}</span> },
    { key: 'borrowerName', header: 'Borrower', render: (r) => <><div className="fw-semibold">{r.borrowerName}</div><small className="text-muted">{r.phone || '—'}</small></> },
    { key: 'principalAmount', header: 'Principal', className: 'text-end', render: (r) => inr(r.principalAmount) },
    { key: 'totalOverdue', header: 'Overdue', className: 'text-end', render: (r) => <span className="text-danger fw-semibold">{inr(r.totalOverdue)}</span> },
    { key: 'dpd', header: 'DPD', className: 'text-center', render: (r) => num(r.dpd) },
    { key: 'bucket', header: 'Bucket', render: (r) => <span className={`badge-pill ${bucketClass(r.bucket)}`}>{r.bucket}</span> },
    { key: 'status', header: 'Status', render: (r) => <span className={`badge-pill ${statusClass(r.status)}`}>{titleCase(r.status)}</span> },
  ];

  return (
    <>
      <PageHeader title="Delinquent Portfolio" subtitle="Manage delinquent accounts across collection buckets" icon="folder2-open"
        actions={canWrite && <Button onClick={() => setShowCreate(true)}><i className="bi bi-plus-lg me-1" />New Account</Button>} />

      <Row className="g-2 mb-3">
        <Col sm={4} md={3}>
          <Form.Select size="sm" value={filters.bucket || ''} onChange={(e) => cleanFilters({ bucket: e.target.value })}>
            <option value="">All buckets</option>
            {ENUMS.DpdBucket.map((b) => <option key={b} value={b}>{b}</option>)}
          </Form.Select>
        </Col>
        <Col sm={4} md={3}>
          <Form.Select size="sm" value={filters.status || ''} onChange={(e) => cleanFilters({ status: e.target.value })}>
            <option value="">All statuses</option>
            {ENUMS.AccountStatus.map((s) => <option key={s} value={s}>{titleCase(s)}</option>)}
          </Form.Select>
        </Col>
      </Row>

      <ErrorNote error={error} />
      <DataTable columns={columns} page={page} loading={loading} onPageChange={setPage}
        onRowClick={(r) => navigate(`/portfolio/${r.accountId}`)}
        emptyIcon="folder2-open" emptyTitle="No accounts" emptyMessage="Create an account to get started." />

      <FormModal show={showCreate} title="New Delinquent Account" submitLabel="Create account"
        initial={{ loanRef: '', borrowerName: '', phone: '', address: '', branchId: '', principalAmount: '', totalOverdue: '', dpd: '' }}
        onClose={() => setShowCreate(false)} onSaved={reload}
        onSubmit={(v) => accountApi.create({
          ...v, principalAmount: Number(v.principalAmount), totalOverdue: Number(v.totalOverdue), dpd: Number(v.dpd),
        })}>
        {(v, set, errs) => (
          <Row>
            <Col md={6}><Field label="Loan Reference" name="loanRef" value={v.loanRef} onChange={set} error={errs.loanRef} required /></Col>
            <Col md={6}><Field label="Borrower Name" name="borrowerName" value={v.borrowerName} onChange={set} error={errs.borrowerName} required /></Col>
            <Col md={6}><Field label="Phone" name="phone" value={v.phone} onChange={set} error={errs.phone} help="10 digits" /></Col>
            <Col md={6}><Field label="Branch ID" name="branchId" value={v.branchId} onChange={set} error={errs.branchId} /></Col>
            <Col md={12}><Field label="Address" name="address" value={v.address} onChange={set} error={errs.address} /></Col>
            <Col md={4}><Field label="Principal" name="principalAmount" type="number" min="0" value={v.principalAmount} onChange={set} error={errs.principalAmount} required /></Col>
            <Col md={4}><Field label="Total Overdue" name="totalOverdue" type="number" min="0" value={v.totalOverdue} onChange={set} error={errs.totalOverdue} required /></Col>
            <Col md={4}><Field label="DPD" name="dpd" type="number" min="0" value={v.dpd} onChange={set} error={errs.dpd} help="Bucket is derived" required /></Col>
          </Row>
        )}
      </FormModal>
    </>
  );
}
