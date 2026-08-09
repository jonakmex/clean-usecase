package com.usecase.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
                SampleReason.INSUFFICIENT_FUNDS, "No cuenta con fondos suficientes");

        assertEquals("No cuenta con fondos suficientes", failure.message());
        assertEquals(SampleReason.INSUFFICIENT_FUNDS,
                ((ValidationFailure.Custom<?>) failure).reason());
    }
}
