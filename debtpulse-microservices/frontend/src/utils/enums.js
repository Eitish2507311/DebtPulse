// Enum option lists mirrored from the backend (used to populate selects). Keeping these
// in one place means a new enum value is a one-line change.
export const ENUMS = {
  Role: ['COLLECTIONS_AGENT', 'FIELD_OFFICER', 'LEGAL_OFFICER', 'SETTLEMENT_OFFICER',
    'L1_APPROVER', 'L2_APPROVER', 'L3_APPROVER', 'PORTFOLIO_MANAGER', 'ADMIN'],
  UserStatus: ['ACTIVE', 'INACTIVE', 'SUSPENDED'],
  AccountStatus: ['ACTIVE', 'SETTLED', 'LEGAL', 'WRITEOFF', 'CLOSED'],
  DpdBucket: ['X30', 'X60', 'X90', 'X120', 'X180', 'NPA', 'WRITEOFF'],
  AssetType: ['PROPERTY', 'VEHICLE', 'GOLD', 'MACHINERY', 'STOCKS'],
  VerificationStatus: ['UNVERIFIED', 'VERIFIED', 'DISPUTED', 'NOT_TRACEABLE'],
  AllocationStrategy: ['ROUND_ROBIN', 'BRANCH_BASED', 'LEAST_LOADED'],
  ContactChannel: ['CALL', 'SMS', 'EMAIL', 'VISIT', 'LETTER'],
  ContactOutcome: ['CONNECTED', 'NOT_CONNECTED', 'BUSY_TONE', 'NUMBER_INVALID', 'DISCONNECTED', 'REFUSED'],
  PtpStatus: ['ACTIVE', 'KEPT', 'BROKEN', 'PARTIAL', 'RESCHEDULED'],
  BorrowerContactType: ['PRIMARY', 'ALTERNATE', 'REFERENCE', 'EMPLOYER'],
  BorrowerContactStatus: ['ACTIVE', 'INVALID', 'DO_NOT_CONTACT'],
  AssetCondition: ['GOOD', 'FAIR', 'POOR', 'NOT_FOUND'],
  VisitStatus: ['SCHEDULED', 'COMPLETED', 'MISSED', 'BORROWER_ABSENT', 'REFUSED'],
  SettlementStatus: ['DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'PAID', 'EXPIRED'],
  ApprovalLevel: ['L1', 'L2', 'L3'],
  ApprovalDecision: ['APPROVE', 'REJECT'],
  RestructuringStatus: ['DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'ACTIVE', 'DEFAULTED'],
  CaseType: ['CIVIL_SUIT', 'ARBITRATION_PROCEEDING', 'DRT_FILING', 'CRIMINAL_COMPLAINT', 'SARFAESI_ACTION'],
  CaseStatus: ['FILED', 'PENDING', 'HEARING_SCHEDULED', 'DECREED', 'DISMISSED', 'WITHDRAWN', 'SETTLED'],
  HearingOutcome: ['ADJOURNED', 'PARTIALLY_HEARD', 'ORDER_PASSED', 'DISMISSED', 'SETTLED'],
  OrderType: ['ATTACHMENT_ORDER', 'GARNISHEE_ORDER', 'EVICTION_ORDER', 'AUCTION_ORDER'],
  OrderStatus: ['ISSUED', 'IN_EXECUTION', 'EXECUTED', 'CHALLENGED', 'VACATED'],
  NotifCategory: ['PTP', 'FIELD_VISIT', 'SETTLEMENT', 'LEGAL', 'ESCALATION', 'PORTFOLIO'],
};

// Map a status string to a semantic pill colour class.
const GREEN = ['ACTIVE', 'VERIFIED', 'CONNECTED', 'KEPT', 'APPROVED', 'PAID', 'COMPLETED', 'EXECUTED', 'SETTLED', 'DECREED'];
const RED = ['BROKEN', 'REJECTED', 'MISSED', 'REFUSED', 'DEFAULTED', 'NOT_TRACEABLE', 'NOT_FOUND', 'INVALID', 'SUSPENDED', 'WRITEOFF', 'EXPIRED', 'DISPUTED', 'DO_NOT_CONTACT', 'DISMISSED', 'NUMBER_INVALID', 'VACATED', 'CHALLENGED'];
const AMBER = ['PENDING_APPROVAL', 'PARTIAL', 'RESCHEDULED', 'PENDING', 'HEARING_SCHEDULED', 'SCHEDULED', 'UNVERIFIED', 'ISSUED', 'IN_EXECUTION', 'DRAFT', 'ADJOURNED', 'PARTIALLY_HEARD', 'BORROWER_ABSENT', 'BUSY_TONE', 'NOT_CONNECTED', 'FILED'];

export function statusClass(status) {
  if (!status) return 's-gray';
  const s = String(status).toUpperCase();
  if (GREEN.includes(s)) return 's-green';
  if (RED.includes(s)) return 's-red';
  if (AMBER.includes(s)) return 's-amber';
  return 's-blue';
}

export const bucketClass = (b) =>
  ['NPA', 'WRITEOFF', 'X180'].includes(b) ? 's-red' : ['X120', 'X90'].includes(b) ? 's-amber' : 's-blue';
