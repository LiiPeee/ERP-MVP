package br.com.erpcore.adapter.web.auth;

import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/** Corpos das rotas de autenticação. Só o formato e o tamanho são conferidos aqui; as regras são do domínio. */
final class AuthDtos {

    private static final int MAX_NAME = 120;
    private static final int MAX_EMAIL = 254;
    private static final int MAX_PASSWORD = 128;

    private AuthDtos() {
    }

    record RegisterRequest(@Size(max = MAX_NAME) String companyName, @Size(max = MAX_NAME) String ownerName,
                           @Size(max = MAX_EMAIL) String email, @Size(max = MAX_PASSWORD) String password) {

        @Override
        public String toString() {
            return "RegisterRequest[redacted]";
        }
    }

    record LoginRequest(@Size(max = MAX_EMAIL) String email, @Size(max = MAX_PASSWORD) String password) {

        @Override
        public String toString() {
            return "LoginRequest[redacted]";
        }
    }

    record RegisterResponse(UUID companyId, UUID userId) {
    }

    /** O refresh token não vai no corpo: só no cookie HttpOnly. */
    record SessionResponse(String accessToken, Instant expiresAt) {

        @Override
        public String toString() {
            return "SessionResponse[redacted]";
        }
    }
}
