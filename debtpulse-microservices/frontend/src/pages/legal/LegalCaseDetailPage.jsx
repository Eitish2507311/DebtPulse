import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Row, Col, Card, Button, Table } from 'react-bootstrap';
import { legalApi } from '../../api/services.js';
import { useAsync } from '../../hooks/usePaged.js';
import { useAuth } from '../../auth/AuthContext.jsx';
import { ROLES } from '../../auth/roles.js';
import { PageHeader, Loading, StatusBadge, EmptyState, ErrorNote } from '../../components/ui.jsx';
import FormModal from '../../components/FormModal.jsx';
import Field from '../../components/Field.jsx';
import { ENUMS } from '../../utils/enums.js';
import { date, titleCase, today } from '../../utils/format.js';

export default function LegalCaseDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { role } = useAuth();
  const canWrite = [ROLES.ADMIN, ROLES.LEGAL_OFFICER].includes(role);
  const { data: c, loading, error, reload } = useAsync(() => legalApi.getCase(id), [id]);
  const hearings = useAsync(() => legalApi.listHearings(id), [id]);
  const [edit, setEdit] = useState(false);
  const [addHearing, setAddHearing] = useState(false);

  if (loading) return <Loading />;
  if (error) return <ErrorNote error={error} />;
  if (!c) return null;

  const info = [
    ['Account', c.accountId], ['Case Type', titleCase(c.caseType)], ['Court', c.courtName],
    ['Case Number', c.caseNumber], ['Filing Date', date(c.filingDate)], ['Legal Officer', c.legalOfficerId],
  ];

  return (
    <>
      <Button variant="link" className="px-0 mb-2" onClick={() => navigate('/legal')}>
        <i className="bi bi-arrow-left me-1" />Back to legal
      </Button>
      <PageHeader title={c.caseNumber} subtitle={<span className="text-mono">{c.caseId}</span>} icon="bank"
        actions={<>
          <StatusBadge value={c.status} />
          {canWrite && <Button size="sm" variant="outline-primary" onClick={() => setEdit(true)}><i className="bi bi-pencil me-1" />Update case</Button>}
          {canWrite && <Button size="sm" onClick={() => setAddHearing(true)}><i className="bi bi-plus-lg me-1" />Add hearing</Button>}
        </>} />

      <Card className="mb-3"><Card.Body><Row>
        {info.map(([k, v]) => (
          <Col md={4} key={k} className="mb-3">
            <div className="text-muted small text-uppercase">{k}</div>
            <div className="fw-semibold">{v}</div>
          </Col>
        ))}
      </Row></Card.Body></Card>

      <Card>
        <Card.Header>Court Hearings</Card.Header>
        <Card.Body className="p-0">
          {hearings.loading ? <Loading /> : !hearings.data?.length ? <EmptyState icon="calendar-event" title="No hearings recorded" /> : (
            <Table hover className="mb-0"><thead><tr>
              <th>Hearing</th><th>Date</th><th>Outcome</th><th>Next Hearing</th><th>Notes</th>
            </tr></thead><tbody>
              {hearings.data.map((h) => (
                <tr key={h.hearingId}>
                  <td className="text-mono">{h.hearingId}</td><td>{date(h.hearingDate)}</td>
                  <td><StatusBadge value={h.hearingOutcome} /></td><td>{date(h.nextHearingDate)}</td><td>{h.notes || '—'}</td>
                </tr>
              ))}
            </tbody></Table>
          )}
        </Card.Body>
      </Card>

      <FormModal show={edit} title="Update Legal Case" submitLabel="Save changes"
        initial={{ accountId: c.accountId, caseType: c.caseType, filingDate: c.filingDate, courtName: c.courtName, caseNumber: c.caseNumber, status: c.status }}
        onClose={() => setEdit(false)} onSaved={reload} onSubmit={(v) => legalApi.updateCase(id, v)}>
        {(v, set, errs) => (<>
          <Row>
            <Col md={6}><Field label="Case Type" name="caseType" type="select" options={ENUMS.CaseType} value={v.caseType} onChange={set} error={errs.caseType} required /></Col>
            <Col md={6}><Field label="Status" name="status" type="select" options={ENUMS.CaseStatus} value={v.status} onChange={set} error={errs.status} /></Col>
            <Col md={6}><Field label="Court Name" name="courtName" value={v.courtName} onChange={set} error={errs.courtName} required /></Col>
            <Col md={6}><Field label="Case Number" name="caseNumber" value={v.caseNumber} onChange={set} error={errs.caseNumber} required /></Col>
            <Col md={6}><Field label="Filing Date" name="filingDate" type="date" value={v.filingDate} onChange={set} error={errs.filingDate} required /></Col>
          </Row>
        </>)}
      </FormModal>

      <FormModal show={addHearing} title="Add Court Hearing" submitLabel="Add hearing"
        initial={{ caseId: id, hearingDate: today(), hearingOutcome: '', nextHearingDate: '', notes: '' }}
        onClose={() => setAddHearing(false)} onSaved={hearings.reload} onSubmit={(v) => legalApi.addHearing(v)}>
        {(v, set, errs) => (<>
          <Row>
            <Col md={6}><Field label="Hearing Date" name="hearingDate" type="date" value={v.hearingDate} onChange={set} error={errs.hearingDate} required /></Col>
            <Col md={6}><Field label="Outcome" name="hearingOutcome" type="select" options={ENUMS.HearingOutcome} value={v.hearingOutcome} onChange={set} error={errs.hearingOutcome} required /></Col>
            <Col md={6}><Field label="Next Hearing Date" name="nextHearingDate" type="date" value={v.nextHearingDate} onChange={set} error={errs.nextHearingDate} /></Col>
          </Row>
          <Field label="Notes" name="notes" type="textarea" value={v.notes} onChange={set} error={errs.notes} />
        </>)}
      </FormModal>
    </>
  );
}
