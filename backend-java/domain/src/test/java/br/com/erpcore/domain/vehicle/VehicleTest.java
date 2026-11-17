package br.com.erpcore.domain.vehicle;

import static br.com.erpcore.domain.shared.DomainValidationAssertions.assertRejectedWith;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.contact.ContactId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VehicleTest {

    private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");
    private static final CompanyId COMPANY = new CompanyId(UUID.fromString("00000000-0000-0000-0000-0000000000c1"));
    private static final ContactId OWNER = new ContactId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));

    private static Vehicle vehicle(String brand, String model, int year, Integer odometerKm) {
        return Vehicle.register(new VehicleId(UUID.randomUUID()), COMPANY, OWNER, LicensePlate.of("ABC1234"), brand,
                model, year, odometerKm, NOW);
    }

    @Test
    void givenValidData_whenRegistering_thenTheVehicleBelongsToTheContactAndTheOdometerIsOptional() {
        Vehicle vehicle = vehicle("  Fiat ", "Uno", 2012, null);

        assertThat(vehicle.brand()).isEqualTo("Fiat");
        assertThat(vehicle.contactId()).isEqualTo(OWNER);
        assertThat(vehicle.odometerKm()).isEmpty();
        assertThat(vehicle.createdAt()).isEqualTo(NOW);
    }

    @Test
    void givenAnOdometer_whenRegistering_thenItIsKept() {
        assertThat(vehicle("Fiat", "Uno", 2012, 123_456).odometerKm()).hasValue(123_456);
    }

    @Test
    void givenBlankOrTooLongBrandOrModel_whenRegistering_thenItIsRejected() {
        assertRejectedWith(() -> vehicle(" ", "Uno", 2012, null), "brand", "REQUIRED");
        assertRejectedWith(() -> vehicle("Fiat", null, 2012, null), "model", "REQUIRED");
        assertRejectedWith(() -> vehicle("a".repeat(Vehicle.TEXT_MAX_LENGTH + 1), "Uno", 2012, null), "brand", "TOO_LONG");
        assertRejectedWith(() -> vehicle("Fiat", "a".repeat(Vehicle.TEXT_MAX_LENGTH + 1), 2012, null), "model", "TOO_LONG");
    }

    @Test
    void givenAYearOutsideTheRange_whenRegistering_thenItIsOutOfRange() {
        assertRejectedWith(() -> vehicle("Fiat", "Uno", Vehicle.MIN_YEAR - 1, null), "year", "OUT_OF_RANGE");
        assertRejectedWith(() -> vehicle("Fiat", "Uno", Vehicle.MAX_YEAR + 1, null), "year", "OUT_OF_RANGE");
        assertThat(vehicle("Fiat", "Uno", Vehicle.MIN_YEAR, null).year()).isEqualTo(Vehicle.MIN_YEAR);
        assertThat(vehicle("Fiat", "Uno", Vehicle.MAX_YEAR, null).year()).isEqualTo(Vehicle.MAX_YEAR);
    }

    @Test
    void givenAnOdometerOutsideTheRange_whenRegistering_thenItIsOutOfRange() {
        assertRejectedWith(() -> vehicle("Fiat", "Uno", 2012, -1), "odometerKm", "OUT_OF_RANGE");
        assertRejectedWith(() -> vehicle("Fiat", "Uno", 2012, Vehicle.MAX_ODOMETER_KM + 1), "odometerKm", "OUT_OF_RANGE");
        assertThat(vehicle("Fiat", "Uno", 2012, 0).odometerKm()).hasValue(0);
        assertThat(vehicle("Fiat", "Uno", 2012, Vehicle.MAX_ODOMETER_KM).odometerKm()).hasValue(Vehicle.MAX_ODOMETER_KM);
    }

    @Test
    void givenAVehicle_whenPrinting_thenThePlateNeverAppears() {
        assertThat(vehicle("Fiat", "Uno", 2012, null).toString()).doesNotContain("ABC1234");
    }
}
