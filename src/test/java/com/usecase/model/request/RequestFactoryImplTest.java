package com.usecase.model.request;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.Map;

class RequestFactoryImplTest {

    private static RequestFactory requestFactory;

    @BeforeAll
    static void setup() {
        requestFactory = new RequestFactoryImpl("com.usecase.model.request");
    }

    @Test
    void should_get_request_by_name() {
        Request request = requestFactory.get("FeatureRequest", null);
        Assertions.assertInstanceOf(FeatureRequest.class, request);
    }

    @Test
    void should_get_request_by_name_with_params() {
        Request request = requestFactory.get("FeatureRequest", Map.of("name", "Jonathan"));
        Assertions.assertInstanceOf(FeatureRequest.class, request);
        Assertions.assertEquals("Jonathan", ((FeatureRequest) request).name);
    }
}

