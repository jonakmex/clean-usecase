package com.usecase.shared;

public final class UseCaseException extends RuntimeException {

    private final ValidationFailure failure;

    /**
     * Business failures are expected outcomes carried as data, not bugs, so
     * no stack trace is captured (it's costly and says nothing useful) and
     * suppression is disabled.
     */
    public UseCaseException(ValidationFailure failure) {
        super(failure.message(), null, false, false);
        this.failure = failure;
    }

    public ValidationFailure failure() {
        return failure;
    }
}
