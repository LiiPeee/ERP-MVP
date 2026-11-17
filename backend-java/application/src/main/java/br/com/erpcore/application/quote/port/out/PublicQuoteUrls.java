package br.com.erpcore.application.quote.port.out;

public interface PublicQuoteUrls {

    /** O endereço público que o cliente abre, a partir do token. A base vem da configuração. */
    String forToken(String token);
}
