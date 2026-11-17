package br.com.erpcore.domain.company;

import java.time.Instant;

public record Company(CompanyId id, CompanyName name, Instant createdAt) {
}
