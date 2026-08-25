package com.example.inventory.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Executable hexagonal-layering rules from AGENT.md section 4. If this test fails, the layering
 * was broken - do not weaken the rule without updating AGENT.md and adding an ADR.
 */
@AnalyzeClasses(packages = "com.example.inventory", importOptions = com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainMustNotDependOnOuterLayers = noClasses()
            .that().resideInAPackage("..inventory.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..inventory.application..",
                    "..inventory.infrastructure..",
                    "..inventory.web..");

    @ArchTest
    static final ArchRule applicationMustNotDependOnInfrastructureOrWeb = noClasses()
            .that().resideInAPackage("..inventory.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..inventory.infrastructure..",
                    "..inventory.web..");

    @ArchTest
    static final ArchRule jpaEntitiesLiveOnlyInPersistencePackage = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..infrastructure.persistence..");

    @ArchTest
    static final ArchRule restControllersLiveOnlyInWebPackage = classes()
            .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .should().resideInAPackage("..inventory.web..");
}
