package com.usecase.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "com.usecase", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainPurityTest {

    // Allowlist, not denylist: anything not listed here (Spring, Jackson,
    // java.sql, java.net, Lombok, ...) fails the build, including libraries
    // nobody thought to ban yet.
    @ArchTest
    static final ArchRule must_only_depend_on_core_java_and_reactor = classes()
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                    "com.usecase..",
                    "java.lang..",
                    "java.util..",
                    "java.time..",
                    "java.math..",
                    "reactor.core..",
                    "org.reactivestreams.."
            );
}
