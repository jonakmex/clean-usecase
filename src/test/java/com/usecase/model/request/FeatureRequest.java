package com.usecase.model.request;

public record FeatureRequest(String userId, String name) implements Request {
}
