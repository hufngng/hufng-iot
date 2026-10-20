package com.hufng.common.dto;

/**
 * Minimal snapshot of a logged-in user, cached in Redis keyed by JWT token
 * (see AuthConstant.buildUserLoginKey). Deliberately NOT the full JPA entity
 * (com.hufng.auth.entity.UserMember) — that class only exists on hufng-auth's
 * classpath, so a cross-service reader (e.g. hufng-gateway) would fail to
 * deserialize it. This class lives in hufng-common so every service that
 * shares Redis can read it.
 *
 * Must stay a plain (non-final) class, not a record — BaseRedisConfig's
 * ObjectMapper uses activateDefaultTyping(..., NON_FINAL), which skips
 * embedding "@class" type info for final classes (records are implicitly
 * final), so a record would deserialize back as a raw LinkedHashMap instead
 * of this type.
 */
public class CachedUserSession {
    private Long userId;
    private String username;
    private String role;

    /**
     * Epoch millis of the ORIGINAL login (Google or local) — carried forward
     * unchanged across /v1/refresh calls (never reset to "now" on refresh).
     * UserMemberService.refreshToken uses this to enforce an absolute session
     * lifetime: once it's too old, refresh is refused and the user must go
     * through a real login again (which also re-syncs against Google — see
     * README, "1 vấn đề đối với oauth2 là khi user revoke quyền").
     */
    private Long loginAt;

    public CachedUserSession() {
    }

    public CachedUserSession(Long userId, String username, String role, Long loginAt) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.loginAt = loginAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getLoginAt() {
        return loginAt;
    }

    public void setLoginAt(Long loginAt) {
        this.loginAt = loginAt;
    }
}
