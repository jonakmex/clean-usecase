package com.usecase.shared;

public sealed interface ValidationFailure permits
        ValidationFailure.MissingField,
        ValidationFailure.InvalidValue,
        ValidationFailure.NotFound,
        ValidationFailure.Forbidden {

    String message();

    record MissingField(String field) implements ValidationFailure {
        @Override
        public String message() {
            return field + " is required";
        }
    }

    record InvalidValue(String field, String reason) implements ValidationFailure {
        @Override
        public String message() {
            return field + ": " + reason;
        }
    }

    record NotFound(String entity, String id) implements ValidationFailure {
        @Override
        public String message() {
            return entity + " not found: " + id;
        }
    }

    record Forbidden(String reason) implements ValidationFailure {
        @Override
        public String message() {
            return reason;
        }
    }
}
