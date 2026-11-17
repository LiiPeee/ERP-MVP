package br.com.erpcore.adapter.persistence.quote;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuoteJpaRepository extends JpaRepository<QuoteJpaEntity, UUID> {

    Optional<QuoteJpaEntity> findByIdAndCompanyId(UUID id, UUID companyId);

    /** Trava a linha até o fim da transação: duas decisões ao mesmo tempo se enfileiram. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from QuoteJpaEntity q where q.id = :id and q.companyId = :companyId")
    Optional<QuoteJpaEntity> lockByIdAndCompanyId(@Param("id") UUID id, @Param("companyId") UUID companyId);

    @Query("select q from QuoteJpaEntity q where q.companyId = :companyId and q.status in :statuses order by q.createdAt desc")
    List<QuoteJpaEntity> findByStatuses(@Param("companyId") UUID companyId, @Param("statuses") Collection<String> statuses,
                                        Pageable limit);

    @Query("select q from QuoteJpaEntity q where q.status = 'SENT' and q.expiresAt <= :now order by q.expiresAt asc")
    List<QuoteJpaEntity> findDueForExpiry(@Param("now") Instant now, Pageable limit);
}
