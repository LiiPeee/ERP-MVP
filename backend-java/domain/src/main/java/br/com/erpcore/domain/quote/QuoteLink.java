package br.com.erpcore.domain.quote;

import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.TokenHash;
import java.time.Instant;
import java.util.Optional;

/**
 * O link público de um orçamento. Do token só existe o hash. Abrir o link (consultar) não gasta nada: só a
 * decisão o consome, uma vez. O link vale até o fim do prazo do orçamento.
 */
public final class QuoteLink {

    private final QuoteId quoteId;
    private final CompanyId companyId;
    private final TokenHash tokenHash;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private Instant decidedAt;

    private QuoteLink(QuoteId quoteId, CompanyId companyId, TokenHash tokenHash, Instant issuedAt, Instant expiresAt) {
        this.quoteId = quoteId;
        this.companyId = companyId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public static QuoteLink issue(QuoteId quoteId, CompanyId companyId, TokenHash tokenHash, Instant now,
                                  Instant expiresAt) {
        return new QuoteLink(quoteId, companyId, tokenHash, now, expiresAt);
    }

    /** Pode ser aberto para consulta (inclusive depois da decisão, para mostrar o resultado). */
    public boolean isActiveAt(Instant now) {
        return now.isBefore(expiresAt);
    }

    /** Ainda pode receber a decisão do cliente. */
    public boolean canDecideAt(Instant now) {
        return decidedAt == null && isActiveAt(now);
    }

    public void markDecided(Instant now) {
        if (decidedAt == null) {
            decidedAt = now;
        }
    }

    public QuoteId quoteId() {
        return quoteId;
    }

    public CompanyId companyId() {
        return companyId;
    }

    public TokenHash tokenHash() {
        return tokenHash;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Optional<Instant> decidedAt() {
        return Optional.ofNullable(decidedAt);
    }

    @Override
    public String toString() {
        return "QuoteLink[quoteId=" + quoteId.value() + "]";
    }
}
