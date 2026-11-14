package com.usecase.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainFailureTest {

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
    }

    private static void assertNullRejected(String component, Executable construction) {
        NullPointerException e = assertThrows(NullPointerException.class, construction);
        assertEquals(component + " must not be null", e.getMessage());
    }
}
