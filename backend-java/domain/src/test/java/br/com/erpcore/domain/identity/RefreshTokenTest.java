package br.com.erpcore.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.domain.company.CompanyId;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    private static final Instant NOW = Instant.parse("2026-02-01T10:00:00Z");
    private static final SessionPolicy POLICY = new SessionPolicy(Duration.ofDays(7), Duration.ofDays(30));
    private static final UserId USER = new UserId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
    private static final CompanyId COMPANY = new CompanyId(UUID.fromString("00000000-0000-0000-0000-0000000000c1"));
    private static final TokenFamilyId FAMILY = new TokenFamilyId(UUID.fromString("00000000-0000-0000-0000-0000000000f1"));
    private static final RefreshTokenId FIRST_ID = new RefreshTokenId(UUID.fromString("00000000-0000-0000-0000-000000000011"));
    private static final RefreshTokenId SECOND_ID = new RefreshTokenId(UUID.fromString("00000000-0000-0000-0000-000000000012"));
    private static final TokenHash FIRST_HASH = new TokenHash("hash-sintetico-1");
    private static final TokenHash SECOND_HASH = new TokenHash("hash-sintetico-2");

    private static RefreshToken firstToken() {
        return RefreshToken.first(FIRST_ID, USER, COMPANY, FAMILY, FIRST_HASH, NOW, POLICY);
    }

    @Test
    void givenANewSession_whenIssuingTheFirstToken_thenItIsActiveAndExpiresAfterTheConfiguredLifetime() {
        RefreshToken token = firstToken();

        assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        assertThat(token.familyStartedAt()).isEqualTo(NOW);
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.isExpiredAt(NOW)).isFalse();
    }

    @Test
    void givenAToken_whenCheckingAtTheExpiryInstant_thenItIsExpired() {
        RefreshToken token = firstToken();

        assertThat(token.isExpiredAt(token.expiresAt().minusSeconds(1))).isFalse();
        assertThat(token.isExpiredAt(token.expiresAt())).isTrue();
    }

    @Test
    void givenAnActiveToken_whenRevoking_thenItIsRevokedAtThatInstant() {
        RefreshToken token = firstToken();

        token.revoke(NOW.plusSeconds(5));

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.revokedAt()).contains(NOW.plusSeconds(5));
    }

    @Test
    void givenARevokedToken_whenRevokingAgain_thenTheOriginalInstantIsKept() {
        RefreshToken token = firstToken();
        token.revoke(NOW.plusSeconds(5));

        token.revoke(NOW.plusSeconds(60));

        assertThat(token.revokedAt()).contains(NOW.plusSeconds(5));
    }

    @Test
    void givenAnActiveToken_whenRotating_thenTheOldOneIsRevokedAndTheNewOneKeepsTheFamily() {
        RefreshToken old = firstToken();
        Instant later = NOW.plus(Duration.ofDays(1));

        RefreshToken rotated = old.rotateTo(SECOND_ID, SECOND_HASH, later, POLICY);

        assertThat(old.revokedAt()).contains(later);
        assertThat(rotated.id()).isEqualTo(SECOND_ID);
        assertThat(rotated.tokenHash()).isEqualTo(SECOND_HASH);
        assertThat(rotated.familyId()).isEqualTo(FAMILY);
        assertThat(rotated.userId()).isEqualTo(USER);
        assertThat(rotated.companyId()).isEqualTo(COMPANY);
        assertThat(rotated.isRevoked()).isFalse();
        assertThat(rotated.expiresAt()).isEqualTo(later.plus(Duration.ofDays(7)));
    }

    @Test
    void givenASessionRenewedEverySixDays_whenRotatingNearItsAbsoluteLimit_thenTheNewTokenNeverOutlivesTheFamilyLimit() {
        RefreshToken current = firstToken();
        for (int day : new int[] {6, 12, 18}) {
            current = current.rotateTo(SECOND_ID, SECOND_HASH, NOW.plus(Duration.ofDays(day)), POLICY);
            assertThat(current.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(day + 7)));
        }

        RefreshToken nearTheEnd = current.rotateTo(SECOND_ID, SECOND_HASH, NOW.plus(Duration.ofDays(24)), POLICY);

        assertThat(nearTheEnd.familyStartedAt()).isEqualTo(NOW);
        assertThat(nearTheEnd.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
    }

    @Test
    void givenARevokedToken_whenRotating_thenItIsRefused() {
        RefreshToken token = firstToken();
        token.revoke(NOW);

        assertThatThrownBy(() -> token.rotateTo(SECOND_ID, SECOND_HASH, NOW, POLICY))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givenAnExpiredToken_whenRotating_thenItIsRefused() {
        RefreshToken token = firstToken();
        Instant afterExpiry = token.expiresAt().plusSeconds(1);

        assertThatThrownBy(() -> token.rotateTo(SECOND_ID, SECOND_HASH, afterExpiry, POLICY))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givenPersistedState_whenRestoring_thenEveryFieldIsPreserved() {
        Instant revokedAt = NOW.plusSeconds(9);

        RefreshToken restored = RefreshToken.restore(FIRST_ID, USER, COMPANY, FAMILY, FIRST_HASH,
                NOW, NOW.plus(Duration.ofDays(7)), NOW, revokedAt);

        assertThat(restored.revokedAt()).contains(revokedAt);
        assertThat(restored.issuedAt()).isEqualTo(NOW);
        assertThat(restored.isRevoked()).isTrue();
    }
}
