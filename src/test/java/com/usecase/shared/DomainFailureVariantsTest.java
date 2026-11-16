package com.usecase.shared;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the variants of {@link DomainFailure}. A new variant breaks every
 * consumer's exhaustive {@code switch}, and apiCheck (japicmp) can't see it:
 * to japicmp it's just an added class. This test is that missing check.
 */
class DomainFailureVariantsTest {

    // When this list changes, set VARIANTS_SINCE to the next minor and bump
    // the version in build.gradle to it (pre-1.0 SemVer: breaking = minor).
    private static final Set<String> VARIANTS = Set.of(
            "MissingField", "InvalidValue", "NotFound", "Forbidden",
            "NotActive", "Conflict", "Custom", "Multiple");
    private static final String VARIANTS_SINCE = "0.5";

    @Test
    void variants_are_the_pinned_ones() {
        Set<String> actual = Arrays.stream(DomainFailure.class.getPermittedSubclasses())
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());

        assertEquals(VARIANTS, actual, """
                DomainFailure's variants changed. That breaks consumers' exhaustive switches \
                even though apiCheck passes: update VARIANTS, set VARIANTS_SINCE to the next \
                minor and bump the version in build.gradle to it.""");
    }

    @Test
    void version_is_at_least_the_minor_that_introduced_the_variants() {
        String version = System.getProperty("project.version");
        assertNotNull(version, "project.version system property is not set by the test task");

        assertTrue(compareMajorMinor(version, VARIANTS_SINCE) >= 0,
                "DomainFailure's variants changed in " + VARIANTS_SINCE + " but the version is "
                        + version + ": bump it to " + VARIANTS_SINCE + ".0-SNAPSHOT");
    }

    private static int compareMajorMinor(String version, String majorMinor) {
        int[] a = majorMinor(version);
        int[] b = majorMinor(majorMinor);
        return a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]);
    }

    private static int[] majorMinor(String version) {
        String[] parts = version.split("[.-]");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }
}
