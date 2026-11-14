package com.usecase.model.response;

/**
 * Sealed because success genuinely has two shapes. Failures (not found,
 * forbidden, ...) never show up here — they travel through the error
 * channel as a {@code DomainFailure}.
 */
public sealed interface FeatureResponse extends Response
        permits FeatureResponse.Created, FeatureResponse.PendingApproval {

    record Created(String id, String name) implements FeatureResponse {
    }

    record PendingApproval(String id, String name) implements FeatureResponse {
    }
}
