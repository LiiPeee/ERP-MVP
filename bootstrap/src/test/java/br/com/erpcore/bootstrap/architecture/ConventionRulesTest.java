package br.com.erpcore.bootstrap.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import br.com.erpcore.domain.shared.DomainException;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
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

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT_PACKAGE);

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
    void givenTheFeatures_whenCheckingTheirDependencies_thenThereAreNoCyclesBetweenThem() {
        slices().matching(ROOT_PACKAGE + ".domain.(*)..").should().beFreeOfCycles().check(PRODUCTION_CLASSES);
        slices().matching(ROOT_PACKAGE + ".application.(*)..").should().beFreeOfCycles().check(PRODUCTION_CLASSES);
    }
}
