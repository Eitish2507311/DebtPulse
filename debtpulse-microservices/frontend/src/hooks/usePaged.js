import { useCallback, useEffect, useState } from 'react';
import { toAppError } from '../api/client.js';

/**
 * Drives a paginated list backed by a `fetcher(params)` that returns a PageResponse.
 * Extra filters are merged into the request; changing them resets to page 0.
 */
export function usePaged(fetcher, { size = 20, filters = {} } = {}) {
  const [page, setPage] = useState(null);
  const [pageNo, setPageNo] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const filterKey = JSON.stringify(filters);

  const load = useCallback(async (p = pageNo) => {
    setLoading(true); setError(null);
    try {
      const { data } = await fetcher({ page: p, size, ...filters });
      setPage(data);
      setPageNo(data.page ?? p);
    } catch (e) {
      setError(toAppError(e));
      setPage({ content: [], page: 0, totalPages: 0, totalElements: 0 });
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fetcher, size, filterKey]);

  useEffect(() => { load(0); /* eslint-disable-next-line */ }, [filterKey]);

  return { page, loading, error, pageNo, setPage: (p) => load(p), reload: () => load(pageNo) };
}

/** Simple one-shot loader for non-paginated GETs. */
export function useAsync(fetcher, deps = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const reload = useCallback(async () => {
    setLoading(true); setError(null);
    try { const res = await fetcher(); setData(res.data); }
    catch (e) { setError(toAppError(e)); }
    finally { setLoading(false); }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => { reload(); }, [reload]);
  return { data, loading, error, reload, setData };
}
