import { useState, useCallback } from 'react';
import { Button, Card } from 'react-bootstrap';
import { notificationApi } from '../../api/services.js';
import { usePaged } from '../../hooks/usePaged.js';
import { PageHeader, ErrorNote, EmptyState, Loading, Pill } from '../../components/ui.jsx';
import { useToast } from '../../components/ToastHost.jsx';
import { dateTime, titleCase } from '../../utils/format.js';
import { statusClass } from '../../utils/enums.js';

const CATEGORY_ICON = {
  PTP: 'hand-thumbs-up', FIELD_VISIT: 'geo-alt', SETTLEMENT: 'cash-coin',
  LEGAL: 'bank', ESCALATION: 'exclamation-triangle', PORTFOLIO: 'folder2-open',
};

export default function NotificationsPage() {
  const toast = useToast();
  const fetcher = useCallback((p) => notificationApi.list(p), []);
  const { page, loading, error, setPage, reload } = usePaged(fetcher);

  const act = async (fn, msg) => { try { await fn(); toast.success(msg); reload(); } catch { toast.error('Action failed'); } };

  return (
    <>
      <PageHeader title="Notifications" subtitle="Alerts for PTP breaches, visits, settlements, legal hearings and escalations" icon="bell"
        actions={<Button variant="outline-primary" onClick={() => act(() => notificationApi.markAllRead(), 'All marked read')}>
          <i className="bi bi-check2-all me-1" />Mark all read</Button>} />
      <ErrorNote error={error} />
      {loading ? <Loading /> : !page?.content?.length ? (
        <Card><Card.Body><EmptyState icon="bell-slash" title="No notifications" /></Card.Body></Card>
      ) : (
        <Card><Card.Body className="p-0"><ul className="list-group list-group-flush">
          {page.content.map((n) => (
            <li key={n.notificationId} className="list-group-item d-flex align-items-start gap-3 py-3">
              <div className="icon d-flex align-items-center justify-content-center flex-shrink-0"
                   style={{ width: 40, height: 40, borderRadius: 10, background: '#e8f1fb', color: 'var(--dp-blue-600)' }}>
                <i className={`bi bi-${CATEGORY_ICON[n.category] || 'bell'}`} />
              </div>
              <div className="flex-grow-1">
                <div className="d-flex justify-content-between">
                  <span className={n.status === 'UNREAD' ? 'fw-semibold' : ''}>{n.message}</span>
                  <Pill value={n.category} className={statusClass(n.category)} />
                </div>
                <small className="text-muted">{dateTime(n.createdDate)} · {titleCase(n.status)}</small>
              </div>
              <div className="d-flex gap-1">
                {n.status === 'UNREAD' && <Button size="sm" variant="light" title="Mark read" onClick={() => act(() => notificationApi.markRead(n.notificationId), 'Marked read')}><i className="bi bi-check2" /></Button>}
                {n.status !== 'DISMISSED' && <Button size="sm" variant="light" title="Dismiss" onClick={() => act(() => notificationApi.dismiss(n.notificationId), 'Dismissed')}><i className="bi bi-x-lg" /></Button>}
              </div>
            </li>
          ))}
        </ul></Card.Body>
          {page.totalPages > 1 && (
            <Card.Footer className="d-flex justify-content-between align-items-center">
              <small className="text-muted">Page {page.page + 1} of {page.totalPages}</small>
              <div className="btn-group btn-group-sm">
                <Button variant="light" disabled={page.page === 0} onClick={() => setPage(page.page - 1)}>Prev</Button>
                <Button variant="light" disabled={page.page + 1 >= page.totalPages} onClick={() => setPage(page.page + 1)}>Next</Button>
              </div>
            </Card.Footer>
          )}
        </Card>
      )}
    </>
  );
}
