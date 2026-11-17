package br.com.erpcore.application.shared.port.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.domain.company.CompanyId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FieldContextTest {

    private static final CompanyId COMPANY = new CompanyId(UUID.fromString("00000000-0000-0000-0000-0000000000c1"));

    @Test
    void givenACompanyAndAField_whenAskingForTheAssociatedData_thenBothArePartOfIt() {
        assertThat(new FieldContext(COMPANY, "contact.phone").associatedData())
                .isEqualTo("00000000-0000-0000-0000-0000000000c1|contact.phone");
    }

    @Test
    void givenNoCompany_whenCreating_thenItIsRefused() {
        assertThatThrownBy(() -> new FieldContext(null, "contact.phone")).isInstanceOf(NullPointerException.class);
    }

    @Test
    void givenNoFieldOrABlankOne_whenCreating_thenItIsRefused() {
        assertThatThrownBy(() -> new FieldContext(COMPANY, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FieldContext(COMPANY, "  ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenTheSameCompanyAndField_whenComparing_thenTheContextsAreEqual() {
        assertThat(new FieldContext(COMPANY, "vehicle.plate")).isEqualTo(new FieldContext(COMPANY, "vehicle.plate"));
    }
}
