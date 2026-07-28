import type { Role } from '../types';

export const ROLES: Record<string, Role> = {
  ADMIN: 'ADMIN',
  COLLECTIONS_AGENT: 'COLLECTIONS_AGENT',
  FIELD_OFFICER: 'FIELD_OFFICER',
  LEGAL_OFFICER: 'LEGAL_OFFICER',
  SETTLEMENT_OFFICER: 'SETTLEMENT_OFFICER',
  L1_APPROVER: 'L1_APPROVER',
  L2_APPROVER: 'L2_APPROVER',
  L3_APPROVER: 'L3_APPROVER',
  PORTFOLIO_MANAGER: 'PORTFOLIO_MANAGER',
};

export const APPROVERS: Role[] = ['L1_APPROVER', 'L2_APPROVER', 'L3_APPROVER'];
export const ALL: Role[] = Object.values(ROLES);

export const ROLE_LABELS: Record<Role, string> = {
  ADMIN: 'Administrator',
  COLLECTIONS_AGENT: 'Collections Agent',
  FIELD_OFFICER: 'Field Officer',
  LEGAL_OFFICER: 'Legal Officer',
  SETTLEMENT_OFFICER: 'Settlement Officer',
  L1_APPROVER: 'L1 Approver',
  L2_APPROVER: 'L2 Approver',
  L3_APPROVER: 'L3 Approver',
  PORTFOLIO_MANAGER: 'Portfolio Manager',
};

export const ACCESS: Record<string, Role[]> = {
  dashboard: ALL,
  portfolio: ['ADMIN', 'COLLECTIONS_AGENT', 'PORTFOLIO_MANAGER'],
  collateral: ['ADMIN', 'FIELD_OFFICER', 'PORTFOLIO_MANAGER'],
  allocations: ['ADMIN', 'COLLECTIONS_AGENT', 'PORTFOLIO_MANAGER'],
  contact: ['ADMIN', 'COLLECTIONS_AGENT', 'PORTFOLIO_MANAGER'],
  field: ['ADMIN', 'FIELD_OFFICER', 'PORTFOLIO_MANAGER'],
  settlement: ['ADMIN', 'SETTLEMENT_OFFICER', 'PORTFOLIO_MANAGER', ...APPROVERS],
  legal: ['ADMIN', 'LEGAL_OFFICER', 'PORTFOLIO_MANAGER'],
  analytics: ['ADMIN', 'PORTFOLIO_MANAGER'],
  notifications: ALL,
  admin: ['ADMIN'],
  audit: ['ADMIN', 'PORTFOLIO_MANAGER'],
};

export const hasAny = (role: Role | undefined | null, allowed: Role[]): boolean =>
  !!role && allowed.includes(role);
