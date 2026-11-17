package br.com.erpcore.adapter.persistence.profile;

import br.com.erpcore.adapter.persistence.support.FieldColumns;
import br.com.erpcore.application.profile.port.out.CompanyProfileRepository;
import br.com.erpcore.application.shared.port.out.FieldContext;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.company.CompanyProfile;
import br.com.erpcore.domain.shared.PhoneNumber;
import java.util.Optional;

public final class JpaCompanyProfileRepository implements CompanyProfileRepository {

    private static final String PHONE_FIELD = "company_profile.business_phone";

    private final CompanyProfileJpaRepository jpa;
    private final FieldColumns columns;

    public JpaCompanyProfileRepository(CompanyProfileJpaRepository jpa, FieldColumns columns) {
        this.jpa = jpa;
        this.columns = columns;
    }

    @Override
    public void save(CompanyProfile profile) {
        FieldContext phoneContext = new FieldContext(profile.companyId(), PHONE_FIELD);
        byte[] current = jpa.findById(profile.companyId().value()).map(CompanyProfileJpaEntity::getBusinessPhoneCiphertext).orElse(null);
        byte[] phone = profile.businessPhone().map(value -> columns.reuseOrEncrypt(phoneContext, current, value.value())).orElse(null);
        jpa.save(new CompanyProfileJpaEntity(profile.companyId().value(), profile.displayName(), phone, profile.address(),
                profile.openingHours(), profile.defaultValidityDays(), profile.signatureMessage()));
    }

    @Override
    public Optional<CompanyProfile> find(CompanyId companyId) {
        FieldContext phoneContext = new FieldContext(companyId, PHONE_FIELD);
        return jpa.findById(companyId.value()).map(entity -> new CompanyProfile(companyId, entity.getDisplayName(),
                Optional.ofNullable(entity.getBusinessPhoneCiphertext())
                        .map(cipher -> new PhoneNumber(columns.decrypt(phoneContext, cipher))),
                entity.getAddress(), entity.getOpeningHours(), entity.getDefaultValidityDays(), entity.getSignatureMessage()));
    }
}
