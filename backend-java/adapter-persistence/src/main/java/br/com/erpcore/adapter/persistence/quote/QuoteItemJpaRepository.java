package br.com.erpcore.adapter.persistence.quote;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuoteItemJpaRepository extends JpaRepository<QuoteItemJpaEntity, QuoteItemJpaEntity.Key> {

    @Query("select i from QuoteItemJpaEntity i where i.key.quoteId = :quoteId and i.key.revision = :revision order by i.key.position")
    List<QuoteItemJpaEntity> findRevision(@Param("quoteId") UUID quoteId, @Param("revision") int revision);
}
