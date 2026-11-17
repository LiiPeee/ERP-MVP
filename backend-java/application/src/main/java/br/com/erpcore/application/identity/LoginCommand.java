package br.com.erpcore.application.identity;

public record LoginCommand(String email, String password, String correlationId) {

    @Override
    public String toString() {
        return "LoginCommand[redacted]";
    }
}
