package br.com.erpcore.application.identity;

import java.util.UUID;

public record CurrentContext(UUID userId, String email, String displayName, UUID companyId, String companyName,
                             String role) {

    @Override
    public String toString() {
        return "CurrentContext[redacted]";
    }
}
