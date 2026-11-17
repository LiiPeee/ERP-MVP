package br.com.erpcore.domain.vehicle;

import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.contact.ContactId;
import br.com.erpcore.domain.shared.DomainValidationException;
import br.com.erpcore.domain.shared.Validation;
import java.time.Instant;
import java.util.Optional;

/** Veículo de um cliente da oficina. Um cliente pode ter vários. */
public record Vehicle(VehicleId id, CompanyId companyId, ContactId contactId, LicensePlate plate, String brand,
                      String model, int year, Optional<Integer> odometerKm, Instant createdAt) {

    public static final int TEXT_MAX_LENGTH = 60;
    public static final int MIN_YEAR = 1950;
    public static final int MAX_YEAR = 2100;
    public static final int MAX_ODOMETER_KM = 9_999_999;

    public static Vehicle register(VehicleId id, CompanyId companyId, ContactId contactId, LicensePlate plate,
                                   String brand, String model, int year, Integer odometerKm, Instant now) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new DomainValidationException("year", Validation.OUT_OF_RANGE);
        }
        if (odometerKm != null && (odometerKm < 0 || odometerKm > MAX_ODOMETER_KM)) {
            throw new DomainValidationException("odometerKm", Validation.OUT_OF_RANGE);
        }
        return new Vehicle(id, companyId, contactId, plate,
                Validation.requireTrimmedText("brand", brand, TEXT_MAX_LENGTH),
                Validation.requireTrimmedText("model", model, TEXT_MAX_LENGTH), year,
                Optional.ofNullable(odometerKm), now);
    }
}
