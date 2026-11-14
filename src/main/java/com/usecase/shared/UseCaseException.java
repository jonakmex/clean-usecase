package com.usecase.shared;

public final class UseCaseException extends RuntimeException {

    private final DomainFailure failure;

    /**
     * Business failures are expected outcomes carried as data, not bugs, so
     * no stack trace is captured (it's costly and says nothing useful) and
     * suppression is disabled.
     */
    public UseCaseException(DomainFailure failure) {
        super(failure.message(), null, false, false);
        this.failure = failure;
    }

    public DomainFailure failure() {
        return failure;
    }
}
