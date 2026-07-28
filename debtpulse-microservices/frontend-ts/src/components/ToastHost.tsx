import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import { ToastContainer, Toast } from 'react-bootstrap';

type Variant = 'success' | 'danger' | 'info' | 'warning';
interface ToastItem { id: number; message: string; variant: Variant; title?: string; }

interface ToastApi {
  success: (m: string, t?: string) => void;
  error: (m: string, t?: string) => void;
  info: (m: string, t?: string) => void;
  warn: (m: string, t?: string) => void;
}

const ToastCtx = createContext<ToastApi | null>(null);
let idSeq = 0;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const remove = useCallback((id: number) => setToasts((t) => t.filter((x) => x.id !== id)), []);
  const push = useCallback((message: string, variant: Variant, title?: string) => {
    const id = ++idSeq;
    setToasts((t) => [...t, { id, message, variant, title }]);
    setTimeout(() => remove(id), 4500);
  }, [remove]);

  const api: ToastApi = {
    success: (m, t) => push(m, 'success', t),
    error: (m, t) => push(m, 'danger', t),
    info: (m, t) => push(m, 'info', t),
    warn: (m, t) => push(m, 'warning', t),
  };

  const iconFor: Record<Variant, string> = {
    success: 'check-circle-fill', danger: 'exclamation-octagon-fill',
    warning: 'exclamation-triangle-fill', info: 'info-circle-fill',
  };

  return (
    <ToastCtx.Provider value={api}>
      {children}
      <ToastContainer position="top-end" className="p-3" style={{ zIndex: 2000 }}>
        {toasts.map((t) => (
          <Toast key={t.id} bg={t.variant} onClose={() => remove(t.id)}>
            <Toast.Body className="text-white d-flex align-items-start gap-2">
              <i className={`bi bi-${iconFor[t.variant]} mt-1`} />
              <div>
                {t.title && <div className="fw-semibold">{t.title}</div>}
                <div>{t.message}</div>
              </div>
            </Toast.Body>
          </Toast>
        ))}
      </ToastContainer>
    </ToastCtx.Provider>
  );
}

export const useToast = (): ToastApi => {
  const ctx = useContext(ToastCtx);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
};
