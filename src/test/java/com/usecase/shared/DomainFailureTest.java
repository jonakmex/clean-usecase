package com.usecase.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainFailureTest {

    @Test
    void missing_field_reports_field_as_required() {
        DomainFailure failure = new DomainFailure.MissingField("name");
        assertEquals("name is required", failure.message());
    }

    @Test
    void invalid_value_reports_field_and_reason() {
        DomainFailure failure = new DomainFailure.InvalidValue("phone", "must have 10 digits");
        assertEquals("phone: must have 10 digits", failure.message());
    }

    @Test
    void not_found_reports_entity_and_id() {
        DomainFailure failure = new DomainFailure.NotFound("Customer", "5511566012");
        assertEquals("Customer not found: 5511566012", failure.message());
    }

    @Test
    void forbidden_reports_reason_verbatim() {
        DomainFailure failure = new DomainFailure.Forbidden("user cannot create features");
        assertEquals("user cannot create features", failure.message());
    }

    @Test
    void not_active_reports_entity_and_id() {
        DomainFailure failure = new DomainFailure.NotActive("Business", "5511566012");
        assertEquals("Business is not active: 5511566012", failure.message());
    }

    @Test
    void conflict_reports_entity_and_id() {
        DomainFailure failure = new DomainFailure.Conflict("Customer", "5511566012");
        assertEquals("Customer already exists: 5511566012", failure.message());
    }

    private enum SampleReason { INSUFFICIENT_FUNDS }

    @Test
    void custom_carries_a_consumer_owned_enum_reason() {
        DomainFailure failure = new DomainFailure.Custom<>(
                SampleReason.INSUFFICIENT_FUNDS, "Insufficient funds");

        assertEquals("Insufficient funds", failure.message());
        assertEquals(SampleReason.INSUFFICIENT_FUNDS,
                ((DomainFailure.Custom<?>) failure).reason());
    }

    private static final DomainFailure NAME = new DomainFailure.MissingField("name");
    private static final DomainFailure EMAIL = new DomainFailure.MissingField("email");
    private static final DomainFailure PHONE = new DomainFailure.InvalidValue("phone", "must have 10 digits");

    @Test
    void multiple_reports_every_failure() {
        DomainFailure failure = new DomainFailure.Multiple(List.of(NAME, EMAIL));
        assertEquals("name is required; email is required", failure.message());
    }

    @Test
    void multiple_keeps_its_own_copy_of_the_failures() {
        List<DomainFailure> failures = new ArrayList<>(List.of(NAME, EMAIL));
        DomainFailure.Multiple multiple = new DomainFailure.Multiple(failures);
        failures.add(PHONE);

        assertEquals(List.of(NAME, EMAIL), multiple.failures());
        assertThrows(UnsupportedOperationException.class, () -> multiple.failures().add(PHONE));
    }

    @Test
    void multiple_needs_at_least_two_failures_and_no_nesting() {
        assertThrows(IllegalArgumentException.class, () -> new DomainFailure.Multiple(List.of(NAME)));
        assertThrows(IllegalArgumentException.class, () -> new DomainFailure.Multiple(
                List.of(NAME, new DomainFailure.Multiple(List.of(EMAIL, PHONE)))));
    }

    @Test
    void combine_returns_a_single_failure_as_is() {
        assertSame(NAME, DomainFailure.combine(List.of(NAME)));
    }

    @Test
    void combine_turns_several_failures_into_multiple_and_flattens() {
        DomainFailure combined = DomainFailure.combine(
                List.of(NAME, new DomainFailure.Multiple(List.of(EMAIL, PHONE))));

        assertEquals(new DomainFailure.Multiple(List.of(NAME, EMAIL, PHONE)), combined);
    }

    @Test
    void combine_rejects_an_empty_list() {
        assertThrows(IllegalArgumentException.class, () -> DomainFailure.combine(List.of()));
    }

    @Test
    void rejects_null_components() {
        assertNullRejected("field", () -> new DomainFailure.MissingField(null));
        assertNullRejected("field", () -> new DomainFailure.InvalidValue(null, "reason"));
        assertNullRejected("reason", () -> new DomainFailure.InvalidValue("field", null));
        assertNullRejected("entity", () -> new DomainFailure.NotFound(null, "id"));
        assertNullRejected("id", () -> new DomainFailure.NotFound("entity", null));
        assertNullRejected("reason", () -> new DomainFailure.Forbidden(null));
        assertNullRejected("entity", () -> new DomainFailure.NotActive(null, "id"));
        assertNullRejected("id", () -> new DomainFailure.NotActive("entity", null));
        assertNullRejected("entity", () -> new DomainFailure.Conflict(null, "id"));
        assertNullRejected("id", () -> new DomainFailure.Conflict("entity", null));
        assertNullRejected("reason", () -> new DomainFailure.Custom<SampleReason>(null, "message"));
        assertNullRejected("message", () -> new DomainFailure.Custom<>(SampleReason.INSUFFICIENT_FUNDS, null));
        assertNullRejected("failures", () -> new DomainFailure.Multiple(null));
        assertNullRejected("failures", () -> DomainFailure.combine(null));
        NullPointerException e = assertThrows(NullPointerException.class,
                () -> new DomainFailure.Multiple(Arrays.asList(NAME, null)));
        assertEquals("failures must not contain null", e.getMessage());
    }

    private static void assertNullRejected(String component, Executable construction) {
        NullPointerException e = assertThrows(NullPointerException.class, construction);
        assertEquals(component + " must not be null", e.getMessage());
    }
}
