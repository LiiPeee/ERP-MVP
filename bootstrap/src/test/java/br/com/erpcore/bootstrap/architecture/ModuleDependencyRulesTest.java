package br.com.erpcore.bootstrap.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ModuleDependencyRulesTest {

    private static final String ROOT_PACKAGE = "br.com.erpcore";

    private static final String DOMAIN = "domain";
    private static final String APPLICATION = "application";
    private static final String ADAPTER_WEB = "adapter-web";
    private static final String ADAPTER_PERSISTENCE = "adapter-persistence";
    private static final String ADAPTER_INFRA = "adapter-infra";
    private static final String BOOTSTRAP = "bootstrap";

    private static final String DOMAIN_PACKAGE = ROOT_PACKAGE + ".domain..";
    private static final String APPLICATION_PACKAGE = ROOT_PACKAGE + ".application..";
    private static final String ADAPTER_WEB_PACKAGE = ROOT_PACKAGE + ".adapter.web..";
    private static final String ADAPTER_PERSISTENCE_PACKAGE = ROOT_PACKAGE + ".adapter.persistence..";
    private static final String ADAPTER_INFRA_PACKAGE = ROOT_PACKAGE + ".adapter.infra..";
    private static final String BOOTSTRAP_PACKAGE = ROOT_PACKAGE + ".bootstrap..";

    private static final String JDK_PACKAGE = "java..";

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT_PACKAGE);

    @Test
    void givenAllModules_whenCheckingDependencies_thenEachModuleOnlyAccessesAllowedModules() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .withOptionalLayers(true)
                .layer(DOMAIN).definedBy(DOMAIN_PACKAGE)
                .layer(APPLICATION).definedBy(APPLICATION_PACKAGE)
                .layer(ADAPTER_WEB).definedBy(ADAPTER_WEB_PACKAGE)
                .layer(ADAPTER_PERSISTENCE).definedBy(ADAPTER_PERSISTENCE_PACKAGE)
                .layer(ADAPTER_INFRA).definedBy(ADAPTER_INFRA_PACKAGE)
                .layer(BOOTSTRAP).definedBy(BOOTSTRAP_PACKAGE)
                .whereLayer(DOMAIN).mayNotAccessAnyLayer()
                .whereLayer(APPLICATION).mayOnlyAccessLayers(DOMAIN)
                .whereLayer(ADAPTER_WEB).mayOnlyAccessLayers(APPLICATION)
                .whereLayer(ADAPTER_PERSISTENCE).mayOnlyAccessLayers(APPLICATION, DOMAIN)
                .whereLayer(ADAPTER_INFRA).mayOnlyAccessLayers(APPLICATION)
                .whereLayer(BOOTSTRAP).mayNotBeAccessedByAnyLayer()
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void givenDomainAndApplication_whenCheckingDependencies_thenTheyUseOnlyPlainJava() {
        noClasses()
                .that().resideInAnyPackage(DOMAIN_PACKAGE, APPLICATION_PACKAGE)
                .should().dependOnClassesThat()
                .resideOutsideOfPackages(JDK_PACKAGE, DOMAIN_PACKAGE, APPLICATION_PACKAGE)
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void givenProductionClasses_whenCheckingPackages_thenEachBelongsToAKnownModule() {
        classes()
                .should().resideInAnyPackage(
                        DOMAIN_PACKAGE,
                        APPLICATION_PACKAGE,
                        ADAPTER_WEB_PACKAGE,
                        ADAPTER_PERSISTENCE_PACKAGE,
                        ADAPTER_INFRA_PACKAGE,
                        BOOTSTRAP_PACKAGE)
                .check(PRODUCTION_CLASSES);
    }
}
