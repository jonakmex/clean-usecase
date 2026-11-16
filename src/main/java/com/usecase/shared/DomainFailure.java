package com.usecase.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public sealed interface DomainFailure permits
        DomainFailure.MissingField,
        DomainFailure.InvalidValue,
        DomainFailure.NotFound,
        DomainFailure.Forbidden,
        DomainFailure.NotActive,
        DomainFailure.Conflict,
        DomainFailure.Custom,
        DomainFailure.Multiple {

    String message();

    /**
     * Combines the failures found while checking one action: a single
     * failure is returned as is, several become a {@link Multiple} (nested
     * {@code Multiple}s are flattened).
     *
     * @throws IllegalArgumentException if {@code failures} is empty
     */
    static DomainFailure combine(List<? extends DomainFailure> failures) {
        Objects.requireNonNull(failures, "failures must not be null");
        List<DomainFailure> flat = new ArrayList<>();
        for (DomainFailure failure : failures) {
            Objects.requireNonNull(failure, "failures must not contain null");
            if (failure instanceof Multiple multiple) {
                flat.addAll(multiple.failures());
            } else {
                flat.add(failure);
            }
        }
        if (flat.isEmpty()) {
            throw new IllegalArgumentException("failures must not be empty");
        }
        return flat.size() == 1 ? flat.getFirst() : new Multiple(flat);
    }

    record MissingField(String field) implements DomainFailure {
        public MissingField {
            Objects.requireNonNull(field, "field must not be null");
        }

        @Override
        public String message() {
            return field + " is required";
        }
    }

    record InvalidValue(String field, String reason) implements DomainFailure {
        public InvalidValue {
            Objects.requireNonNull(field, "field must not be null");
            Objects.requireNonNull(reason, "reason must not be null");
        }

        @Override
        public String message() {
            return field + ": " + reason;
        }
    }

    record NotFound(String entity, String id) implements DomainFailure {
        public NotFound {
            Objects.requireNonNull(entity, "entity must not be null");
            Objects.requireNonNull(id, "id must not be null");
        }

        @Override
        public String message() {
            return entity + " not found: " + id;
        }
    }

    record Forbidden(String reason) implements DomainFailure {
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
    record NotActive(String entity, String id) implements DomainFailure {
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
    record Conflict(String entity, String id) implements DomainFailure {
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
    record Custom<T extends Enum<T>>(T reason, String message) implements DomainFailure {
        public Custom {
            Objects.requireNonNull(reason, "reason must not be null");
            Objects.requireNonNull(message, "message must not be null");
        }
    }

    /**
     * Several failures found at once (e.g. every missing detail of a
     * registration), so the caller can fix them all in one go instead of one
     * per attempt. Holds at least two failures, none of them a
     * {@code Multiple}; build it with {@link #combine(List)} to get those
     * guarantees from any list.
     */
    record Multiple(List<DomainFailure> failures) implements DomainFailure {
        public Multiple {
            Objects.requireNonNull(failures, "failures must not be null");
            failures.forEach(f -> Objects.requireNonNull(f, "failures must not contain null"));
            failures = List.copyOf(failures);
            if (failures.size() < 2) {
                throw new IllegalArgumentException("Multiple needs at least two failures; use combine()");
            }
            if (failures.stream().anyMatch(Multiple.class::isInstance)) {
                throw new IllegalArgumentException("Multiple must not nest; use combine()");
            }
        }

        @Override
        public String message() {
            return failures.stream().map(DomainFailure::message).collect(Collectors.joining("; "));
        }
    }
}
