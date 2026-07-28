import { useCallback } from 'react';
import { auditApi } from '../../api/services';
import { usePaged } from '../../hooks/usePaged';
import { PageHeader, ErrorNote } from '../../components/ui';
import DataTable from '../../components/DataTable';
import { dateTime, titleCase } from '../../utils/format';
import type { AuditLog, Column } from '../../types';

export default function AuditPage() {
  const fetcher = useCallback((p: Record<string, unknown>) => auditApi.list(p), []);
  const { page, loading, error, setPage } = usePaged<AuditLog>(fetcher);

  const columns: Column<AuditLog>[] = [
    { key: 'timestamp', header: 'When', render: (r) => dateTime(r.timestamp) },
    // Open/permitAll actions (login, logout, refresh, forgot-password) have no authenticated actor,
    // so userId is null — fall back to the entity id (the real/attempted account) instead of blank.
    { key: 'userId', header: 'User', render: (r) => <span className="text-mono">{r.userId || r.recordId || '—'}</span> },
    { key: 'action', header: 'Action', render: (r) => <span className="badge-pill s-blue">{titleCase(r.action)}</span> },
    { key: 'entityType', header: 'Entity', render: (r) => r.entityType },
    { key: 'recordId', header: 'Record', render: (r) => <span className="text-mono">{r.recordId}</span> },
    { key: 'sourceService', header: 'Source', render: (r) => r.sourceService || '—' },
  ];

  return (
    <>
      <PageHeader title="Audit Trail" subtitle="Every create, update and decision recorded across the platform" icon="shield-check" />
      <ErrorNote error={error} />
      <DataTable<AuditLog> columns={columns} page={page} loading={loading} onPageChange={setPage}
        emptyIcon="shield-check" emptyTitle="No audit entries" />
    </>
  );
}
