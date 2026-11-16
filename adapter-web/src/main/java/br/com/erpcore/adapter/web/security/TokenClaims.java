package br.com.erpcore.adapter.web.security;

/** Nomes das afirmações do token de acesso que o servidor lê (o mesmo nome que a infraestrutura escreve). */
public final class TokenClaims {

    public static final String COMPANY = "cid";
    public static final String ROLE = "role";

    private TokenClaims() {
    }
}
