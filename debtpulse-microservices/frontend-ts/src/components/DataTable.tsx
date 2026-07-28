import { Table, Pagination } from 'react-bootstrap';
import { Loading, EmptyState } from './ui';
import type { Column, PageResponse } from '../types';

interface DataTableProps<T> {
  columns: Column<T>[];
  rows?: T[];
  page?: PageResponse<T> | null;
  onPageChange?: (p: number) => void;
  loading?: boolean;
  onRowClick?: (row: T) => void;
  emptyIcon?: string;
  emptyTitle?: string;
  emptyMessage?: string;
}

export default function DataTable<T>({
  columns, rows, page, onPageChange, loading, onRowClick,
  emptyIcon = 'inbox', emptyTitle = 'No records', emptyMessage,
}: DataTableProps<T>) {
  const data: T[] = rows ?? page?.content ?? [];

  if (loading) return <div className="card"><div className="card-body"><Loading /></div></div>;
  if (!data.length) {
    return <div className="card"><div className="card-body"><EmptyState icon={emptyIcon} title={emptyTitle} message={emptyMessage} /></div></div>;
  }

  const start = page ? Math.max(0, page.page - 2) : 0;

  return (
    <div className="card">
      <div className="table-responsive">
        <Table hover className="align-middle mb-0">
          <thead>
            <tr>{columns.map((c) => <th key={c.key} className={c.className}>{c.header}</th>)}</tr>
          </thead>
          <tbody>
            {data.map((row, i) => {
              const bag = row as Record<string, unknown>;
              return (
                <tr key={(bag.id as string) || i}
                    onClick={onRowClick ? () => onRowClick(row) : undefined}
                    className={onRowClick ? 'cursor-pointer' : ''}>
                  {columns.map((c) => (
                    <td key={c.key} className={c.className}>
                      {c.render ? c.render(row) : ((bag[c.key] as ReactNodeLike) ?? '—')}
                    </td>
                  ))}
                </tr>
              );
            })}
          </tbody>
        </Table>
      </div>
      {page && page.totalPages > 1 && onPageChange && (
        <div className="d-flex justify-content-between align-items-center px-3 py-2 border-top">
          <small className="text-muted">
            Page {page.page + 1} of {page.totalPages} · {page.totalElements} record{page.totalElements === 1 ? '' : 's'}
          </small>
          <Pagination size="sm" className="mb-0">
            <Pagination.Prev disabled={page.page === 0} onClick={() => onPageChange(page.page - 1)} />
            {Array.from({ length: page.totalPages }).slice(start, start + 5).map((_, idx) => {
              const p = start + idx;
              return <Pagination.Item key={p} active={p === page.page} onClick={() => onPageChange(p)}>{p + 1}</Pagination.Item>;
            })}
            <Pagination.Next disabled={page.page + 1 >= page.totalPages} onClick={() => onPageChange(page.page + 1)} />
          </Pagination>
        </div>
      )}
    </div>
  );
}

type ReactNodeLike = string | number | null | undefined;
