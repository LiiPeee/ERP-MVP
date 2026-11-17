package br.com.erpcore.application.identity;

public record LogoutCommand(String refreshToken, String correlationId) {

    @Override
    public String toString() {
        return "LogoutCommand[redacted]";
    }
}
