import { Navigate, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuth } from './AuthContext';
import { hasAny } from './roles';
import type { Role } from '../types';

export default function ProtectedRoute({ allow, children }: { allow?: Role[]; children: ReactNode }) {
  const { isAuthenticated, role, ready } = useAuth();
  const location = useLocation();

  // Wait for the silent bootstrap refresh before deciding — otherwise a page reload would
  // bounce an authenticated user to /login before the cookie-based refresh resolves.
  if (!ready) {
    return (
      <div className="d-flex align-items-center justify-content-center" style={{ minHeight: '60vh' }}>
        <div className="spinner-border text-primary" role="status" aria-label="Loading" />
      </div>
    );
  }
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: location }} />;
  if (allow && !hasAny(role, allow)) return <Navigate to="/forbidden" replace />;
  return <>{children}</>;
}
