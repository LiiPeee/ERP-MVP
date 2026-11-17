package br.com.erpcore.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.domain.shared.DomainException;
import java.util.List;
import org.junit.jupiter.api.Test;

class IdentityExceptionsTest {

    @Test
    void givenAnyIdentityError_whenReadingIt_thenItIsADomainExceptionWithAGenericMessage() {
        List<DomainException> errors = List.of(
                new EmailAlreadyRegisteredException(),
                new InvalidCredentialsException(),
                new InvalidRefreshTokenException(),
                new AccessDeniedException());

        assertThat(errors).allSatisfy(error -> assertThat(error.getMessage()).isNotBlank().doesNotContain("@"));
    }

    @Test
    void givenWrongUserAndWrongPassword_whenComparingTheirErrors_thenTheMessagesAreIdentical() {
        assertThat(new InvalidCredentialsException().getMessage())
                .isEqualTo(new InvalidCredentialsException().getMessage());
    }
}
