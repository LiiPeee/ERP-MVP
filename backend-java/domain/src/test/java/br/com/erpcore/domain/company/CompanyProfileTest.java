package br.com.erpcore.domain.company;

import static br.com.erpcore.domain.shared.DomainValidationAssertions.assertRejectedWith;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.domain.shared.PhoneNumber;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompanyProfileTest {

    private static final CompanyId COMPANY = new CompanyId(UUID.fromString("00000000-0000-0000-0000-0000000000c1"));

    private static CompanyProfile profile(String displayName, String phone, String address, String hours, int validityDays,
                                          String signature) {
        return CompanyProfile.of(COMPANY, displayName, phone == null ? null : PhoneNumber.of(phone), address, hours,
                validityDays, signature);
    }

    @Test
    void givenACompany_whenBuildingTheDefaultProfile_thenItUsesTheCompanyNameAndSevenDaysOfValidity() {
        Company company = new Company(COMPANY, CompanyName.of("Oficina Fictícia ME"), Instant.parse("2026-07-01T12:00:00Z"));

        CompanyProfile profile = CompanyProfile.defaultFor(company);

        assertThat(profile.displayName()).isEqualTo("Oficina Fictícia ME");
        assertThat(profile.defaultValidityDays()).isEqualTo(CompanyProfile.DEFAULT_VALIDITY_DAYS).isEqualTo(7);
        assertThat(profile.businessPhone()).isEmpty();
        assertThat(profile.address()).isEmpty();
        assertThat(profile.openingHours()).isEmpty();
        assertThat(profile.signatureMessage()).isEmpty();
    }

    @Test
    void givenCompleteData_whenCreating_thenTheTextsAreTrimmed() {
        CompanyProfile profile = profile("  Oficina do Zé Fictício ", "11987654321", " Rua Fictícia, 100 ", " Seg a sex, 8h às 18h ", 10,
                " Obrigado pela preferência! ");

        assertThat(profile.displayName()).isEqualTo("Oficina do Zé Fictício");
        assertThat(profile.businessPhone()).contains(PhoneNumber.of("11987654321"));
        assertThat(profile.address()).isEqualTo("Rua Fictícia, 100");
        assertThat(profile.openingHours()).isEqualTo("Seg a sex, 8h às 18h");
        assertThat(profile.signatureMessage()).isEqualTo("Obrigado pela preferência!");
    }

    @Test
    void givenBlankOptionalTexts_whenCreating_thenTheyAreEmpty() {
        CompanyProfile profile = profile("Oficina", null, "  ", null, 7, "");

        assertThat(profile.address()).isEmpty();
        assertThat(profile.openingHours()).isEmpty();
        assertThat(profile.signatureMessage()).isEmpty();
    }

    @Test
    void givenBlankOrTooLongDisplayName_whenCreating_thenItIsRejected() {
        assertRejectedWith(() -> profile(" ", null, null, null, 7, null), "displayName", "REQUIRED");
        assertRejectedWith(() -> profile("a".repeat(CompanyProfile.DISPLAY_NAME_MAX_LENGTH + 1), null, null, null, 7, null),
                "displayName", "TOO_LONG");
    }

    @Test
    void givenTooLongOptionalTexts_whenCreating_thenTheyAreRejected() {
        assertRejectedWith(() -> profile("Oficina", null, "a".repeat(CompanyProfile.ADDRESS_MAX_LENGTH + 1), null, 7, null),
                "address", "TOO_LONG");
        assertRejectedWith(() -> profile("Oficina", null, null, "a".repeat(CompanyProfile.HOURS_MAX_LENGTH + 1), 7, null),
                "openingHours", "TOO_LONG");
        assertRejectedWith(() -> profile("Oficina", null, null, null, 7, "a".repeat(CompanyProfile.SIGNATURE_MAX_LENGTH + 1)),
                "signatureMessage", "TOO_LONG");
    }

    @Test
    void givenAValidityOutsideTheRange_whenCreating_thenItIsOutOfRange() {
        assertRejectedWith(() -> profile("Oficina", null, null, null, 0, null), "defaultValidityDays", "OUT_OF_RANGE");
        assertRejectedWith(() -> profile("Oficina", null, null, null, 91, null), "defaultValidityDays", "OUT_OF_RANGE");
        assertThat(profile("Oficina", null, null, null, 90, null).defaultValidityDays()).isEqualTo(90);
    }
}
