package br.com.erpcore.adapter.persistence.contact;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactJpaRepository extends JpaRepository<ContactJpaEntity, UUID> {

    Optional<ContactJpaEntity> findByIdAndCompanyId(UUID id, UUID companyId);

    Optional<ContactJpaEntity> findByCompanyIdAndPhoneBlindIndex(UUID companyId, String phoneBlindIndex);

    /** {@code pattern} já vem com os curingas do LIKE escapados com a barra invertida. */
    @Query("""
            select c from ContactJpaEntity c
             where c.companyId = :companyId and c.nameSearch like :pattern escape '\\'
             order by c.createdAt desc
            """)
    List<ContactJpaEntity> searchByName(@Param("companyId") UUID companyId, @Param("pattern") String pattern, Pageable limit);
}
