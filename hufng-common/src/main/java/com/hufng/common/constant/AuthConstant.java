package com.hufng.common.constant;

/**
 * Permission-related constant definitions
 * Created by macro on 2020/6/19.
 */
public interface AuthConstant {

    /**
     * JWT authority prefix
     */
    String AUTHORITY_PREFIX = "ROLE_";

    /**
     * JWT authority claim attribute name
     */
    String AUTHORITY_CLAIM_NAME = "authorities";

    /**
     * Admin backend client_id
     */
    String ADMIN_CLIENT_ID = "admin-app";

    /**
     * Frontend storefront client_id
     */
    String PORTAL_CLIENT_ID = "portal-app";

    /**
     * Admin backend API path pattern
     */
    String ADMIN_URL_PATTERN = "/mall-admin/**";

    /**
     * Redis cached permission rules (path -> resource)
     */
    String PATH_RESOURCE_MAP = "auth:pathResourceMap";

    /**
     * Authentication information HTTP request header
     */
    String JWT_TOKEN_HEADER = "Authorization";

    /**
     * JWT token prefix
     */
    String JWT_TOKEN_PREFIX = "Bearer ";

    /**
     * User information HTTP request header
     */
    String USER_TOKEN_HEADER = "user";

    /**
     * Member information stored in the sa-token session
     */
    String STP_MEMBER_INFO = "memberInfo";

    /**
     * Admin user information stored in the sa-token session
     */
    String STP_ADMIN_INFO = "adminInfo";

    String JWT_USER_LOGIN = "user:login";

    /**
     * Redis key for a cached logged-in user by token — shared format between
     * hufng-auth (writes it: UserMemberCacheService) and hufng-gateway (reads it:
     * AuthGlobalFilter, to reject a revoked/logged-out token before it reaches
     * hufng-auth). Both MUST build the key the same way, or the gateway's lookup
     * will never find what hufng-auth wrote.
     */
    static String buildUserLoginKey(String redisDatabase, String token) {
        return redisDatabase + ":" + JWT_USER_LOGIN + ":" + token;
    }
}
