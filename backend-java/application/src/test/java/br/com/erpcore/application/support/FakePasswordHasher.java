package br.com.erpcore.application.support;

import br.com.erpcore.application.identity.port.out.PasswordHasher;

/** Hash de brinquedo (a senha invertida com prefixo): determinístico, e o resultado nunca contém a senha. */
public final class FakePasswordHasher implements PasswordHasher {

    private static final String PREFIX = "fake-hash:";

    private int hashCalls;
    private int placeholderVerifications;

    @Override
    public String hash(String rawPassword) {
        hashCalls++;
        return PREFIX + new StringBuilder(rawPassword).reverse();
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        return (PREFIX + new StringBuilder(rawPassword).reverse()).equals(hash);
    }

    @Override
    public void spendVerificationTime(String rawPassword) {
        placeholderVerifications++;
    }

    public int hashCalls() {
        return hashCalls;
    }

    public int placeholderVerifications() {
        return placeholderVerifications;
    }
}
