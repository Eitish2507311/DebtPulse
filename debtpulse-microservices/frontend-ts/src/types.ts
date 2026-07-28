// ==========================================================================
// Domain + API types mirrored from the DebtPulse backend DTOs.
// ==========================================================================
import type { ReactNode } from 'react';

export type Role =
  | 'ADMIN' | 'COLLECTIONS_AGENT' | 'FIELD_OFFICER' | 'LEGAL_OFFICER'
  | 'SETTLEMENT_OFFICER' | 'L1_APPROVER' | 'L2_APPROVER' | 'L3_APPROVER' | 'PORTFOLIO_MANAGER';

export interface AuthResponse {
  message: string; token: string; refreshToken?: string; expiresIn?: number;
  userId: string; role: Role; name: string; branchId: string;
}

export interface SessionUser {
  userId: string; role: Role; name: string; branchId: string; email: string;
}

export interface PageResponse<T> {
  content: T[]; page: number; size: number; totalElements: number; totalPages: number; last?: boolean;
}

export interface AppError {
  status: number;
  message: string;
  ruleCode?: string;
  fieldErrors: Record<string, string>;
}

export interface UserDto {
  userId: string; fullName: string; email: string; phone?: string;
  role: Role; branchId?: string; status: string; createdAt?: string;
}

export interface AuditLog {
  auditId: string; userId: string; action: string; entityType: string;
  recordId: string; sourceService?: string; timestamp: string;
}

export interface Account {
  accountId: string; loanRef: string; borrowerName: string; phone?: string; address?: string;
  branchId?: string; principalAmount: number; totalOverdue: number; dpd: number;
  bucket: string; status: string; assignedAgentId?: string; daysInCurrentBucket?: number;
}

export interface CollateralAsset {
  assetId: string; accountId: string; assetType: string; description?: string;
  estimatedValue: number; verificationStatus: string; lastVerifiedDate?: string;
}

export interface AllocationRule {
  ruleId: string; name: string; strategy: string; bucket?: string; targetRole: string;
  daysInBucketThreshold?: number | null; minDpd?: number | null; gracePeriodDays?: number | null;
  capacityLimit?: number | null; branchId?: string; priority?: number | null;
  autoEscalate?: boolean; active?: boolean;
}

export interface ContactAttempt {
  contactId: string; accountId: string; agentId?: string; contactDate?: string;
  channel: string; outcome: string; notes?: string; status?: string;
}

export interface Ptp {
  ptpId: string; accountId: string; agentId?: string; ptpDate: string; ptpAmount: number;
  commitmentDate: string; actualPaidAmount?: number; status: string;
}

export interface BorrowerContact {
  contactRecordId: string; accountId: string; contactType: string; name: string;
  phone: string; relationship?: string; status?: string;
}

export interface FieldVisit {
  visitId: string; accountId: string; officerId: string; scheduledDate: string; visitDate?: string;
  borrowerMet?: boolean; assetSighted?: boolean; outcomeSummary?: string; nextActionRequired?: string; status: string;
}

export interface AssetVerification {
  reportId: string; visitId: string; assetId: string; physicalCondition?: string; condition?: string;
  currentLocation?: string; estimatedRealisableValue?: number; realisableValue?: number;
  remarks?: string; verifiedById?: string; verificationDate?: string;
}

export interface Settlement {
  proposalId: string; accountId: string; officerId?: string; totalOutstanding: number; settlementAmount: number;
  haircutPercent?: number; paymentDeadline: string; approvalLevel: string; approvedById?: string; status: string;
  requiredApprovalChain?: string[]; currentStep?: string | null; notes?: string;
}

export interface Restructuring {
  restructureId: string; accountId: string; officerId?: string; revisedTenure: number; revisedEmi: number;
  waiverAmount: number; startDate: string; approvedById?: string; status: string;
}

export interface LegalCase {
  caseId: string; accountId: string; legalOfficerId?: string; caseType: string; filingDate: string;
  courtName: string; caseNumber: string; status: string;
}

export interface CourtHearing {
  hearingId: string; caseId: string; hearingDate: string; hearingOutcome: string;
  nextHearingDate?: string; notes?: string;
}

export interface RecoveryOrder {
  orderId: string; caseId: string; orderType: string; issuedDate: string; executionDeadline: string; status: string;
}

export interface RecoveryReport { reportId: string; scope: string; metrics?: string; generatedDate: string; }

export interface Notification {
  notificationId: string; userId: string; message: string; category: string; status: string; createdDate: string;
}

export type Metrics = Record<string, unknown>;

// A generic table column definition used by <DataTable/>.
export interface Column<T> {
  key: string;
  header: string;
  className?: string;
  render?: (row: T) => ReactNode;
}

// Option for a <select> — either a bare string or a labelled value.
export type FieldOption = string | { value: string; label: string };
