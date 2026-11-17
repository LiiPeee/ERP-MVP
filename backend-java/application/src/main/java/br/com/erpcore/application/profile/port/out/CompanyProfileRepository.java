package br.com.erpcore.application.profile.port.out;

import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.company.CompanyProfile;
import java.util.Optional;

public interface CompanyProfileRepository {

    /** Insere o perfil ou substitui o que a oficina já tinha. */
    void save(CompanyProfile profile);

    Optional<CompanyProfile> find(CompanyId companyId);
}
