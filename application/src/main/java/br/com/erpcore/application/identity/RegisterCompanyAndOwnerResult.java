package br.com.erpcore.application.identity;

import java.util.UUID;

public record RegisterCompanyAndOwnerResult(UUID companyId, UUID userId) {
}
