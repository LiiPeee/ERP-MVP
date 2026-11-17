package br.com.erpcore.adapter.persistence.quote;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Uma linha de uma revisão dos itens. Só é inserida: revisar grava um conjunto novo e nunca altera o antigo. */
@Entity
@Table(name = "quote_item")
public class QuoteItemJpaEntity {

    @EmbeddedId
    private Key key;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "description", nullable = false, updatable = false)
    private String description;

    @Column(name = "kind", nullable = false, updatable = false)
    private String kind;

    @Column(name = "quantity", nullable = false, updatable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, updatable = false)
    private BigDecimal unitPrice;

    protected QuoteItemJpaEntity() {
    }

    public QuoteItemJpaEntity(UUID quoteId, int revision, int position, UUID companyId, String description, String kind,
                              BigDecimal quantity, BigDecimal unitPrice) {
        this.key = new Key(quoteId, revision, position);
        this.companyId = companyId;
        this.description = description;
        this.kind = kind;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getDescription() {
        return description;
    }

    public String getKind() {
        return kind;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    @Embeddable
    public static class Key implements Serializable {

        @Column(name = "quote_id")
        private UUID quoteId;

        @Column(name = "revision")
        private int revision;

        @Column(name = "position")
        private int position;

        protected Key() {
        }

        public Key(UUID quoteId, int revision, int position) {
            this.quoteId = quoteId;
            this.revision = revision;
            this.position = position;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && quoteId.equals(key.quoteId) && revision == key.revision && position == key.position;
        }

        @Override
        public int hashCode() {
            return Objects.hash(quoteId, revision, position);
        }
    }
}
