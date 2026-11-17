package br.com.erpcore.application.quote.port.out;

public record NewQuoteLinkSecret(String value, String hash) {

    @Override
    public String toString() {
        return "NewQuoteLinkSecret[redacted]";
    }
}
