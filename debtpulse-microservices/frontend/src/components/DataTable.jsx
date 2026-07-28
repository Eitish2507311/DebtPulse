import { Table, Pagination } from 'react-bootstrap';
import { Loading, EmptyState } from './ui.jsx';

/**
 * Reusable data table with a header, optional row-click, and server-side pagination.
 * `columns`: [{ key, header, render?(row), className? }]
 * `page` is a PageResponse ({ content, page, totalPages, totalElements }) or null.
 */
export default function DataTable({
  columns, rows, page, onPageChange, loading, onRowClick,
  emptyIcon = 'inbox', emptyTitle = 'No records', emptyMessage,
}) {
  const data = rows ?? page?.content ?? [];

  if (loading) return <div className="card"><div className="card-body"><Loading /></div></div>;
  if (!data.length) {
    return <div className="card"><div className="card-body"><EmptyState icon={emptyIcon} title={emptyTitle} message={emptyMessage} /></div></div>;
  }

  return (
    <div className="card">
      <div className="table-responsive">
        <Table hover className="align-middle mb-0">
          <thead>
            <tr>{columns.map((c) => <th key={c.key} className={c.className}>{c.header}</th>)}</tr>
          </thead>
          <tbody>
            {data.map((row, i) => (
              <tr key={row.id || row._id || i}
                  onClick={onRowClick ? () => onRowClick(row) : undefined}
                  className={onRowClick ? 'cursor-pointer' : ''}>
                {columns.map((c) => (
                  <td key={c.key} className={c.className}>{c.render ? c.render(row) : row[c.key] ?? '—'}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </Table>
      </div>
      {page && page.totalPages > 1 && (
        <div className="d-flex justify-content-between align-items-center px-3 py-2 border-top">
          <small className="text-muted">
            Page {page.page + 1} of {page.totalPages} · {page.totalElements} record{page.totalElements === 1 ? '' : 's'}
          </small>
          <Pagination size="sm" className="mb-0">
            <Pagination.Prev disabled={page.page === 0} onClick={() => onPageChange(page.page - 1)} />
            {Array.from({ length: page.totalPages }).slice(
              Math.max(0, page.page - 2), Math.max(0, page.page - 2) + 5
            ).map((_, idx) => {
              const p = Math.max(0, page.page - 2) + idx;
              return <Pagination.Item key={p} active={p === page.page} onClick={() => onPageChange(p)}>{p + 1}</Pagination.Item>;
            })}
            <Pagination.Next disabled={page.page + 1 >= page.totalPages} onClick={() => onPageChange(page.page + 1)} />
          </Pagination>
        </div>
      )}
    </div>
  );
}
