import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import ProtectedRoute from './auth/ProtectedRoute';
import { ACCESS } from './auth/roles';
import Layout from './components/Layout';

import LandingPage from './pages/LandingPage';
import LoginPage from './pages/LoginPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import ChangePasswordPage from './pages/ChangePasswordPage';
import DashboardPage from './pages/DashboardPage';
import AccountsPage from './pages/portfolio/AccountsPage';
import AccountDetailPage from './pages/portfolio/AccountDetailPage';
import AllocationsPage from './pages/portfolio/AllocationsPage';
import ContactWorkspace from './pages/contact/ContactWorkspace';
import FieldWorkspace from './pages/field/FieldWorkspace';
import SettlementWorkspace from './pages/settlement/SettlementWorkspace';
import LegalPage from './pages/legal/LegalPage';
import LegalCaseDetailPage from './pages/legal/LegalCaseDetailPage';
import AnalyticsPage from './pages/analytics/AnalyticsPage';
import NotificationsPage from './pages/notifications/NotificationsPage';
import UsersPage from './pages/admin/UsersPage';
import AuditPage from './pages/admin/AuditPage';
import { ForbiddenPage, NotFoundPage } from './pages/StatusPages';

export default function App() {
  const { isAuthenticated } = useAuth();

  return (
    <Routes>
      <Route path="/login" element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <LoginPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />

      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/change-password" element={<ChangePasswordPage />} />

        <Route path="/portfolio" element={<ProtectedRoute allow={ACCESS.portfolio}><AccountsPage /></ProtectedRoute>} />
        <Route path="/portfolio/:id" element={<ProtectedRoute allow={ACCESS.portfolio}><AccountDetailPage /></ProtectedRoute>} />
        <Route path="/admin/allocations" element={<ProtectedRoute allow={ACCESS.allocations}><AllocationsPage /></ProtectedRoute>} />

        <Route path="/contacts" element={<ProtectedRoute allow={ACCESS.contact}><ContactWorkspace /></ProtectedRoute>} />
        <Route path="/field" element={<ProtectedRoute allow={ACCESS.field}><FieldWorkspace /></ProtectedRoute>} />
        <Route path="/settlements" element={<ProtectedRoute allow={ACCESS.settlement}><SettlementWorkspace /></ProtectedRoute>} />

        <Route path="/legal" element={<ProtectedRoute allow={ACCESS.legal}><LegalPage /></ProtectedRoute>} />
        <Route path="/legal/:id" element={<ProtectedRoute allow={ACCESS.legal}><LegalCaseDetailPage /></ProtectedRoute>} />

        <Route path="/analytics" element={<ProtectedRoute allow={ACCESS.analytics}><AnalyticsPage /></ProtectedRoute>} />
        <Route path="/notifications" element={<NotificationsPage />} />

        <Route path="/admin/users" element={<ProtectedRoute allow={ACCESS.admin}><UsersPage /></ProtectedRoute>} />
        <Route path="/admin/audit" element={<ProtectedRoute allow={ACCESS.audit}><AuditPage /></ProtectedRoute>} />
      </Route>

      <Route path="/forbidden" element={<ForbiddenPage />} />
      <Route path="/" element={<LandingPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
