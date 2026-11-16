package br.com.erpcore.adapter.persistence.identity;

import br.com.erpcore.application.identity.port.out.MembershipRepository;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.Membership;
import br.com.erpcore.domain.identity.MembershipId;
import br.com.erpcore.domain.identity.Role;
import br.com.erpcore.domain.identity.UserId;
import java.util.List;
import java.util.Optional;

public final class JpaMembershipRepository implements MembershipRepository {

    private final MembershipJpaRepository jpa;

    public JpaMembershipRepository(MembershipJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(Membership membership) {
        jpa.save(new MembershipJpaEntity(membership.id().value(), membership.companyId().value(),
                membership.userId().value(), membership.role().name(), membership.createdAt()));
    }

    @Override
    public List<Membership> findByUserId(UserId userId) {
        return jpa.findByUserId(userId.value()).stream().map(JpaMembershipRepository::toDomain).toList();
    }

    @Override
    public Optional<Membership> find(CompanyId companyId, UserId userId) {
        return jpa.findByCompanyIdAndUserId(companyId.value(), userId.value()).map(JpaMembershipRepository::toDomain);
    }

    private static Membership toDomain(MembershipJpaEntity entity) {
        return new Membership(new MembershipId(entity.getId()), new CompanyId(entity.getCompanyId()),
                new UserId(entity.getUserId()), Role.valueOf(entity.getRole()), entity.getCreatedAt());
    }
}
