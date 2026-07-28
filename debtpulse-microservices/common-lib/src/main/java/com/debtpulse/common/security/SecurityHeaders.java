package com.debtpulse.common.security;

/**
 * Names of the trusted identity headers the API Gateway injects after validating the JWT.
 * Downstream services read these to rebuild the security context and Feign propagates them
 * on inter-service calls.
 */
public final class SecurityHeaders {

    private SecurityHeaders() {}

    public static final String USER_ID = "X-Auth-UserId";
    public static final String ROLE = "X-Auth-Role";
    public static final String BRANCH_ID = "X-Auth-BranchId";
    public static final String NAME = "X-Auth-Name";
}
