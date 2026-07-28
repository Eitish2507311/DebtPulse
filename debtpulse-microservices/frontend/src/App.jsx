import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './auth/AuthContext.jsx';
import ProtectedRoute from './auth/ProtectedRoute.jsx';
import { ACCESS } from './auth/roles.js';
import Layout from './components/Layout.jsx';

import LandingPage from './pages/LandingPage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import ForgotPasswordPage from './pages/ForgotPasswordPage.jsx';
import ResetPasswordPage from './pages/ResetPasswordPage.jsx';
import ChangePasswordPage from './pages/ChangePasswordPage.jsx';
import DashboardPage from './pages/DashboardPage.jsx';
import AccountsPage from './pages/portfolio/AccountsPage.jsx';
import AccountDetailPage from './pages/portfolio/AccountDetailPage.jsx';
import AllocationsPage from './pages/portfolio/AllocationsPage.jsx';
import ContactWorkspace from './pages/contact/ContactWorkspace.jsx';
import FieldWorkspace from './pages/field/FieldWorkspace.jsx';
import SettlementWorkspace from './pages/settlement/SettlementWorkspace.jsx';
import LegalPage from './pages/legal/LegalPage.jsx';
import LegalCaseDetailPage from './pages/legal/LegalCaseDetailPage.jsx';
import AnalyticsPage from './pages/analytics/AnalyticsPage.jsx';
import NotificationsPage from './pages/notifications/NotificationsPage.jsx';
import UsersPage from './pages/admin/UsersPage.jsx';
import AuditPage from './pages/admin/AuditPage.jsx';
import { ForbiddenPage, NotFoundPage } from './pages/StatusPages.jsx';

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
