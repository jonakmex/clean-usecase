package com.usecase.model.request;

import com.usecase.model.shared.Complex;
import com.usecase.model.shared.Detail;
import com.usecase.model.shared.Info;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
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
        LocalDate expectedBirthDate = LocalDate.of(1990, 6, 15);

        Complex expectedComplex = Complex.builder()
                .sample("Sample")
                .detail(Detail.builder()
                        .label("my-label")
                        .enabled(true)
                        .info(Info.builder()
                                .description("some info")
                                .priority(1)
                                .build())
                        .build())
                .build();

        Request request = requestFactory.get("FeatureRequest", Map.of(
                "name",      "Jonathan",
                "age",       30,
                "active",    true,
                "score",     9.5d,
                "code",      42,
                "verified",  Boolean.TRUE,
                "timestamp", 1_000_000L,
                "birthDate", expectedBirthDate,
                "complex",   expectedComplex
        ));

        Assertions.assertInstanceOf(FeatureRequest.class, request);
        FeatureRequest r = (FeatureRequest) request;

        // String
        Assertions.assertEquals("Jonathan", r.name);
        // primitives
        Assertions.assertEquals(30,   r.age);
        Assertions.assertTrue(r.active);
        Assertions.assertEquals(9.5d, r.score);
        // wrappers
        Assertions.assertEquals(42,           r.code);
        Assertions.assertEquals(Boolean.TRUE, r.verified);
        Assertions.assertEquals(1_000_000L,   r.timestamp);
        // date
        Assertions.assertEquals(expectedBirthDate, r.birthDate);
        // complex - nivel 1
        Assertions.assertNotNull(r.complex);
        Assertions.assertEquals("Sample", r.complex.sample);
        // complex.detail - nivel 2
        Assertions.assertNotNull(r.complex.detail);
        Assertions.assertEquals("my-label", r.complex.detail.label);
        Assertions.assertTrue(r.complex.detail.enabled);
        // complex.detail.info - nivel 3
        Assertions.assertNotNull(r.complex.detail.info);
        Assertions.assertEquals("some info", r.complex.detail.info.description);
        Assertions.assertEquals(1, r.complex.detail.info.priority);
    }
}
