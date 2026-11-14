package com.usecase.shared;

import java.util.Objects;

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
        public MissingField {
            Objects.requireNonNull(field, "field must not be null");
        }

        @Override
        public String message() {
            return field + " is required";
        }
    }

    record InvalidValue(String field, String reason) implements ValidationFailure {
        public InvalidValue {
            Objects.requireNonNull(field, "field must not be null");
            Objects.requireNonNull(reason, "reason must not be null");
        }

        @Override
        public String message() {
            return field + ": " + reason;
        }
    }

    record NotFound(String entity, String id) implements ValidationFailure {
        public NotFound {
            Objects.requireNonNull(entity, "entity must not be null");
            Objects.requireNonNull(id, "id must not be null");
        }

        @Override
        public String message() {
            return entity + " not found: " + id;
        }
    }

    record Forbidden(String reason) implements ValidationFailure {
        public Forbidden {
            Objects.requireNonNull(reason, "reason must not be null");
        }

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
        public NotActive {
            Objects.requireNonNull(entity, "entity must not be null");
            Objects.requireNonNull(id, "id must not be null");
        }

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
        public Conflict {
            Objects.requireNonNull(entity, "entity must not be null");
            Objects.requireNonNull(id, "id must not be null");
        }

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
        public Custom {
            Objects.requireNonNull(reason, "reason must not be null");
            Objects.requireNonNull(message, "message must not be null");
        }
    }
}
