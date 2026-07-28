import { Navigate, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuth } from './AuthContext';
import { hasAny } from './roles';
import type { Role } from '../types';

export default function ProtectedRoute({ allow, children }: { allow?: Role[]; children: ReactNode }) {
  const { isAuthenticated, role } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: location }} />;
  if (allow && !hasAny(role, allow)) return <Navigate to="/forbidden" replace />;
  return <>{children}</>;
}
