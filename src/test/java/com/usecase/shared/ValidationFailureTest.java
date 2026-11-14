package com.usecase.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationFailureTest {

    @Test
    void not_active_reports_entity_and_id() {
        ValidationFailure failure = new ValidationFailure.NotActive("Business", "5511566012");
        assertEquals("Business is not active: 5511566012", failure.message());
    }

    @Test
    void conflict_reports_entity_and_id() {
        ValidationFailure failure = new ValidationFailure.Conflict("Customer", "5511566012");
        assertEquals("Customer already exists: 5511566012", failure.message());
    }

    private enum SampleReason { INSUFFICIENT_FUNDS }

    @Test
    void custom_carries_a_consumer_owned_enum_reason() {
        ValidationFailure failure = new ValidationFailure.Custom<>(
                SampleReason.INSUFFICIENT_FUNDS, "Insufficient funds");

        assertEquals("Insufficient funds", failure.message());
        assertEquals(SampleReason.INSUFFICIENT_FUNDS,
                ((ValidationFailure.Custom<?>) failure).reason());
    }

    @Test
    void rejects_null_components() {
        assertNullRejected("field", () -> new ValidationFailure.MissingField(null));
        assertNullRejected("field", () -> new ValidationFailure.InvalidValue(null, "reason"));
        assertNullRejected("reason", () -> new ValidationFailure.InvalidValue("field", null));
        assertNullRejected("entity", () -> new ValidationFailure.NotFound(null, "id"));
        assertNullRejected("id", () -> new ValidationFailure.NotFound("entity", null));
        assertNullRejected("reason", () -> new ValidationFailure.Forbidden(null));
        assertNullRejected("entity", () -> new ValidationFailure.NotActive(null, "id"));
        assertNullRejected("id", () -> new ValidationFailure.NotActive("entity", null));
        assertNullRejected("entity", () -> new ValidationFailure.Conflict(null, "id"));
        assertNullRejected("id", () -> new ValidationFailure.Conflict("entity", null));
        assertNullRejected("reason", () -> new ValidationFailure.Custom<SampleReason>(null, "message"));
        assertNullRejected("message", () -> new ValidationFailure.Custom<>(SampleReason.INSUFFICIENT_FUNDS, null));
    }

    private static void assertNullRejected(String component, Executable construction) {
        NullPointerException e = assertThrows(NullPointerException.class, construction);
        assertEquals(component + " must not be null", e.getMessage());
    }
}
