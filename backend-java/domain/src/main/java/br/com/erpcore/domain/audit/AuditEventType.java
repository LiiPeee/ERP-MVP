package br.com.erpcore.domain.audit;

public enum AuditEventType {
    REGISTRATION,
    LOGIN,
    TOKEN_REFRESH,
    TOKEN_REUSE_DETECTED,
    LOGOUT,
    CONTEXT_VIEWED
}
