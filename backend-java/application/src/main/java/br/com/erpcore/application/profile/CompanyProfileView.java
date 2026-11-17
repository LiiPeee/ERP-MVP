package br.com.erpcore.application.profile;

import br.com.erpcore.domain.company.CompanyProfile;
import br.com.erpcore.domain.shared.PhoneNumber;

/** O perfil como a API o devolve: o telefone comercial vai mascarado. */
public record CompanyProfileView(String displayName, String businessPhoneMasked, String address, String openingHours,
                                 int defaultValidityDays, String signatureMessage) {

    static CompanyProfileView from(CompanyProfile profile) {
        return new CompanyProfileView(profile.displayName(), profile.businessPhone().map(PhoneNumber::masked).orElse(null),
                profile.address(), profile.openingHours(), profile.defaultValidityDays(), profile.signatureMessage());
    }
}
