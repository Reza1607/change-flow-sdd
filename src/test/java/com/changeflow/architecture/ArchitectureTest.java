package com.changeflow.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

class ArchitectureTest {
    @Test
    void dependenciesPointInwards() {
        var classes = new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.changeflow");
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..",
                        "..api..", "..application..", "..infrastructure..", "com.changeflow.web..")
                .allowEmptyShould(true).check(classes);
        noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage("..api..", "..infrastructure..", "com.changeflow.web..")
                .allowEmptyShould(true).check(classes);
        noClasses().that().resideInAnyPackage("com.changeflow.identity..", "com.changeflow.changerequest..")
                .should().dependOnClassesThat().resideInAPackage("com.changeflow.web..")
                .allowEmptyShould(true).check(classes);
        noClasses().that().resideInAPackage("com.changeflow.identity..")
                .should().dependOnClassesThat().resideInAPackage("com.changeflow.changerequest..")
                .allowEmptyShould(true).check(classes);
        noClasses().that().resideInAPackage("..api..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .allowEmptyShould(true).check(classes);
        noClasses().that().resideInAPackage("com.changeflow.changerequest..")
                .should().dependOnClassesThat().resideInAPackage("com.changeflow.identity.infrastructure..")
                .allowEmptyShould(true).check(classes);
        fields().that().areDeclaredInClassesThat().resideInAPackage("com.changeflow..")
                .should().notBeAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                .allowEmptyShould(true).check(classes);
    }

    @Test
    void persistenceAndControllerBoundariesAreExplicit() {
        var classes=new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests()).importPackages("com.changeflow");
        classes().that().areAnnotatedWith("jakarta.persistence.Entity")
            .should().resideInAPackage("..infrastructure..")
            .andShould().notBePublic().check(classes);
        noClasses().that().resideInAPackage("..api..")
            .should().dependOnClassesThat().areAnnotatedWith("jakarta.persistence.Entity")
            .allowEmptyShould(true).check(classes);
        classes().that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .should(new com.tngtech.archunit.lang.ArchCondition<com.tngtech.archunit.core.domain.JavaClass>("delegate without calling domain rules or persistence") {
                @Override public void check(com.tngtech.archunit.core.domain.JavaClass controller,com.tngtech.archunit.lang.ConditionEvents events) {
                    for(var call:controller.getMethodCallsFromSelf()) {
                        var owner=call.getTargetOwner().getPackageName();
                        if(owner.contains(".domain") || owner.contains(".infrastructure") || owner.startsWith("org.springframework.data") || owner.startsWith("jakarta.persistence"))
                            events.add(com.tngtech.archunit.lang.SimpleConditionEvent.violated(call,call.getDescription()));
                    }
                }
            }).check(classes);
        for(var c:classes) for(var dependency:c.getDirectDependenciesFromSelf()) {
            var source=c.getPackageName();var target=dependency.getTargetClass();
            if(source.startsWith("com.changeflow.changerequest") && target.getPackageName().startsWith("com.changeflow.identity")) {
                org.junit.jupiter.api.Assertions.assertTrue(
                    (target.getPackageName().equals("com.changeflow.identity.application") && target.isInterface())
                    || (target.getPackageName().equals("com.changeflow.identity.domain") && (target.isRecord() || target.isEnum())),
                    dependency.getDescription());
            }
        }
    }
}
