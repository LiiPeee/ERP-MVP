package br.com.erpcore.application.identity.port.out;

public record NewRefreshSecret(String value, String hash) {

    @Override
    public String toString() {
        return "NewRefreshSecret[redacted]";
    }
}
