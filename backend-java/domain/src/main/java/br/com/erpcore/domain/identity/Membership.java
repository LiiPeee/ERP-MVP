package br.com.erpcore.domain.identity;

import br.com.erpcore.domain.company.CompanyId;
import java.time.Instant;

public record Membership(MembershipId id, CompanyId companyId, UserId userId, Role role, Instant createdAt) {
}
