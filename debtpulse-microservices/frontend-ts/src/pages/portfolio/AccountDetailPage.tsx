import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Row, Col, Card, Button, Tabs, Tab, Table, Dropdown } from 'react-bootstrap';
import { accountApi, collateralApi, borrowerContactApi } from '../../api/services';
import { useAsync } from '../../hooks/usePaged';
import { useAuth } from '../../auth/AuthContext';
import { PageHeader, Loading, StatusBadge, EmptyState, ErrorNote } from '../../components/ui';
import FormModal from '../../components/FormModal';
import Field from '../../components/Field';
import { useToast } from '../../components/ToastHost';
import { ENUMS, bucketClass } from '../../utils/enums';
import { inr, num, date, titleCase } from '../../utils/format';
import type { Account, CollateralAsset, BorrowerContact } from '../../types';

export default function AccountDetailPage() {
  const { id = '' } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const { role } = useAuth();
  const canEdit = role === 'ADMIN' || role === 'COLLECTIONS_AGENT';
  const canCollateral = role === 'ADMIN' || role === 'FIELD_OFFICER';

  const { data: acc, loading, error, reload } = useAsync<Account>(() => accountApi.get(id), [id]);
  const [edit, setEdit] = useState(false);

  if (loading) return <Loading />;
  if (error) return <ErrorNote error={error} />;
  if (!acc) return null;

  const info: [string, string | number][] = [
    ['Loan Reference', acc.loanRef], ['Borrower', acc.borrowerName], ['Phone', acc.phone || '—'],
    ['Address', acc.address || '—'], ['Branch', acc.branchId || '—'], ['Assigned Agent', acc.assignedAgentId || 'Unassigned'],
    ['Principal', inr(acc.principalAmount)], ['Total Overdue', inr(acc.totalOverdue)], ['DPD', num(acc.dpd)],
  ];

  const setStatus = async (status: string) => {
    try { await accountApi.setStatus(id, status); toast.success(`Status set to ${titleCase(status)}`); reload(); }
    catch { toast.error('Could not update status'); }
  };

  return (
    <>
      <Button variant="link" className="px-0 mb-2" onClick={() => navigate('/portfolio')}>
        <i className="bi bi-arrow-left me-1" />Back to portfolio
      </Button>
      <PageHeader
        title={acc.borrowerName}
        subtitle={<span className="text-mono">{acc.accountId} · {acc.loanRef}</span>}
        icon="person-vcard"
        actions={<>
          <span className={`badge-pill ${bucketClass(acc.bucket)} align-self-center`}>{acc.bucket}</span>
          <StatusBadge value={acc.status} />
          {canEdit && <Button variant="outline-primary" size="sm" onClick={() => setEdit(true)}><i className="bi bi-pencil me-1" />Edit</Button>}
          {canEdit && (
            <Dropdown>
              <Dropdown.Toggle variant="outline-secondary" size="sm">Status</Dropdown.Toggle>
              <Dropdown.Menu>
                {ENUMS.AccountStatus.map((s) => <Dropdown.Item key={s} onClick={() => setStatus(s)}>{titleCase(s)}</Dropdown.Item>)}
              </Dropdown.Menu>
            </Dropdown>
          )}
        </>}
      />

      <Row className="g-3 mb-3">
        <Col md={4}><Card className="h-100"><Card.Body>
          <div className="text-muted small text-uppercase">Total Overdue</div>
          <div className="h3 text-danger mb-0">{inr(acc.totalOverdue)}</div>
        </Card.Body></Card></Col>
        <Col md={4}><Card className="h-100"><Card.Body>
          <div className="text-muted small text-uppercase">Principal Outstanding</div>
          <div className="h3 mb-0" style={{ color: 'var(--dp-navy)' }}>{inr(acc.principalAmount)}</div>
        </Card.Body></Card></Col>
        <Col md={4}><Card className="h-100"><Card.Body>
          <div className="text-muted small text-uppercase">Days Past Due</div>
          <div className="h3 mb-0" style={{ color: 'var(--dp-navy)' }}>{num(acc.dpd)}</div>
        </Card.Body></Card></Col>
      </Row>

      <Tabs defaultActiveKey="overview" className="mb-3">
        <Tab eventKey="overview" title="Overview">
          <Card><Card.Body><Row>
            {info.map(([k, val]) => (
              <Col md={4} key={k} className="mb-3">
                <div className="text-muted small text-uppercase">{k}</div>
                <div className="fw-semibold">{val}</div>
              </Col>
            ))}
          </Row></Card.Body></Card>
        </Tab>
        <Tab eventKey="collateral" title="Collateral">
          <CollateralTab accountId={id} canWrite={canCollateral} />
        </Tab>
        <Tab eventKey="contacts" title="Borrower Contacts">
          <BorrowerContactsTab accountId={id} canWrite={role === 'ADMIN' || role === 'COLLECTIONS_AGENT'} />
        </Tab>
      </Tabs>

      <FormModal show={edit} title="Edit Account" submitLabel="Save changes"
        initial={{ borrowerName: acc.borrowerName, phone: acc.phone || '', address: acc.address || '', branchId: acc.branchId || '',
          principalAmount: acc.principalAmount, totalOverdue: acc.totalOverdue, dpd: acc.dpd,
          daysInCurrentBucket: acc.daysInCurrentBucket, status: acc.status }}
        onClose={() => setEdit(false)} onSaved={reload}
        onSubmit={(v) => accountApi.update(id, {
          ...v, principalAmount: Number(v.principalAmount), totalOverdue: Number(v.totalOverdue), dpd: Number(v.dpd),
          daysInCurrentBucket: v.daysInCurrentBucket === '' || v.daysInCurrentBucket == null ? null : Number(v.daysInCurrentBucket),
        })}>
        {(v, set, errs) => (
          <Row>
            <Col md={6}><Field label="Borrower Name" name="borrowerName" value={v.borrowerName} onChange={set} error={errs.borrowerName} /></Col>
            <Col md={6}><Field label="Phone" name="phone" value={v.phone} onChange={set} error={errs.phone} /></Col>
            <Col md={12}><Field label="Address" name="address" value={v.address} onChange={set} error={errs.address} /></Col>
            <Col md={4}><Field label="Principal" name="principalAmount" type="number" value={v.principalAmount} onChange={set} error={errs.principalAmount} /></Col>
            <Col md={4}><Field label="Total Overdue" name="totalOverdue" type="number" value={v.totalOverdue} onChange={set} error={errs.totalOverdue} /></Col>
            <Col md={4}><Field label="DPD" name="dpd" type="number" value={v.dpd} onChange={set} error={errs.dpd} help="Bucket is re-derived" /></Col>
            <Col md={6}><Field label="Days in Current Bucket" name="daysInCurrentBucket" type="number" min="0" value={v.daysInCurrentBucket} onChange={set} error={errs.daysInCurrentBucket} /></Col>
            <Col md={6}><Field label="Status" name="status" type="select" options={ENUMS.AccountStatus} value={v.status} onChange={set} error={errs.status} /></Col>
          </Row>
        )}
      </FormModal>
    </>
  );
}

function CollateralTab({ accountId, canWrite }: { accountId: string; canWrite: boolean }) {
  const { data, loading, reload } = useAsync<CollateralAsset[]>(() => collateralApi.byAccount(accountId), [accountId]);
  const [show, setShow] = useState(false);
  if (loading) return <Loading />;
  return (
    <Card><Card.Header className="d-flex justify-content-between align-items-center">
      Collateral Assets
      {canWrite && <Button size="sm" onClick={() => setShow(true)}><i className="bi bi-plus-lg me-1" />Add asset</Button>}
    </Card.Header>
      <Card.Body className="p-0">
        {!data?.length ? <EmptyState icon="box-seam" title="No collateral registered" /> : (
          <Table hover className="mb-0"><thead><tr>
            <th>Asset</th><th>Type</th><th>Description</th><th className="text-end">Est. Value</th><th>Verification</th><th>Last Verified</th>
          </tr></thead><tbody>
            {data.map((a) => (
              <tr key={a.assetId}>
                <td className="text-mono">{a.assetId}</td><td>{titleCase(a.assetType)}</td><td>{a.description || '—'}</td>
                <td className="text-end">{inr(a.estimatedValue)}</td><td><StatusBadge value={a.verificationStatus} /></td>
                <td>{date(a.lastVerifiedDate)}</td>
              </tr>
            ))}
          </tbody></Table>
        )}
      </Card.Body>
      <FormModal show={show} title="Register Collateral Asset" submitLabel="Add asset"
        initial={{ accountId, assetType: '', description: '', estimatedValue: '' }}
        onClose={() => setShow(false)} onSaved={reload}
        onSubmit={(v) => collateralApi.create({ ...v, estimatedValue: Number(v.estimatedValue) })}>
        {(v, set, errs) => (<>
          <Field label="Asset Type" name="assetType" type="select" options={ENUMS.AssetType} value={v.assetType} onChange={set} error={errs.assetType} required />
          <Field label="Description" name="description" value={v.description} onChange={set} error={errs.description} />
          <Field label="Estimated Value" name="estimatedValue" type="number" min="0" value={v.estimatedValue} onChange={set} error={errs.estimatedValue} required />
        </>)}
      </FormModal>
    </Card>
  );
}

function BorrowerContactsTab({ accountId, canWrite }: { accountId: string; canWrite: boolean }) {
  const { data, loading, reload } = useAsync<BorrowerContact[]>(() => borrowerContactApi.byAccount(accountId), [accountId]);
  const [show, setShow] = useState(false);
  if (loading) return <Loading />;
  return (
    <Card><Card.Header className="d-flex justify-content-between align-items-center">
      Borrower Contacts
      {canWrite && <Button size="sm" onClick={() => setShow(true)}><i className="bi bi-plus-lg me-1" />Add contact</Button>}
    </Card.Header>
      <Card.Body className="p-0">
        {!data?.length ? <EmptyState icon="person-lines-fill" title="No contact records" /> : (
          <Table hover className="mb-0"><thead><tr>
            <th>Name</th><th>Type</th><th>Phone</th><th>Relationship</th><th>Status</th>
          </tr></thead><tbody>
            {data.map((c) => (
              <tr key={c.contactRecordId}>
                <td className="fw-semibold">{c.name}</td><td>{titleCase(c.contactType)}</td>
                <td>{c.phone}</td><td>{c.relationship || '—'}</td><td><StatusBadge value={c.status} /></td>
              </tr>
            ))}
          </tbody></Table>
        )}
      </Card.Body>
      <FormModal show={show} title="Add Borrower Contact" submitLabel="Add contact"
        initial={{ accountId, contactType: '', name: '', phone: '', relationship: '' }}
        onClose={() => setShow(false)} onSaved={reload}
        onSubmit={(v) => borrowerContactApi.create(v)}>
        {(v, set, errs) => (<>
          <Field label="Contact Type" name="contactType" type="select" options={ENUMS.BorrowerContactType} value={v.contactType} onChange={set} error={errs.contactType} required />
          <Field label="Name" name="name" value={v.name} onChange={set} error={errs.name} required />
          <Field label="Phone" name="phone" value={v.phone} onChange={set} error={errs.phone} help="10 digits" required />
          <Field label="Relationship" name="relationship" value={v.relationship} onChange={set} error={errs.relationship} />
        </>)}
      </FormModal>
    </Card>
  );
}
