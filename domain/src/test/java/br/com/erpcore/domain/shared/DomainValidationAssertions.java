package br.com.erpcore.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

public final class DomainValidationAssertions {

    private DomainValidationAssertions() {
    }

    public static void assertRejectedWith(ThrowingCallable action, String field, String code) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(DomainValidationException.class, exception -> {
                    assertThat(exception.field()).isEqualTo(field);
                    assertThat(exception.code()).isEqualTo(code);
                });
    }
}
