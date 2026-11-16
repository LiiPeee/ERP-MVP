package br.com.erpcore.adapter.persistence.identity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipJpaRepository extends JpaRepository<MembershipJpaEntity, UUID> {

    List<MembershipJpaEntity> findByUserId(UUID userId);

    Optional<MembershipJpaEntity> findByCompanyIdAndUserId(UUID companyId, UUID userId);
}
