import { useState, useCallback } from 'react';
import { Row, Col, Card, Button } from 'react-bootstrap';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, PieChart, Pie, Cell, Legend } from 'recharts';
import { analyticsApi } from '../../api/services';
import { usePaged, useAsync } from '../../hooks/usePaged';
import { PageHeader, StatCard, Loading, ErrorNote } from '../../components/ui';
import DataTable from '../../components/DataTable';
import { useToast } from '../../components/ToastHost';
import { inr, num, pct, dateTime } from '../../utils/format';
import type { RecoveryReport, Column } from '../../types';

type Blob = Record<string, any>;
const PIE = ['#1d6fb8', '#0aa2c0', '#b8860b', '#1a7f4b', '#b42318', '#6b7280', '#13315c'];

export default function AnalyticsPage() {
  const toast = useToast();
  const dash = useAsync<Blob>(() => analyticsApi.dashboard(), []);
  const buckets = useAsync<Blob>(() => analyticsApi.bucketDistribution(), []);
  const ptp = useAsync<Blob>(() => analyticsApi.ptpMetrics(), []);
  const settle = useAsync<Blob>(() => analyticsApi.settlementMetrics(), []);
  const reportsFetcher = useCallback((p: Record<string, unknown>) => analyticsApi.listReports(p), []);
  const reports = usePaged<RecoveryReport>(reportsFetcher, { size: 10 });
  const [gen, setGen] = useState(false);

  const generate = async () => {
    setGen(true);
    try { await analyticsApi.generateReport('Branch'); toast.success('Report generated'); reports.reload(); }
    catch { toast.error('Could not generate report'); } finally { setGen(false); }
  };

  const d: Blob = dash.data || {};
  const bucketData = buckets.data?.byBucket
    ? Object.entries(buckets.data.byBucket as Record<string, number>).map(([name, value]) => ({ name, value: Number(value) })) : [];
  const settleData = settle.data ? [
    { name: 'Approved', value: Number(settle.data.approvedSettlements || 0) },
    { name: 'Rejected', value: Number(settle.data.rejectedSettlements || 0) },
    { name: 'Paid', value: Number(settle.data.paidSettlements || 0) },
    { name: 'Pending', value: Number(settle.data.pendingSettlements || 0) },
  ].filter((x) => x.value > 0) : [];

  const reportCols: Column<RecoveryReport>[] = [
    { key: 'reportId', header: 'Report', render: (r) => <span className="text-mono">{r.reportId}</span> },
    { key: 'scope', header: 'Scope' },
    { key: 'generatedDate', header: 'Generated', render: (r) => dateTime(r.generatedDate) },
  ];

  return (
    <>
      <PageHeader title="Recovery Analytics" subtitle="Portfolio performance, recovery rates and bucket ageing" icon="graph-up"
        actions={<Button onClick={generate} disabled={gen}><i className="bi bi-file-earmark-bar-graph me-1" />{gen ? 'Generating…' : 'Generate report'}</Button>} />

      <ErrorNote error={dash.error} />
      {dash.loading ? <Loading /> : (
        <Row className="g-3 mb-3">
          <Col md={6} xl={3}><StatCard label="Accounts Managed" value={num(d.accountsManaged ?? d.totalAccounts)} icon="folder2-open" tone="navy" /></Col>
          <Col md={6} xl={3}><StatCard label="Total Overdue" value={inr(d.totalOverdue)} icon="cash-stack" tone="red" /></Col>
          <Col md={6} xl={3}><StatCard label="Cash Collected" value={inr(d.cashCollected)} icon="wallet2" tone="green" /></Col>
          <Col md={6} xl={3}><StatCard label="Recovery Rate" value={pct(d.recoveryRate)} icon="graph-up-arrow" tone="blue" /></Col>
          <Col md={6} xl={3}><StatCard label="PTP Breach Rate" value={pct(d.ptpBreachRate)} icon="exclamation-triangle" tone="amber" /></Col>
          <Col md={6} xl={3}><StatCard label="Settlements Approved" value={num(d.settlementsApproved)} icon="check2-circle" tone="green" /></Col>
          <Col md={6} xl={3}><StatCard label="Legal Conversion" value={pct(d.legalConversionRate)} icon="bank" tone="navy" /></Col>
          <Col md={6} xl={3}><StatCard label="Field Visit Success" value={pct(d.fieldVisitSuccessRate)} icon="geo-alt" tone="blue" /></Col>
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

      <div className="fw-semibold mb-2" style={{ color: 'var(--dp-navy)' }}>Generated Reports</div>
      <DataTable<RecoveryReport> columns={reportCols} page={reports.page} loading={reports.loading} onPageChange={reports.setPage}
        emptyIcon="file-earmark-bar-graph" emptyTitle="No reports generated yet" />
    </>
  );
}
