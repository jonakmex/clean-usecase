package com.usecase.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.usecase")
class DomainPurityTest {

    @ArchTest
    static final ArchRule must_not_depend_on_implementation_details = noClasses()
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.servlet..",
                    "javax.servlet..",
                    "java.sql..",
                    "javax.sql..",
                    "org.apache.http..",
                    "io.netty..",
                    "com.fasterxml.jackson.."
            );
}
