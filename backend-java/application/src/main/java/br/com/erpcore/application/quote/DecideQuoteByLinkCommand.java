package br.com.erpcore.application.quote;

/** Aprovar ou recusar pelo link. O motivo só vale na recusa e é opcional. */
public record DecideQuoteByLinkCommand(String token, String reason, String correlationId) {

    @Override
    public String toString() {
        return "DecideQuoteByLinkCommand[redacted]";
    }
}
