import { useState, useCallback } from 'react';
import { Row, Col, Card, Button, Modal, Form, InputGroup, Table } from 'react-bootstrap';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, PieChart, Pie, Cell, Legend } from 'recharts';
import { analyticsApi } from '../../api/services.js';
import { usePaged, useAsync } from '../../hooks/usePaged.js';
import { PageHeader, StatCard, Loading, ErrorNote } from '../../components/ui.jsx';
import DataTable from '../../components/DataTable.jsx';
import { useToast } from '../../components/ToastHost.jsx';
import { inr, num, pct, dateTime, titleCase } from '../../utils/format.js';

const PIE = ['#1d6fb8', '#0aa2c0', '#b8860b', '#1a7f4b', '#b42318', '#6b7280', '#13315c'];

/** Renders one report's persisted metrics snapshot (nested JSON) as readable sections. */
function ReportView({ report, onClose }) {
  let metrics = null;
  try { metrics = report ? JSON.parse(report.metrics || '{}') : null; } catch { metrics = null; }
  return (
    <Modal show={!!report} onHide={onClose} centered size="lg" scrollable>
      <Modal.Header closeButton><Modal.Title className="h6 mb-0">Report {report?.reportId}</Modal.Title></Modal.Header>
      <Modal.Body>
        <div className="d-flex gap-4 mb-3 small">
          <div><span className="text-muted d-block">Scope</span><span className="fw-semibold">{report?.scope}</span></div>
          <div><span className="text-muted d-block">Generated</span><span className="fw-semibold">{dateTime(report?.generatedDate)}</span></div>
        </div>
        {!metrics ? <div className="text-muted">No metrics captured for this report.</div>
          : Object.entries(metrics).map(([section, val]) => (
            <div key={section} className="mb-3">
              <div className="fw-semibold text-capitalize mb-1">{titleCase(section)}</div>
              {val && typeof val === 'object' ? (
                <Table size="sm" borderless className="mb-0">
                  <tbody>
                    {Object.entries(val).map(([k, v]) => (
                      <tr key={k}>
                        <td className="text-muted" style={{ width: '50%' }}>{titleCase(k)}</td>
                        <td className="text-end fw-semibold">{typeof v === 'object' ? JSON.stringify(v) : String(v)}</td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              ) : <div className="fw-semibold">{String(val)}</div>}
            </div>
          ))}
      </Modal.Body>
      <Modal.Footer><Button variant="light" onClick={onClose}>Close</Button></Modal.Footer>
    </Modal>
  );
}

export default function AnalyticsPage() {
  const toast = useToast();
  const dash = useAsync(() => analyticsApi.dashboard(), []);
  const buckets = useAsync(() => analyticsApi.bucketDistribution(), []);
  const ptp = useAsync(() => analyticsApi.ptpMetrics(), []);
  const settle = useAsync(() => analyticsApi.settlementMetrics(), []);

  const [scope, setScope] = useState('');
  const reportFilters = scope ? { scope } : {};
  const reportsFetcher = useCallback((p) => analyticsApi.listReports(p), []);
  const reports = usePaged(reportsFetcher, { size: 10, filters: reportFilters });
  const [gen, setGen] = useState(false);
  const [rq, setRq] = useState('');
  const [view, setView] = useState(null);

  const refreshAll = () => { dash.reload(); buckets.reload(); ptp.reload(); settle.reload(); reports.reload(); };

  const generate = async () => {
    setGen(true);
    try { await analyticsApi.generateReport('Branch'); toast.success('Report generated'); reports.reload(); }
    catch { toast.error('Could not generate report'); } finally { setGen(false); }
  };

  // The dashboard endpoint returns a NESTED aggregate — read each KPI from its section.
  const d = dash.data || {};
  const portfolio = d.portfolio || {};
  const recovery = d.recoveryRate || {};
  const settlements = d.settlements || {};
  const legal = d.legal || {};
  const field = d.fieldVisits || {};
  const ptpBreachPct = ptp.data?.ptpBreachRate != null ? Number(ptp.data.ptpBreachRate) * 100 : null;

  const bucketData = buckets.data?.byBucket
    ? Object.entries(buckets.data.byBucket).map(([name, value]) => ({ name, value: Number(value) })) : [];
  const settleData = settle.data ? [
    { name: 'Approved', value: Number(settle.data.approvedSettlements || 0) },
    { name: 'Rejected', value: Number(settle.data.rejectedSettlements || 0) },
    { name: 'Paid', value: Number(settle.data.paidSettlements || 0) },
    { name: 'Pending', value: Number(settle.data.pendingSettlements || 0) },
  ].filter((x) => x.value > 0) : [];

  const reportCols = [
    { key: 'reportId', header: 'Report', render: (r) => <span className="text-mono">{r.reportId}</span> },
    { key: 'scope', header: 'Scope' },
    { key: 'generatedDate', header: 'Generated', render: (r) => dateTime(r.generatedDate) },
    { key: '_a', header: '', render: (r) => <Button size="sm" variant="light" onClick={() => setView(r)}><i className="bi bi-eye" /></Button> },
  ];
  // Client-side Report-ID search over the loaded page.
  const shownReports = reports.page
    ? { ...reports.page, content: (reports.page.content || []).filter((r) => !rq.trim() || (r.reportId || '').toLowerCase().includes(rq.trim().toLowerCase())) }
    : reports.page;

  return (
    <>
      <PageHeader title="Recovery Analytics" subtitle="Portfolio performance, recovery rates and bucket ageing" icon="graph-up"
        actions={<>
          <Button variant="outline-secondary" onClick={refreshAll}><i className="bi bi-arrow-clockwise me-1" />Refresh</Button>
          <Button onClick={generate} disabled={gen}><i className="bi bi-file-earmark-bar-graph me-1" />{gen ? 'Generating…' : 'Generate report'}</Button>
        </>} />

      <ErrorNote error={dash.error} />
      {dash.loading ? <Loading /> : (
        <Row className="g-3 mb-3">
          <Col md={6} xl={3}><StatCard label="Accounts Managed" value={num(portfolio.totalAccounts)} icon="folder2-open" tone="navy" /></Col>
          <Col md={6} xl={3}><StatCard label="Total Overdue" value={inr(portfolio.totalOverdue)} icon="cash-stack" tone="red" /></Col>
          <Col md={6} xl={3}><StatCard label="Settled Accounts" value={num(portfolio.settledAccounts)} icon="check2-circle" tone="green" /></Col>
          <Col md={6} xl={3}><StatCard label="Recovery Rate" value={pct(recovery.recoveryRatePercent)} icon="graph-up-arrow" tone="blue" /></Col>
          <Col md={6} xl={3}><StatCard label="PTP Breach Rate" value={pct(ptpBreachPct)} icon="exclamation-triangle" tone="amber" /></Col>
          <Col md={6} xl={3}><StatCard label="Settlements Approved" value={num(settlements.approvedSettlements)} icon="check2-circle" tone="green" /></Col>
          <Col md={6} xl={3}><StatCard label="Legal Conversion" value={pct(legal.legalConversionRate)} icon="bank" tone="navy" /></Col>
          <Col md={6} xl={3}><StatCard label="Field Visit Success" value={pct(field.fieldVisitSuccessRate)} icon="geo-alt" tone="blue" /></Col>
        </Row>
      )}

      <Row className="g-3 mb-3">
        <Col lg={7}><Card className="h-100"><Card.Header>Bucket Ageing Distribution</Card.Header>
          <Card.Body style={{ height: 320 }}>
            {bucketData.length ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={bucketData}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="name" fontSize={12} /><YAxis fontSize={12} allowDecimals={false} /><Tooltip />
                  <Bar dataKey="value" fill="#1d6fb8" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : <div className="text-muted text-center pt-5">No data</div>}
          </Card.Body></Card></Col>
        <Col lg={5}><Card className="h-100"><Card.Header>Settlement Mix</Card.Header>
          <Card.Body style={{ height: 320 }}>
            {settleData.length ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={settleData} dataKey="value" nameKey="name" outerRadius={95} label>
                    {settleData.map((_, i) => <Cell key={i} fill={PIE[i % PIE.length]} />)}
                  </Pie><Tooltip /><Legend />
                </PieChart>
              </ResponsiveContainer>
            ) : <div className="text-muted text-center pt-5">No settlement data</div>}
          </Card.Body></Card></Col>
      </Row>

      <Row className="g-3 mb-3">
        <Col md={4}><StatCard label="Total PTPs" value={num(ptp.data?.totalPtp)} icon="hand-thumbs-up" tone="blue" /></Col>
        <Col md={4}><StatCard label="PTPs Kept" value={num(ptp.data?.keptPtp)} icon="check-circle" tone="green" /></Col>
        <Col md={4}><StatCard label="PTPs Broken" value={num(ptp.data?.brokenPtp)} icon="x-circle" tone="red" /></Col>
      </Row>

      <div className="d-flex justify-content-between align-items-center gap-2 mb-2 flex-wrap">
        <div className="fw-semibold" style={{ color: 'var(--dp-navy)' }}>Generated Reports</div>
        <div className="d-flex gap-2 align-items-center flex-wrap filter-bar">
          <InputGroup size="sm" style={{ width: 240 }}>
            <InputGroup.Text><i className="bi bi-search" /></InputGroup.Text>
            <Form.Control placeholder="Search by Report ID…" value={rq} onChange={(e) => setRq(e.target.value)} />
            {rq && <Button variant="outline-secondary" onClick={() => setRq('')}><i className="bi bi-x" /></Button>}
          </InputGroup>
          <Form.Control size="sm" style={{ width: 170 }} placeholder="Filter by scope…"
            value={scope} onChange={(e) => setScope(e.target.value)} />
        </div>
      </div>
      <DataTable columns={reportCols} page={shownReports} loading={reports.loading} onPageChange={reports.setPage}
        onRowClick={(r) => setView(r)}
        emptyIcon="file-earmark-bar-graph" emptyTitle="No reports generated yet" />

      <ReportView report={view} onClose={() => setView(null)} />
    </>
  );
}
