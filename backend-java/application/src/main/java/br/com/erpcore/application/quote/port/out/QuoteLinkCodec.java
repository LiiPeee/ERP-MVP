package br.com.erpcore.application.quote.port.out;

public interface QuoteLinkCodec {

    /** Cria um token de link novo, aleatório e imprevisível, junto com o seu hash. */
    NewQuoteLinkSecret generate();

    /** Calcula o hash de um token apresentado, para procurá-lo sem nunca guardar o original. */
    String hash(String presentedToken);
}
