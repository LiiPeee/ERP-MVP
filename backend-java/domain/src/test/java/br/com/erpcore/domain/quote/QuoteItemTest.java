package br.com.erpcore.domain.quote;

import static br.com.erpcore.domain.shared.DomainValidationAssertions.assertRejectedWith;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.domain.shared.Money;
import br.com.erpcore.domain.shared.Quantity;
import org.junit.jupiter.api.Test;

class QuoteItemTest {

    @Test
    void givenValidData_whenCreating_thenTheDescriptionIsTrimmedAndTheLineTotalIsCalculated() {
        QuoteItem item = QuoteItem.of("  Pastilha de freio dianteira ", QuoteItemKind.PART, Quantity.of("2"), Money.of("89.90"));

        assertThat(item.description()).isEqualTo("Pastilha de freio dianteira");
        assertThat(item.lineTotal()).isEqualTo(Money.of("179.80"));
    }

    @Test
    void givenADecimalQuantity_whenCalculatingTheLine_thenItIsRoundedHalfUpToCents() {
        QuoteItem item = QuoteItem.of("Fluido", QuoteItemKind.PART, Quantity.of("2.5"), Money.of("19.99"));

        assertThat(item.lineTotal()).isEqualTo(Money.of("49.98"));
    }

    @Test
    void givenBlankOrTooLongDescription_whenCreating_thenItIsRejected() {
        assertRejectedWith(() -> QuoteItem.of(" ", QuoteItemKind.LABOR, Quantity.of("1"), Money.of("10")), "description", "REQUIRED");
        assertRejectedWith(() -> QuoteItem.of(null, QuoteItemKind.LABOR, Quantity.of("1"), Money.of("10")), "description", "REQUIRED");
        assertRejectedWith(() -> QuoteItem.of("a".repeat(QuoteItem.DESCRIPTION_MAX_LENGTH + 1), QuoteItemKind.LABOR,
                Quantity.of("1"), Money.of("10")), "description", "TOO_LONG");
    }

    @Test
    void givenAZeroUnitPrice_whenCreating_thenItIsAcceptedAsACourtesyItem() {
        assertThat(QuoteItem.of("Cortesia", QuoteItemKind.LABOR, Quantity.of("1"), Money.zero()).lineTotal().isZero()).isTrue();
    }

    @Test
    void givenANegativeOrTooHighUnitPrice_whenCreating_thenItIsOutOfRange() {
        assertRejectedWith(() -> QuoteItem.of("x", QuoteItemKind.PART, Quantity.of("1"), Money.of("-0.01")), "unitPrice", "OUT_OF_RANGE");
        assertRejectedWith(() -> QuoteItem.of("x", QuoteItemKind.PART, Quantity.of("1"),
                QuoteItem.MAX_UNIT_PRICE.add(Money.of("0.01"))), "unitPrice", "OUT_OF_RANGE");
        assertThat(QuoteItem.of("x", QuoteItemKind.PART, Quantity.of("1"), QuoteItem.MAX_UNIT_PRICE).unitPrice())
                .isEqualTo(QuoteItem.MAX_UNIT_PRICE);
    }
}
