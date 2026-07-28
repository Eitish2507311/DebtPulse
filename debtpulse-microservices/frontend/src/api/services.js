import api from './client.js';

// Thin, typed-by-convention wrappers around every gateway endpoint the UI uses.
// Grouped by domain module so pages import exactly what they need.

export const authApi = {
  login: (email, password) => api.post('/auth/login', { email, password }),
  refresh: (refreshToken) => api.post('/auth/refresh', { refreshToken }),
  logout: (refreshToken) => api.post('/auth/logout', { refreshToken }),
  forgotPassword: (email) => api.post('/auth/forgot-password', { email }),
  resetPassword: (token, newPassword) => api.post('/auth/reset-password', { token, newPassword }),
  changePassword: (currentPassword, newPassword) => api.post('/auth/change-password', { currentPassword, newPassword }),
  register: (body) => api.post('/auth/register', body),
};

export const userApi = {
  list: (params) => api.get('/users', { params }),
  get: (id) => api.get(`/users/${id}`),
  create: (body) => api.post('/users', body),
  update: (id, body) => api.put(`/users/${id}`, body),
  setStatus: (id, status) => api.patch(`/users/${id}/status`, null, { params: { status } }),
  remove: (id) => api.delete(`/users/${id}`),
};

export const auditApi = {
  list: (params) => api.get('/audit-logs', { params }),
  byUser: (userId, params) => api.get(`/audit-logs/user/${userId}`, { params }),
  byEntity: (entityType, recordId) => api.get(`/audit-logs/entity/${entityType}/${recordId}`),
};

export const accountApi = {
  list: (params) => api.get('/accounts', { params }),
  get: (id) => api.get(`/accounts/${id}`),
  create: (body) => api.post('/accounts', body),
  update: (id, body) => api.put(`/accounts/${id}`, body),
  remove: (id) => api.delete(`/accounts/${id}`),
  assignAgent: (id, agentId) => api.patch(`/accounts/${id}/assign-agent/${agentId}`),
  setStatus: (id, status) => api.patch(`/accounts/${id}/status`, null, { params: { status } }),
};

export const collateralApi = {
  byAccount: (accountId) => api.get(`/collateral-assets/account/${accountId}`),
  get: (id) => api.get(`/collateral-assets/${id}`),
  create: (body) => api.post('/collateral-assets', body),
  update: (id, body) => api.put(`/collateral-assets/${id}`, body),
};

export const allocationApi = {
  list: (params) => api.get('/allocations', { params }),
  create: (body) => api.post('/allocations', body),
  update: (id, body) => api.put(`/allocations/${id}`, body),
  remove: (id) => api.delete(`/allocations/${id}`),
  execute: () => api.post('/allocations/execute'),
};

export const contactApi = {
  list: (params) => api.get('/contacts', { params }),
  get: (id) => api.get(`/contacts/${id}`),
  create: (body) => api.post('/contacts', body),
  update: (id, body) => api.put(`/contacts/${id}`, body),
};

export const ptpApi = {
  list: (params) => api.get('/ptp', { params }),
  get: (id) => api.get(`/ptp/${id}`),
  create: (body) => api.post('/ptp', body),
  recordPayment: (id, actualPaidAmount) => api.patch(`/ptp/${id}/payment`, null, { params: { actualPaidAmount } }),
  reschedule: (id, commitmentDate) => api.patch(`/ptp/${id}/reschedule`, null, { params: { commitmentDate } }),
};

export const borrowerContactApi = {
  list: (params) => api.get('/borrower-contacts', { params }),
  byAccount: (accountId) => api.get(`/borrower-contacts/account/${accountId}`),
  create: (body) => api.post('/borrower-contacts', body),
  update: (id, body) => api.put(`/borrower-contacts/${id}`, body),
  remove: (id) => api.delete(`/borrower-contacts/${id}`),
};

export const visitApi = {
  list: (params) => api.get('/visits', { params }),
  myVisits: () => api.get('/visits/my-visits'),
  schedule: (body) => api.post('/visits', body),
  complete: (id, body) => api.patch(`/visits/${id}/complete`, body),
  markMissed: (id) => api.patch(`/visits/${id}/missed`),
};

export const assetVerificationApi = {
  list: (params) => api.get('/asset-verifications', { params }),
  byVisit: (visitId) => api.get(`/asset-verifications/visit/${visitId}`),
  create: (body) => api.post('/asset-verifications', body),
  update: (id, body) => api.put(`/asset-verifications/${id}`, body),
};

export const settlementApi = {
  list: (params) => api.get('/settlements', { params }),
  get: (id) => api.get(`/settlements/${id}`),
  create: (body) => api.post('/settlements', body),
  update: (id, body) => api.put(`/settlements/${id}`, body),
  submit: (id) => api.patch(`/settlements/${id}/submit`),
  decide: (id, level, body) => api.post(`/settlements/${id}/decide`, body, { params: { level } }),
  markPaid: (id) => api.patch(`/settlements/${id}/mark-paid`),
  outstanding: () => api.get('/settlements/outstanding'),
  approvalQueue: () => api.get('/settlements/approval-queue'),
};

export const restructuringApi = {
  list: (params) => api.get('/restructuring', { params }),
  get: (id) => api.get(`/restructuring/${id}`),
  byAccount: (accountId) => api.get(`/restructuring/account/${accountId}`),
  create: (body) => api.post('/restructuring', body),
  update: (id, body) => api.put(`/restructuring/${id}`, body),
  approve: (id) => api.patch(`/restructuring/${id}/approve`),
  reject: (id) => api.patch(`/restructuring/${id}/reject`),
};

export const legalApi = {
  listCases: (params) => api.get('/legal/cases', { params }),
  getCase: (id) => api.get(`/legal/cases/${id}`),
  createCase: (body) => api.post('/legal/cases', body),
  updateCase: (id, body) => api.put(`/legal/cases/${id}`, body),
  listHearings: (caseId) => api.get(`/legal/cases/${caseId}/hearings`),
  addHearing: (body) => api.post('/legal/hearings', body),
  listOrders: () => api.get('/legal/orders'),
  issueOrder: (body) => api.post('/legal/orders', body),
  deleteOrder: (id) => api.delete(`/legal/orders/${id}`),
};

export const analyticsApi = {
  dashboard: () => api.get('/analytics/dashboard'),
  bucketDistribution: () => api.get('/analytics/bucket-distribution'),
  ptpMetrics: () => api.get('/analytics/ptp-metrics'),
  settlementMetrics: () => api.get('/analytics/settlement-metrics'),
  recoveryRate: () => api.get('/analytics/recovery-rate'),
  cashCollected: () => api.get('/analytics/cash-collected'),
  fieldVisitSuccess: () => api.get('/analytics/field-visit-success'),
  legalConversion: () => api.get('/analytics/legal-conversion'),
  generateReport: (scope) => api.post('/analytics/reports/generate', null, { params: { scope } }),
  listReports: (params) => api.get('/analytics/reports', { params }),
};

export const notificationApi = {
  list: (params) => api.get('/notifications', { params }),
  unreadCount: () => api.get('/notifications/unread-count'),
  markRead: (id) => api.patch(`/notifications/${id}/read`),
  dismiss: (id) => api.patch(`/notifications/${id}/dismiss`),
  markAllRead: () => api.patch('/notifications/read-all'),
};
