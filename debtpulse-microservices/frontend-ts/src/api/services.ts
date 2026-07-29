import api from './client';
import type {
  AuthResponse, PageResponse, UserDto, AuditLog, Account, CollateralAsset, AllocationRule,
  ContactAttempt, Ptp, BorrowerContact, FieldVisit, AssetVerification, Settlement, Restructuring,
  LegalCase, CourtHearing, RecoveryOrder, RecoveryReport, Notification, Metrics,
} from '../types';

type Params = Record<string, unknown>;

export const authApi = {
  login: (email: string, password: string) => api.post<AuthResponse>('/auth/login', { email, password }),
  // refresh/logout carry no body — the refresh token travels as the httpOnly cookie.
  refresh: () => api.post<AuthResponse>('/auth/refresh'),
  logout: () => api.post('/auth/logout'),
  forgotPassword: (email: string) => api.post<{ message: string; token?: string }>('/auth/forgot-password', { email }),
  resetPassword: (token: string, newPassword: string) => api.post('/auth/reset-password', { token, newPassword }),
  changePassword: (currentPassword: string, newPassword: string) => api.post('/auth/change-password', { currentPassword, newPassword }),
  register: (body: Params) => api.post<AuthResponse>('/auth/register', body),
};

export const userApi = {
  list: (params: Params) => api.get<PageResponse<UserDto>>('/users', { params }),
  get: (id: string) => api.get<UserDto>(`/users/${id}`),
  create: (body: Params) => api.post<UserDto>('/users', body),
  update: (id: string, body: Params) => api.put<UserDto>(`/users/${id}`, body),
  setStatus: (id: string, status: string) => api.patch<UserDto>(`/users/${id}/status`, null, { params: { status } }),
  remove: (id: string) => api.delete(`/users/${id}`),
};

export const auditApi = {
  list: (params: Params) => api.get<PageResponse<AuditLog>>('/audit-logs', { params }),
  byUser: (userId: string, params: Params) => api.get<PageResponse<AuditLog>>(`/audit-logs/user/${userId}`, { params }),
  byEntity: (entityType: string, recordId: string) => api.get<AuditLog[]>(`/audit-logs/entity/${entityType}/${recordId}`),
};

export const accountApi = {
  list: (params: Params) => api.get<PageResponse<Account>>('/accounts', { params }),
  get: (id: string) => api.get<Account>(`/accounts/${id}`),
  create: (body: Params) => api.post<Account>('/accounts', body),
  update: (id: string, body: Params) => api.put<Account>(`/accounts/${id}`, body),
  remove: (id: string) => api.delete(`/accounts/${id}`),
  assignAgent: (id: string, agentId: string) => api.patch<Account>(`/accounts/${id}/assign-agent/${agentId}`),
  setStatus: (id: string, status: string) => api.patch<Account>(`/accounts/${id}/status`, null, { params: { status } }),
};

export const collateralApi = {
  byAccount: (accountId: string) => api.get<CollateralAsset[]>(`/collateral-assets/account/${accountId}`),
  get: (id: string) => api.get<CollateralAsset>(`/collateral-assets/${id}`),
  create: (body: Params) => api.post<CollateralAsset>('/collateral-assets', body),
  update: (id: string, body: Params) => api.put<CollateralAsset>(`/collateral-assets/${id}`, body),
};

export const allocationApi = {
  list: (params: Params) => api.get<PageResponse<AllocationRule>>('/allocations', { params }),
  create: (body: Params) => api.post<AllocationRule>('/allocations', body),
  update: (id: string, body: Params) => api.put<AllocationRule>(`/allocations/${id}`, body),
  remove: (id: string) => api.delete(`/allocations/${id}`),
  execute: () => api.post<{ allocated: number }>('/allocations/execute'),
};

export const contactApi = {
  list: (params: Params) => api.get<PageResponse<ContactAttempt>>('/contacts', { params }),
  get: (id: string) => api.get<ContactAttempt>(`/contacts/${id}`),
  create: (body: Params) => api.post<ContactAttempt>('/contacts', body),
  update: (id: string, body: Params) => api.put<ContactAttempt>(`/contacts/${id}`, body),
};

export const ptpApi = {
  list: (params: Params) => api.get<PageResponse<Ptp>>('/ptp', { params }),
  get: (id: string) => api.get<Ptp>(`/ptp/${id}`),
  create: (body: Params) => api.post<Ptp>('/ptp', body),
  recordPayment: (id: string, actualPaidAmount: number) => api.patch<Ptp>(`/ptp/${id}/payment`, null, { params: { actualPaidAmount } }),
  reschedule: (id: string, commitmentDate: string) => api.patch<Ptp>(`/ptp/${id}/reschedule`, null, { params: { commitmentDate } }),
};

export const borrowerContactApi = {
  list: (params: Params) => api.get<PageResponse<BorrowerContact>>('/borrower-contacts', { params }),
  byAccount: (accountId: string) => api.get<BorrowerContact[]>(`/borrower-contacts/account/${accountId}`),
  create: (body: Params) => api.post<BorrowerContact>('/borrower-contacts', body),
  update: (id: string, body: Params) => api.put<BorrowerContact>(`/borrower-contacts/${id}`, body),
  remove: (id: string) => api.delete(`/borrower-contacts/${id}`),
};

export const visitApi = {
  list: (params: Params) => api.get<PageResponse<FieldVisit>>('/visits', { params }),
  myVisits: () => api.get<FieldVisit[]>('/visits/my-visits'),
  schedule: (body: Params) => api.post<FieldVisit>('/visits', body),
  complete: (id: string, body: Params) => api.patch<FieldVisit>(`/visits/${id}/complete`, body),
  markMissed: (id: string) => api.patch<FieldVisit>(`/visits/${id}/missed`),
};

export const assetVerificationApi = {
  list: (params: Params) => api.get<PageResponse<AssetVerification>>('/asset-verifications', { params }),
  byVisit: (visitId: string) => api.get<AssetVerification[]>(`/asset-verifications/visit/${visitId}`),
  create: (body: Params) => api.post<AssetVerification>('/asset-verifications', body),
  update: (id: string, body: Params) => api.put<AssetVerification>(`/asset-verifications/${id}`, body),
};

export const settlementApi = {
  list: (params: Params) => api.get<PageResponse<Settlement>>('/settlements', { params }),
  get: (id: string) => api.get<Settlement>(`/settlements/${id}`),
  create: (body: Params) => api.post<Settlement>('/settlements', body),
  update: (id: string, body: Params) => api.put<Settlement>(`/settlements/${id}`, body),
  submit: (id: string) => api.patch<Settlement>(`/settlements/${id}/submit`),
  decide: (id: string, level: string, body: Params) => api.post<Settlement>(`/settlements/${id}/decide`, body, { params: { level } }),
  markPaid: (id: string) => api.patch<Settlement>(`/settlements/${id}/mark-paid`),
  outstanding: () => api.get<Settlement[]>('/settlements/outstanding'),
  approvalQueue: () => api.get<Settlement[]>('/settlements/approval-queue'),
};

export const restructuringApi = {
  list: (params: Params) => api.get<PageResponse<Restructuring>>('/restructuring', { params }),
  get: (id: string) => api.get<Restructuring>(`/restructuring/${id}`),
  byAccount: (accountId: string) => api.get<Restructuring[]>(`/restructuring/account/${accountId}`),
  create: (body: Params) => api.post<Restructuring>('/restructuring', body),
  update: (id: string, body: Params) => api.put<Restructuring>(`/restructuring/${id}`, body),
  approve: (id: string) => api.patch<Restructuring>(`/restructuring/${id}/approve`),
  reject: (id: string) => api.patch<Restructuring>(`/restructuring/${id}/reject`),
};

export const legalApi = {
  listCases: (params: Params) => api.get<PageResponse<LegalCase>>('/legal/cases', { params }),
  getCase: (id: string) => api.get<LegalCase>(`/legal/cases/${id}`),
  createCase: (body: Params) => api.post<LegalCase>('/legal/cases', body),
  updateCase: (id: string, body: Params) => api.put<LegalCase>(`/legal/cases/${id}`, body),
  listHearings: (caseId: string) => api.get<CourtHearing[]>(`/legal/cases/${caseId}/hearings`),
  listAllHearings: () => api.get<CourtHearing[]>('/legal/hearings'),
  addHearing: (body: Params) => api.post<CourtHearing>('/legal/hearings', body),
  listOrders: () => api.get<RecoveryOrder[]>('/legal/orders'),
  getOrder: (id: string) => api.get<RecoveryOrder>(`/legal/orders/${id}`),
  issueOrder: (body: Params) => api.post<RecoveryOrder>('/legal/orders', body),
  deleteOrder: (id: string) => api.delete(`/legal/orders/${id}`),
};

export const analyticsApi = {
  dashboard: () => api.get<Metrics>('/analytics/dashboard'),
  bucketDistribution: () => api.get<Metrics>('/analytics/bucket-distribution'),
  ptpMetrics: () => api.get<Metrics>('/analytics/ptp-metrics'),
  settlementMetrics: () => api.get<Metrics>('/analytics/settlement-metrics'),
  recoveryRate: () => api.get<Metrics>('/analytics/recovery-rate'),
  cashCollected: () => api.get<Metrics>('/analytics/cash-collected'),
  fieldVisitSuccess: () => api.get<Metrics>('/analytics/field-visit-success'),
  legalConversion: () => api.get<Metrics>('/analytics/legal-conversion'),
  generateReport: (scope: string) => api.post<RecoveryReport>('/analytics/reports/generate', null, { params: { scope } }),
  listReports: (params: Params) => api.get<PageResponse<RecoveryReport>>('/analytics/reports', { params }),
};

export const notificationApi = {
  list: (params: Params) => api.get<PageResponse<Notification>>('/notifications', { params }),
  unreadCount: () => api.get<{ unreadCount: number }>('/notifications/unread-count'),
  markRead: (id: string) => api.patch<Notification>(`/notifications/${id}/read`),
  dismiss: (id: string) => api.patch<Notification>(`/notifications/${id}/dismiss`),
  markAllRead: () => api.patch<{ updated: number }>('/notifications/read-all'),
};
