package br.com.erpcore.adapter.persistence.profile;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyProfileJpaRepository extends JpaRepository<CompanyProfileJpaEntity, UUID> {
}
