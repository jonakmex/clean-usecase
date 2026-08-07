package com.usecase.shared;

public final class UseCaseException extends RuntimeException {

    private final ValidationFailure failure;

    public UseCaseException(ValidationFailure failure) {
        super(failure.message());
        this.failure = failure;
    }

    public ValidationFailure failure() {
        return failure;
    }
}
