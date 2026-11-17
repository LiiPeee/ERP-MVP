package br.com.erpcore.adapter.persistence.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "company_profile")
@DynamicUpdate
public class CompanyProfileJpaEntity {

    @Id
    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "business_phone_ciphertext")
    private byte[] businessPhoneCiphertext;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "opening_hours", nullable = false)
    private String openingHours;

    @Column(name = "default_validity_days", nullable = false)
    private int defaultValidityDays;

    @Column(name = "signature_message", nullable = false)
    private String signatureMessage;

    protected CompanyProfileJpaEntity() {
    }

    public CompanyProfileJpaEntity(UUID companyId, String displayName, byte[] businessPhoneCiphertext, String address,
                                   String openingHours, int defaultValidityDays, String signatureMessage) {
        this.companyId = companyId;
        this.displayName = displayName;
        this.businessPhoneCiphertext = businessPhoneCiphertext;
        this.address = address;
        this.openingHours = openingHours;
        this.defaultValidityDays = defaultValidityDays;
        this.signatureMessage = signatureMessage;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public byte[] getBusinessPhoneCiphertext() {
        return businessPhoneCiphertext;
    }

    public String getAddress() {
        return address;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public int getDefaultValidityDays() {
        return defaultValidityDays;
    }

    public String getSignatureMessage() {
        return signatureMessage;
    }
}
