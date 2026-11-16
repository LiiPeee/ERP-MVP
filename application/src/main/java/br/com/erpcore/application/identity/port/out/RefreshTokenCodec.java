package br.com.erpcore.application.identity.port.out;

public interface RefreshTokenCodec {

    /** Cria um refresh token novo, aleatório e imprevisível, junto com o seu hash. */
    NewRefreshSecret generate();

    /** Calcula o hash de um token apresentado pelo cliente, para procurá-lo sem nunca guardar o original. */
    String hash(String presentedToken);
}
