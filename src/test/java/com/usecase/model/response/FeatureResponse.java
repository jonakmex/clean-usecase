package com.usecase.model.response;

public sealed interface FeatureResponse extends Response
        permits FeatureResponse.Success, FeatureResponse.Error {

    record Success(String id, String name) implements FeatureResponse {
    }

    record Error(String reason) implements FeatureResponse {
    }
}
