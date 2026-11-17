package br.com.erpcore.application.identity;

public record RefreshSessionCommand(String refreshToken, String correlationId) {

    @Override
    public String toString() {
        return "RefreshSessionCommand[redacted]";
    }
}
