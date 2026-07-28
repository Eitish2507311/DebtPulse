import { useCallback, useEffect, useState } from 'react';
import type { AxiosResponse } from 'axios';
import { toAppError } from '../api/client';
import type { AppError, PageResponse } from '../types';

type Params = Record<string, unknown>;

interface UsePagedResult<T> {
  page: PageResponse<T> | null;
  loading: boolean;
  error: AppError | null;
  pageNo: number;
  setPage: (p: number) => void;
  reload: () => void;
}

export function usePaged<T>(
  fetcher: (params: Params) => Promise<AxiosResponse<PageResponse<T>>>,
  { size = 20, filters = {} }: { size?: number; filters?: Params } = {},
): UsePagedResult<T> {
  const [page, setPageState] = useState<PageResponse<T> | null>(null);
  const [pageNo, setPageNo] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<AppError | null>(null);
  const filterKey = JSON.stringify(filters);

  const load = useCallback(async (p = pageNo) => {
    setLoading(true); setError(null);
    try {
      const { data } = await fetcher({ page: p, size, ...filters });
      setPageState(data);
      setPageNo(data.page ?? p);
    } catch (e) {
      setError(toAppError(e));
      setPageState({ content: [], page: 0, size, totalPages: 0, totalElements: 0 });
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fetcher, size, filterKey]);

  useEffect(() => { load(0); /* eslint-disable-next-line */ }, [filterKey]);

  return { page, loading, error, pageNo, setPage: (p) => load(p), reload: () => load(pageNo) };
}

interface UseAsyncResult<T> {
  data: T | null;
  loading: boolean;
  error: AppError | null;
  reload: () => void;
  setData: (d: T | null) => void;
}

export function useAsync<T>(
  fetcher: () => Promise<AxiosResponse<T>>,
  deps: unknown[] = [],
): UseAsyncResult<T> {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<AppError | null>(null);

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
