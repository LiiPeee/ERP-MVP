package br.com.erpcore.bootstrap.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.domain.shared.DomainException;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConventionRulesTest {

    private static final String ROOT_PACKAGE = "br.com.erpcore";
    private static final String DOMAIN_PACKAGE = ROOT_PACKAGE + ".domain..";
    private static final String APPLICATION_PACKAGE = ROOT_PACKAGE + ".application..";
    private static final String OUTBOUND_PORT_PACKAGE = "..port.out..";
    private static final String PERSISTENCE_PACKAGE = ROOT_PACKAGE + ".adapter.persistence";
    private static final String WEB_PACKAGE = ROOT_PACKAGE + ".adapter.web..";
    private static final String ACCESS_SCOPE = "br.com.erpcore.application.shared.port.out.AccessScope";

    private static final JavaClasses PRODUCTION_CLASSES = ProductionClasses.ALL;

    @Test
    void givenUseCases_whenCheckingTheirShape_thenEachIsAPublicFinalClassInTheApplication() {
        classes()
                .that().haveSimpleNameEndingWith("UseCase")
                .should().resideInAPackage(APPLICATION_PACKAGE)
                .andShould().bePublic()
                .andShould().haveModifier(JavaModifier.FINAL)
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void givenUseCases_whenCheckingTheirPublicMethods_thenTheOnlyOneIsExecute() {
        methods()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("UseCase")
                .and().arePublic()
                .should().haveName("execute")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void givenPublicInterfacesInTheApplication_whenCheckingWhereTheyLive_thenTheyAreOutboundPorts() {
        classes()
                .that().resideInAPackage(APPLICATION_PACKAGE)
                .and().areInterfaces()
                .and().arePublic()
                .should().resideInAPackage(OUTBOUND_PORT_PACKAGE)
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void givenDomainExceptions_whenCheckingTheirType_thenAllExtendDomainException() {
        classes()
                .that().resideInAPackage(DOMAIN_PACKAGE)
                .and().haveSimpleNameEndingWith("Exception")
                .should().beAssignableTo(DomainException.class)
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void givenDomainAndApplication_whenReadingTimeOrGeneratingIds_thenTheyGoThroughTheInjectedClockAndGenerator() {
        noClasses()
                .that().resideInAnyPackage(DOMAIN_PACKAGE, APPLICATION_PACKAGE)
                .should().callMethod(Instant.class, "now")
                .orShould().callMethod(LocalDateTime.class, "now")
                .orShould().callMethod(LocalDate.class, "now")
                .orShould().callMethod(OffsetDateTime.class, "now")
                .orShould().callMethod(ZonedDateTime.class, "now")
                .orShould().callMethod(UUID.class, "randomUUID")
                .orShould().callMethod(System.class, "currentTimeMillis")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void givenJpaEntities_whenCheckingWhereTheyLive_thenTheyAreNamedJpaEntityAndStayInThePersistenceAdapter() {
        classes()
                .that().areAnnotatedWith("jakarta.persistence.Entity")
                .should().haveSimpleNameEndingWith("JpaEntity")
                .andShould().resideInAPackage(PERSISTENCE_PACKAGE + "..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void givenThePreTenantAccessScopes_whenCheckingWhoUsesThem_thenOnlyIdentityUseCasesAndTheTransactionPointDo() {
        noClasses()
                .that().resideOutsideOfPackages(ROOT_PACKAGE + ".application.identity..",
                        ROOT_PACKAGE + ".application.shared..", PERSISTENCE_PACKAGE + ".tenancy..")
                .should().dependOnClassesThat().haveFullyQualifiedName(ACCESS_SCOPE + "$Anonymous")
                .orShould().dependOnClassesThat().haveFullyQualifiedName(ACCESS_SCOPE + "$Registration")
                .orShould().dependOnClassesThat().haveFullyQualifiedName(ACCESS_SCOPE + "$CredentialLookup")
                .orShould().dependOnClassesThat().haveFullyQualifiedName(ACCESS_SCOPE + "$RefreshTokenLookup")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void givenHandWrittenSql_whenCheckingWhoRunsIt_thenOnlyTheTenancyPackageUsesJdbcDirectly() {
        noClasses()
                .that().resideInAPackage(PERSISTENCE_PACKAGE + "..")
                .and().resideOutsideOfPackage(PERSISTENCE_PACKAGE + ".tenancy..")
                .and().doNotHaveSimpleName("PersistenceConfiguration")
                .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.jdbc.core.simple.JdbcClient")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("org.springframework.jdbc.core.JdbcTemplate")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void givenUnitsOfWork_whenCheckingTheirImplementations_thenThereIsExactlyOneProductionImplementation() {
        classes()
                .that().implement(UnitOfWork.class)
                .should().haveSimpleName("PostgresUnitOfWork")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void givenTheWebAdapter_whenCheckingItsDomainDependencies_thenItUsesOnlyDomainExceptions() {
        noClasses()
                .that().resideInAPackage(WEB_PACKAGE)
                .should().dependOnClassesThat(JavaClass.Predicates.resideInAPackage(DOMAIN_PACKAGE)
                        .and(DescribedPredicate.not(JavaClass.Predicates.assignableTo(DomainException.class))))
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void givenTheWebAdapter_whenCheckingItsDependencies_thenItNeverTouchesOutboundPortsDirectly() {
        noClasses()
                .that().resideInAPackage(WEB_PACKAGE)
                .should().dependOnClassesThat().resideInAPackage(OUTBOUND_PORT_PACKAGE)
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void givenControllers_whenCheckingWhereTheyLive_thenOnlyTheWebAdapterHasThem() {
        classes()
                .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .should().resideInAPackage(WEB_PACKAGE)
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void givenTheFeatures_whenCheckingTheirDependencies_thenThereAreNoCyclesBetweenThem() {
        slices().matching(ROOT_PACKAGE + ".domain.(*)..").should().beFreeOfCycles().check(PRODUCTION_CLASSES);
        slices().matching(ROOT_PACKAGE + ".application.(*)..").should().beFreeOfCycles().check(PRODUCTION_CLASSES);
    }
}
