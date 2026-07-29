import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Row, Col, Card } from 'react-bootstrap';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, CartesianGrid } from 'recharts';
import { useAuth } from '../auth/AuthContext';
import { ROLE_LABELS, ACCESS, hasAny } from '../auth/roles';
import { analyticsApi, notificationApi } from '../api/services';
import { StatCard, PageHeader, Loading } from '../components/ui';
import { inr, num, pct, titleCase } from '../utils/format';
import type { Notification, Role } from '../types';

// The analytics maps are dynamic (backend returns Map<String,Object>), so we read them loosely.
type Blob = Record<string, any>;

const QUICK = [
  { to: '/portfolio', key: 'portfolio', icon: 'folder2-open', label: 'Delinquent Portfolio', desc: 'Accounts & buckets' },
  { to: '/contacts', key: 'contact', icon: 'telephone', label: 'Contact & PTP', desc: 'Calls & promises' },
  { to: '/field', key: 'field', icon: 'geo-alt', label: 'Field Recovery', desc: 'Visits & assets' },
  { to: '/settlements', key: 'settlement', icon: 'cash-coin', label: 'Settlements', desc: 'Offers & approvals' },
  { to: '/legal', key: 'legal', icon: 'bank', label: 'Legal', desc: 'Cases & orders' },
  { to: '/analytics', key: 'analytics', icon: 'graph-up', label: 'Analytics', desc: 'Recovery insights' },
];

const PIE = ['#1d6fb8', '#0aa2c0', '#b8860b', '#1a7f4b', '#b42318', '#6b7280', '#13315c'];

export default function DashboardPage() {
  const { user, role } = useAuth();
  // Admin, Portfolio Manager AND Collections Agent all see the same org KPI dashboard (kept in
  // sync across roles). Field/Settlement/Legal officers work from their own module views instead.
  const showKpis = (['ADMIN', 'PORTFOLIO_MANAGER', 'COLLECTIONS_AGENT'] as Role[]).includes(role!);
  const [dash, setDash] = useState<Blob | null>(null);
  const [buckets, setBuckets] = useState<Blob | null>(null);
  const [notes, setNotes] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let alive = true;
    const jobs: Promise<unknown>[] = [
      notificationApi.list({ page: 0, size: 5 }).then((r) => { if (alive) setNotes(r.data.content || []); }).catch(() => {}),
    ];
    if (showKpis) {
      jobs.push(analyticsApi.dashboard().then((r) => { if (alive) setDash(r.data as Blob); }).catch(() => {}));
      jobs.push(analyticsApi.bucketDistribution().then((r) => { if (alive) setBuckets(r.data as Blob); }).catch(() => {}));
    }
    Promise.all(jobs).finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, [showKpis]);

  const d: Blob = dash || {};
  const bucketData = buckets?.byBucket
    ? Object.entries(buckets.byBucket as Record<string, number>).map(([name, value]) => ({ name, value: Number(value) })) : [];

  return (
    <>
      <PageHeader
        title={`Welcome, ${user?.name?.split(' ')[0] || 'there'}`}
        subtitle={`${role ? ROLE_LABELS[role] : ''} · Branch ${user?.branchId || '—'}`}
        icon="speedometer2"
      />

      {showKpis && (loading ? <Loading /> : (
        <>
          <Row className="g-3 mb-3">
            <Col md={6} xl={3}><StatCard label="Accounts Managed" value={num(d.accountsManaged ?? d.totalAccounts)} icon="folder2-open" tone="navy" /></Col>
            <Col md={6} xl={3}><StatCard label="Total Overdue" value={inr(d.totalOverdue)} icon="cash-stack" tone="red" /></Col>
            <Col md={6} xl={3}><StatCard label="Cash Collected" value={inr(d.cashCollected)} icon="wallet2" tone="green" /></Col>
            <Col md={6} xl={3}><StatCard label="Recovery Rate" value={pct(d.recoveryRate)} icon="graph-up-arrow" tone="blue" /></Col>
          </Row>
          <Row className="g-3 mb-3">
            <Col md={6} xl={3}><StatCard label="PTP Breach Rate" value={pct(d.ptpBreachRate)} icon="exclamation-triangle" tone="amber" /></Col>
            <Col md={6} xl={3}><StatCard label="Settlements Approved" value={num(d.settlementsApproved)} icon="check2-circle" tone="green" /></Col>
            <Col md={6} xl={3}><StatCard label="Legal Conversion" value={pct(d.legalConversionRate)} icon="bank" tone="navy" /></Col>
            <Col md={6} xl={3}><StatCard label="Field Visit Success" value={pct(d.fieldVisitSuccessRate)} icon="geo-alt" tone="blue" /></Col>
          </Row>
          <Row className="g-3 mb-3">
            <Col lg={7}>
              <Card className="h-100"><Card.Header>Bucket Distribution</Card.Header>
                <Card.Body style={{ height: 300 }}>
                  {bucketData.length ? (
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={bucketData}>
                        <CartesianGrid strokeDasharray="3 3" vertical={false} />
                        <XAxis dataKey="name" fontSize={12} /><YAxis fontSize={12} allowDecimals={false} />
                        <Tooltip /><Bar dataKey="value" fill="#1d6fb8" radius={[4, 4, 0, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                  ) : <div className="text-muted text-center pt-5">No bucket data yet</div>}
                </Card.Body>
              </Card>
            </Col>
            <Col lg={5}>
              <Card className="h-100"><Card.Header>Portfolio by Bucket</Card.Header>
                <Card.Body style={{ height: 300 }}>
                  {bucketData.length ? (
                    <ResponsiveContainer width="100%" height="100%">
                      <PieChart>
                        <Pie data={bucketData} dataKey="value" nameKey="name" outerRadius={100} label>
                          {bucketData.map((_, i) => <Cell key={i} fill={PIE[i % PIE.length]} />)}
                        </Pie>
                        <Tooltip />
                      </PieChart>
                    </ResponsiveContainer>
                  ) : <div className="text-muted text-center pt-5">No data</div>}
                </Card.Body>
              </Card>
            </Col>
          </Row>
        </>
      ))}

      <Row className="g-3">
        <Col lg={8}>
          <div className="fw-semibold mb-2" style={{ color: 'var(--dp-navy)' }}>Quick actions</div>
          <Row className="g-3">
            {QUICK.filter((q) => hasAny(role, ACCESS[q.key])).map((q) => (
              <Col sm={6} md={4} key={q.to}>
                <Link to={q.to} className="text-decoration-none">
                  <Card className="h-100 hover-lift"><Card.Body className="d-flex align-items-center gap-3">
                    <div className="d-flex align-items-center justify-content-center"
                         style={{ width: 44, height: 44, borderRadius: 10, background: '#e8f1fb', color: 'var(--dp-blue-600)' }}>
                      <i className={`bi bi-${q.icon} fs-5`} />
                    </div>
                    <div><div className="fw-semibold" style={{ color: 'var(--dp-navy)' }}>{q.label}</div>
                      <small className="text-muted">{q.desc}</small></div>
                  </Card.Body></Card>
                </Link>
              </Col>
            ))}
          </Row>
        </Col>
        <Col lg={4}>
          <Card className="h-100">
            <Card.Header className="d-flex justify-content-between align-items-center">
              Recent Notifications <Link to="/notifications" className="small">View all</Link>
            </Card.Header>
            <Card.Body className="p-0">
              {notes.length === 0 ? <div className="text-muted text-center py-4">No notifications</div> : (
                <ul className="list-group list-group-flush">
                  {notes.map((n) => (
                    <li key={n.notificationId} className="list-group-item d-flex gap-2">
                      <i className="bi bi-bell text-primary mt-1" />
                      <div><div className="small">{n.message}</div>
                        <small className="text-muted">{titleCase(n.category)}</small></div>
                    </li>
                  ))}
                </ul>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </>
  );
}
