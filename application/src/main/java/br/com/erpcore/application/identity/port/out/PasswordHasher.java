package br.com.erpcore.application.identity.port.out;

/** Usa só texto simples na assinatura, para que o adaptador de infraestrutura não dependa do domínio. */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);

    /**
     * Gasta o mesmo tempo de uma verificação real. Chamado quando não há hash para comparar (usuário
     * inexistente ou conta bloqueada), para que o tempo de resposta não revele se a conta existe.
     */
    void spendVerificationTime(String rawPassword);
}
