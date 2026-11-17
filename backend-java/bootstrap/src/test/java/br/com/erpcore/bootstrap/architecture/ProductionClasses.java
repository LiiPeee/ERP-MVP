package br.com.erpcore.bootstrap.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * As classes de produção de todos os módulos, para as regras de arquitetura. Fora dos testes do próprio módulo,
 * o {@code bootstrap} usa o jar de testes do {@code adapter-persistence}: ele também fica de fora.
 */
final class ProductionClasses {

    static final String ROOT_PACKAGE = "br.com.erpcore";

    private static final ImportOption NO_TEST_JARS = location -> !location.contains("-tests.jar");

    static final JavaClasses ALL = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(NO_TEST_JARS)
            .importPackages(ROOT_PACKAGE);

    private ProductionClasses() {
    }
}
