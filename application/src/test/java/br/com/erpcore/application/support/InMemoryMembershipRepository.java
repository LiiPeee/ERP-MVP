package br.com.erpcore.application.support;

import br.com.erpcore.application.identity.port.out.MembershipRepository;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.Membership;
import br.com.erpcore.domain.identity.UserId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class InMemoryMembershipRepository implements MembershipRepository {

    private final InMemoryUnitOfWork unitOfWork;
    private final List<Membership> storage = new ArrayList<>();

    public InMemoryMembershipRepository(InMemoryUnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    @Override
    public void save(Membership membership) {
        unitOfWork.requireActive();
        storage.removeIf(existing -> existing.id().equals(membership.id()));
        storage.add(membership);
    }

    @Override
    public List<Membership> findByUserId(UserId userId) {
        unitOfWork.requireActive();
        return storage.stream().filter(membership -> membership.userId().equals(userId)).toList();
    }

    @Override
    public Optional<Membership> find(CompanyId companyId, UserId userId) {
        unitOfWork.requireActive();
        return storage.stream()
                .filter(membership -> membership.companyId().equals(companyId) && membership.userId().equals(userId))
                .findFirst();
    }

    public List<Membership> all() {
        return List.copyOf(storage);
    }
}
