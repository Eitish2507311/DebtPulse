import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext.jsx';
import { hasAny } from './roles.js';

/** Guards a route: requires a session, and optionally a role in `allow`. */
export default function ProtectedRoute({ allow, children }) {
  const { isAuthenticated, role } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: location }} />;
  if (allow && !hasAny(role, allow)) return <Navigate to="/forbidden" replace />;
  return children;
}
