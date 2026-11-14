package com.usecase.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class UseCaseExceptionTest {

    @Test
    void carries_failure_and_its_message() {
        ValidationFailure failure = new ValidationFailure.MissingField("name");
        UseCaseException exception = new UseCaseException(failure);

        assertSame(failure, exception.failure());
        assertEquals("name is required", exception.getMessage());
    }

    @Test
    void does_not_capture_a_stack_trace_for_expected_business_failures() {
        UseCaseException exception = new UseCaseException(new ValidationFailure.Forbidden("nope"));

        assertEquals(0, exception.getStackTrace().length);
    }
}
