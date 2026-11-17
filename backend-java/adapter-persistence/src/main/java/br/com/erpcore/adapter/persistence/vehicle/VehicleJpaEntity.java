package br.com.erpcore.adapter.persistence.vehicle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "vehicle")
@DynamicUpdate
public class VehicleJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "contact_id", nullable = false, updatable = false)
    private UUID contactId;

    @Column(name = "plate_ciphertext", nullable = false)
    private byte[] plateCiphertext;

    @Column(name = "plate_blind_index", nullable = false)
    private String plateBlindIndex;

    @Column(name = "brand", nullable = false)
    private String brand;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "odometer_km")
    private Integer odometerKm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected VehicleJpaEntity() {
    }

    public VehicleJpaEntity(UUID id, UUID companyId, UUID contactId, byte[] plateCiphertext, String plateBlindIndex,
                            String brand, String model, int year, Integer odometerKm, Instant createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.contactId = contactId;
        this.plateCiphertext = plateCiphertext;
        this.plateBlindIndex = plateBlindIndex;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.odometerKm = odometerKm;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getContactId() {
        return contactId;
    }

    public byte[] getPlateCiphertext() {
        return plateCiphertext;
    }

    public String getPlateBlindIndex() {
        return plateBlindIndex;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public int getYear() {
        return year;
    }

    public Integer getOdometerKm() {
        return odometerKm;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
