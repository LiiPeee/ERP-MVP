package br.com.erpcore.adapter.persistence;

import br.com.erpcore.adapter.persistence.audit.JpaAuditEventRepository;
import br.com.erpcore.adapter.persistence.company.CompanyJpaRepository;
import br.com.erpcore.adapter.persistence.company.JpaCompanyRepository;
import br.com.erpcore.adapter.persistence.identity.JpaMembershipRepository;
import br.com.erpcore.adapter.persistence.identity.JpaRefreshTokenRepository;
import br.com.erpcore.adapter.persistence.identity.JpaUserRepository;
import br.com.erpcore.adapter.persistence.identity.MembershipJpaRepository;
import br.com.erpcore.adapter.persistence.identity.RefreshTokenJpaRepository;
import br.com.erpcore.adapter.persistence.identity.UserJpaRepository;
import br.com.erpcore.adapter.persistence.tenancy.PostgresUnitOfWork;
import br.com.erpcore.adapter.persistence.tenancy.ScopeVariables;
import br.com.erpcore.application.audit.port.out.AuditEventRepository;
import br.com.erpcore.application.company.port.out.CompanyRepository;
import br.com.erpcore.application.identity.port.out.MembershipRepository;
import br.com.erpcore.application.identity.port.out.RefreshTokenRepository;
import br.com.erpcore.application.identity.port.out.UserRepository;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Liga os adaptadores de PostgreSQL às portas da aplicação. O módulo expõe esta configuração e o bootstrap a
 * importa. {@code @AutoConfigurationPackage} faz o Spring Data e o JPA enxergarem as entidades e os
 * repositórios deste pacote, mesmo com a aplicação principal em outro.
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigurationPackage(basePackageClasses = PersistenceConfiguration.class)
public class PersistenceConfiguration {

    @Bean
    ScopeVariables scopeVariables(JdbcClient jdbcClient) {
        return new ScopeVariables(jdbcClient);
    }

    @Bean
    UnitOfWork unitOfWork(PlatformTransactionManager transactionManager, ScopeVariables scopeVariables) {
        return new PostgresUnitOfWork(transactionManager, scopeVariables);
    }

    @Bean
    CompanyRepository companyRepository(CompanyJpaRepository jpa) {
        return new JpaCompanyRepository(jpa);
    }

    @Bean
    UserRepository userRepository(UserJpaRepository jpa) {
        return new JpaUserRepository(jpa);
    }

    @Bean
    MembershipRepository membershipRepository(MembershipJpaRepository jpa) {
        return new JpaMembershipRepository(jpa);
    }

    @Bean
    RefreshTokenRepository refreshTokenRepository(RefreshTokenJpaRepository jpa, ScopeVariables scopeVariables) {
        return new JpaRefreshTokenRepository(jpa, scopeVariables);
    }

    @Bean
    AuditEventRepository auditEventRepository(EntityManager entityManager) {
        return new JpaAuditEventRepository(entityManager);
    }
}
