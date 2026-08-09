package com.usecase.shared;

public sealed interface ValidationFailure permits
        ValidationFailure.MissingField,
        ValidationFailure.InvalidValue,
        ValidationFailure.NotFound,
        ValidationFailure.Forbidden,
        ValidationFailure.NotActive,
        ValidationFailure.Conflict,
        ValidationFailure.Custom {

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

    /**
     * The entity exists but isn't in a usable state (e.g. an account that's
     * inactive, a business whose subscription lapsed). Distinct from
     * {@link NotFound} — the entity is real, just not currently eligible.
     */
    record NotActive(String entity, String id) implements ValidationFailure {
        @Override
        public String message() {
            return entity + " is not active: " + id;
        }
    }

    /**
     * A uniqueness rule was violated — something with this identity already
     * exists (e.g. a phone number already registered as a customer).
     */
    record Conflict(String entity, String id) implements ValidationFailure {
        @Override
        public String message() {
            return entity + " already exists: " + id;
        }
    }

    /**
     * Escape hatch for failure reasons specific to one consumer's domain
     * (e.g. insufficient funds in a ledger) that don't generalize enough to
     * justify a first-class variant here. {@code T} is an enum the
     * consumer defines and owns entirely — this library never needs to
     * change again as new domain-specific reasons come up.
     */
    record Custom<T extends Enum<T>>(T reason, String message) implements ValidationFailure {
    }
}
