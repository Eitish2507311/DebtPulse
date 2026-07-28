import { createContext, useContext, useState, useCallback } from 'react';
import { ToastContainer, Toast } from 'react-bootstrap';

const ToastCtx = createContext(null);
let idSeq = 0;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const remove = useCallback((id) => setToasts((t) => t.filter((x) => x.id !== id)), []);
  const push = useCallback((message, variant = 'success', title) => {
    const id = ++idSeq;
    setToasts((t) => [...t, { id, message, variant, title }]);
    setTimeout(() => remove(id), 4500);
  }, [remove]);

  const api = {
    success: (m, t) => push(m, 'success', t),
    error: (m, t) => push(m, 'danger', t),
    info: (m, t) => push(m, 'info', t),
    warn: (m, t) => push(m, 'warning', t),
  };

  const iconFor = { success: 'check-circle-fill', danger: 'exclamation-octagon-fill', warning: 'exclamation-triangle-fill', info: 'info-circle-fill' };

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

export const useToast = () => useContext(ToastCtx);
