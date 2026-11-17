package br.com.erpcore.adapter.persistence.contact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "contact")
@DynamicUpdate
public class ContactJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "name_search", nullable = false)
    private String nameSearch;

    @Column(name = "phone_ciphertext", nullable = false)
    private byte[] phoneCiphertext;

    @Column(name = "phone_blind_index", nullable = false)
    private String phoneBlindIndex;

    @Column(name = "email_ciphertext")
    private byte[] emailCiphertext;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ContactJpaEntity() {
    }

    public ContactJpaEntity(UUID id, UUID companyId, String name, String nameSearch, byte[] phoneCiphertext,
                            String phoneBlindIndex, byte[] emailCiphertext, Instant createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.nameSearch = nameSearch;
        this.phoneCiphertext = phoneCiphertext;
        this.phoneBlindIndex = phoneBlindIndex;
        this.emailCiphertext = emailCiphertext;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public String getNameSearch() {
        return nameSearch;
    }

    public byte[] getPhoneCiphertext() {
        return phoneCiphertext;
    }

    public String getPhoneBlindIndex() {
        return phoneBlindIndex;
    }

    public byte[] getEmailCiphertext() {
        return emailCiphertext;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
