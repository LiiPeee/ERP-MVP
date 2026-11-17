package br.com.erpcore.adapter.persistence.vehicle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleJpaRepository extends JpaRepository<VehicleJpaEntity, UUID> {

    Optional<VehicleJpaEntity> findByIdAndCompanyId(UUID id, UUID companyId);

    Optional<VehicleJpaEntity> findByCompanyIdAndPlateBlindIndex(UUID companyId, String plateBlindIndex);

    List<VehicleJpaEntity> findByCompanyIdAndContactIdOrderByCreatedAtAsc(UUID companyId, UUID contactId);
}
