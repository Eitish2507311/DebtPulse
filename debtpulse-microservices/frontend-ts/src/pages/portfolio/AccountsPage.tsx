import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Row, Col, Form, Modal } from 'react-bootstrap';
import { accountApi } from '../../api/services';
import { usePaged } from '../../hooks/usePaged';
import { useAuth } from '../../auth/AuthContext';
import { PageHeader, ErrorNote } from '../../components/ui';
import DataTable from '../../components/DataTable';
import FormModal from '../../components/FormModal';
import Field from '../../components/Field';
import { ENUMS, bucketClass, statusClass } from '../../utils/enums';
import { inr, num, titleCase } from '../../utils/format';
import type { Account, Column } from '../../types';

export default function AccountsPage() {
  const navigate = useNavigate();
  const { role } = useAuth();
  const canWrite = role === 'ADMIN' || role === 'COLLECTIONS_AGENT';
  const [filters, setFilters] = useState<Record<string, string>>({});
  const [showCreate, setShowCreate] = useState(false);
  const [acctId, setAcctId] = useState('');
  const openAccount = () => { const v = acctId.trim(); if (v) navigate(`/portfolio/${v}`); };

  interface ImportResult { importedCount: number; errorCount: number; errors?: string[] }
  const [showImport, setShowImport] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [importResult, setImportResult] = useState<ImportResult | null>(null);
  const [importing, setImporting] = useState(false);
  const closeImport = () => { setShowImport(false); setImportResult(null); setFile(null); };
  const doImport = async () => {
    if (!file) return;
    setImporting(true);
    try { const { data } = await accountApi.importCsv(file); setImportResult(data as ImportResult); reload(); }
    catch { setImportResult({ importedCount: 0, errorCount: 1, errors: ['Import failed — check the file and try again.'] }); }
    finally { setImporting(false); }
  };

  const fetcher = useCallback((p: Record<string, unknown>) => accountApi.list(p), []);
  const { page, loading, error, setPage, reload } = usePaged<Account>(fetcher, { filters });

  const cleanFilters = (next: Record<string, string>) => {
    const f = { ...filters, ...next };
    Object.keys(f).forEach((k) => { if (!f[k]) delete f[k]; });
    setFilters(f);
  };

  const columns: Column<Account>[] = [
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
          <div className="d-flex gap-1">
            <Form.Control size="sm" placeholder="ACC-…" value={acctId}
              onChange={(e) => setAcctId(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') openAccount(); }} />
            <Button size="sm" variant="outline-primary" onClick={openAccount}>Open</Button>
          </div>
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
      <DataTable<Account> columns={columns} page={page} loading={loading} onPageChange={setPage}
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

      <Modal show={showImport} onHide={closeImport} centered>
        <Modal.Header closeButton><Modal.Title>Bulk import accounts (CSV)</Modal.Title></Modal.Header>
        <Modal.Body>
          <p className="small text-muted mb-2">
            Columns (header row required): <code>loanRef, borrowerName, phone, address, principal, overdue, dpd, [branchId]</code>.
          </p>
          <Form.Control type="file" accept=".csv,text/csv"
            onChange={(e) => setFile((e.target as HTMLInputElement).files?.[0] || null)} />
          {importResult && (
            <div className="mt-3 small">
              <div className="text-success fw-semibold">Imported: {importResult.importedCount}</div>
              {importResult.errorCount > 0 && <div className="text-danger fw-semibold">Errors: {importResult.errorCount}</div>}
              {(importResult.errors || []).slice(0, 6).map((er, i) => <div key={i} className="text-muted">• {er}</div>)}
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
