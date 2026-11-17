package br.com.erpcore.application.identity.port.out;

import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.Membership;
import br.com.erpcore.domain.identity.UserId;
import java.util.List;
import java.util.Optional;

public interface MembershipRepository {

    void save(Membership membership);

    List<Membership> findByUserId(UserId userId);

    Optional<Membership> find(CompanyId companyId, UserId userId);
}
