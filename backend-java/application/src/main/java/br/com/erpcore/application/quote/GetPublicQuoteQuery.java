package br.com.erpcore.application.quote;

/** O token como o cliente o apresentou, no endereço do link. */
public record GetPublicQuoteQuery(String token) {

    @Override
    public String toString() {
        return "GetPublicQuoteQuery[redacted]";
    }
}
