import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Row, Col, Form, Modal } from 'react-bootstrap';
import { accountApi } from '../../api/services.js';
import { usePaged } from '../../hooks/usePaged.js';
import { useAuth } from '../../auth/AuthContext.jsx';
import { ROLES } from '../../auth/roles.js';
import { PageHeader, ErrorNote } from '../../components/ui.jsx';
import DataTable from '../../components/DataTable.jsx';
import FormModal from '../../components/FormModal.jsx';
import Field from '../../components/Field.jsx';
import SearchableSelect from '../../components/SearchableSelect.jsx';
import { ENUMS, bucketClass, statusClass } from '../../utils/enums.js';
import { inr, num, titleCase } from '../../utils/format.js';

export default function AccountsPage() {
  const navigate = useNavigate();
  const { role } = useAuth();
  const canWrite = [ROLES.ADMIN, ROLES.COLLECTIONS_AGENT].includes(role);
  const [filters, setFilters] = useState({});
  const [showCreate, setShowCreate] = useState(false);
  const [acctId, setAcctId] = useState('');

  const [showImport, setShowImport] = useState(false);
  const [file, setFile] = useState(null);
  const [importResult, setImportResult] = useState(null);
  const [importing, setImporting] = useState(false);
  const closeImport = () => { setShowImport(false); setImportResult(null); setFile(null); };
  const doImport = async () => {
    if (!file) return;
    setImporting(true);
    try { const { data } = await accountApi.importCsv(file); setImportResult(data); reload(); }
    catch { setImportResult({ importedCount: 0, errorCount: 1, errors: ['Import failed — check the file and try again.'] }); }
    finally { setImporting(false); }
  };

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
        actions={canWrite && <>
          <Button variant="outline-primary" onClick={() => setShowImport(true)}><i className="bi bi-upload me-1" />Import CSV</Button>
          <Button onClick={() => setShowCreate(true)}><i className="bi bi-plus-lg me-1" />New Account</Button>
        </>} />

      <Row className="g-2 mb-3 align-items-end">
        <Col sm={6} md={3}>
          <Form.Label className="small text-muted mb-1">Open by Account ID</Form.Label>
          <SearchableSelect placeholder="Type or pick an Account ID…" value={acctId}
            loadOptions={async () => {
              const { data } = await accountApi.list({ page: 0, size: 200 });
              return (data.content || []).map((a) => ({ value: a.accountId, label: a.borrowerName }));
            }}
            onChange={(v) => { setAcctId(v); navigate(`/portfolio/${v}`); }} />
        </Col>
        <Col sm={6} md={2}>
          <Form.Label className="small text-muted mb-1">Agent ID</Form.Label>
          <Form.Control size="sm" placeholder="USR-…" value={filters.agentId || ''}
            onChange={(e) => cleanFilters({ agentId: e.target.value })} />
        </Col>
        <Col sm={4} md={2}>
          <Form.Label className="small text-muted mb-1">Bucket</Form.Label>
          <Form.Select size="sm" value={filters.bucket || ''} onChange={(e) => cleanFilters({ bucket: e.target.value })}>
            <option value="">All buckets</option>
            {ENUMS.DpdBucket.map((b) => <option key={b} value={b}>{b}</option>)}
          </Form.Select>
        </Col>
        <Col sm={4} md={2}>
          <Form.Label className="small text-muted mb-1">Status</Form.Label>
          <Form.Select size="sm" value={filters.status || ''} onChange={(e) => cleanFilters({ status: e.target.value })}>
            <option value="">All statuses</option>
            {ENUMS.AccountStatus.map((s) => <option key={s} value={s}>{titleCase(s)}</option>)}
          </Form.Select>
        </Col>
        <Col sm={6} md={3}>
          <Form.Label className="small text-muted mb-1">DPD range</Form.Label>
          <div className="d-flex gap-1 align-items-center">
            <Form.Control size="sm" type="number" min="0" placeholder="min" value={filters.dpdMin || ''}
              onChange={(e) => cleanFilters({ dpdMin: e.target.value })} />
            <span className="text-muted">–</span>
            <Form.Control size="sm" type="number" min="0" placeholder="max" value={filters.dpdMax || ''}
              onChange={(e) => cleanFilters({ dpdMax: e.target.value })} />
          </div>
        </Col>
      </Row>

      <ErrorNote error={error} />
      <DataTable columns={columns} page={page} loading={loading} onPageChange={setPage}
        onRowClick={(r) => navigate(`/portfolio/${r.accountId}`)}
        emptyIcon="folder2-open" emptyTitle="No accounts" emptyMessage="Create an account to get started." />

      <FormModal show={showCreate} title="New Delinquent Account" submitLabel="Create account"
        initial={{ loanRef: '', borrowerName: '', phone: '', address: '', branchId: '', principalAmount: '', totalOverdue: '', dpd: '',
          secured: 'false', assetType: '', assetDescription: '', estimatedValue: '', lastVerifiedDate: '' }}
        onClose={() => setShowCreate(false)} onSaved={reload}
        onSubmit={(v) => {
          const isSecured = v.secured === 'true';
          return accountApi.create({
            loanRef: v.loanRef, borrowerName: v.borrowerName, phone: v.phone, address: v.address, branchId: v.branchId,
            principalAmount: Number(v.principalAmount), totalOverdue: Number(v.totalOverdue), dpd: Number(v.dpd),
            secured: isSecured,
            assetType: isSecured ? (v.assetType || null) : null,
            assetDescription: isSecured ? (v.assetDescription || null) : null,
            estimatedValue: isSecured && v.estimatedValue !== '' ? Number(v.estimatedValue) : null,
            lastVerifiedDate: isSecured ? (v.lastVerifiedDate || null) : null,
          });
        }}>
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
            <Col md={12}><hr className="my-1" /></Col>
            <Col md={4}><Field label="Loan Type" name="secured" type="select"
              options={[{ value: 'false', label: 'Unsecured' }, { value: 'true', label: 'Secured (has collateral)' }]}
              value={v.secured} onChange={set} blankLabel="Unsecured"
              help="Secured loans require a collateral asset" /></Col>
            {v.secured === 'true' && <>
              <Col md={4}><Field label="Asset Type" name="assetType" type="select" options={ENUMS.AssetType} value={v.assetType} onChange={set} error={errs.assetType} required /></Col>
              <Col md={4}><Field label="Estimated Value" name="estimatedValue" type="number" min="0" value={v.estimatedValue} onChange={set} error={errs.estimatedValue} required /></Col>
              <Col md={4}><Field label="Last Verified Date" name="lastVerifiedDate" type="date" value={v.lastVerifiedDate} onChange={set} error={errs.lastVerifiedDate} help="Origination appraisal date" /></Col>
              <Col md={12}><Field label="Asset Description" name="assetDescription" value={v.assetDescription} onChange={set} error={errs.assetDescription} /></Col>
            </>}
          </Row>
        )}
      </FormModal>

      <Modal show={showImport} onHide={closeImport} centered size="lg">
        <Modal.Header closeButton><Modal.Title>Bulk import accounts (CSV)</Modal.Title></Modal.Header>
        <Modal.Body>
          <p className="small text-muted mb-1">Columns (header row required):</p>
          <p className="small mb-2"><code>loanRef, borrowerName, phone, address, principal, overdue, dpd, branchId, secured, assetType, assetDescription, estimatedValue</code></p>
          <p className="small text-muted mb-2">
            The last five are optional. For a <strong>secured</strong> row set <code>secured=true</code> and provide
            <code> assetType</code> (PROPERTY/VEHICLE/GOLD/MACHINERY/STOCKS) and <code>estimatedValue</code>.
          </p>
          <Form.Control type="file" accept=".csv,text/csv" onChange={(e) => setFile(e.target.files?.[0] || null)} />
          {importResult && (
            <div className="mt-3 small">
              <div className="text-success fw-semibold">Imported: {importResult.importedCount}</div>
              {importResult.errorCount > 0 && <div className="text-danger fw-semibold mb-1">Errors: {importResult.errorCount}</div>}
              {importResult.errorCount > 0 && (
                <div className="border rounded p-2 bg-light" style={{ maxHeight: 220, overflowY: 'auto' }}>
                  {(importResult.errors || []).map((er, i) => <div key={i} className="text-danger">• {er}</div>)}
                </div>
              )}
            </div>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="light" onClick={closeImport}>Close</Button>
          <Button disabled={!file || importing} onClick={doImport}>{importing ? 'Importing…' : 'Import'}</Button>
        </Modal.Footer>
      </Modal>
    </>
  );
}
