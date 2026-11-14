package com.usecase.feature;

import com.usecase.model.request.FeatureRequest;
import com.usecase.model.response.FeatureResponse;
import com.usecase.shared.DomainFailure;
import com.usecase.shared.UseCaseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicInteger;

class FeatureUseCaseTest {

    private static final User ALLOWED = new User("u-1", true);
    private static final User NOT_ALLOWED = new User("u-2", false);

    private final AtomicInteger gatewayCalls = new AtomicInteger();

    private FeatureUseCase useCaseWith(UserGateway gateway) {
        return new FeatureUseCase(id -> {
            gatewayCalls.incrementAndGet();
            return gateway.findById(id);
        });
    }

    @Test
    void should_return_success_when_user_exists_and_is_allowed() {
        StepVerifier.create(useCaseWith(id -> Mono.just(ALLOWED))
                        .execute(new FeatureRequest(ALLOWED.id(), "Jonathan")))
                .assertNext(response -> {
                    Assertions.assertInstanceOf(FeatureResponse.Success.class, response);
                    FeatureResponse.Success success = (FeatureResponse.Success) response;
                    Assertions.assertNotNull(success.id());
                    Assertions.assertEquals("Jonathan", success.name());
                })
                .verifyComplete();
        Assertions.assertEquals(1, gatewayCalls.get());
    }

    @Test
    void should_signal_missing_field_without_calling_gateway_when_name_is_null() {
        StepVerifier.create(useCaseWith(id -> Mono.just(ALLOWED))
                        .execute(new FeatureRequest(ALLOWED.id(), null)))
                .verifyErrorMatches(e -> e instanceof UseCaseException uce
                        && uce.failure() instanceof DomainFailure.MissingField missingField
                        && "name".equals(missingField.field()));
        Assertions.assertEquals(0, gatewayCalls.get());
    }

    @Test
    void should_signal_missing_field_without_calling_gateway_when_user_id_is_null() {
        StepVerifier.create(useCaseWith(id -> Mono.just(ALLOWED))
                        .execute(new FeatureRequest(null, "Jonathan")))
                .verifyErrorMatches(e -> e instanceof UseCaseException uce
                        && uce.failure() instanceof DomainFailure.MissingField missingField
                        && "userId".equals(missingField.field()));
        Assertions.assertEquals(0, gatewayCalls.get());
    }

    @Test
    void should_signal_not_found_when_user_does_not_exist() {
        StepVerifier.create(useCaseWith(id -> Mono.empty())
                        .execute(new FeatureRequest("missing", "Jonathan")))
                .verifyErrorMatches(e -> e instanceof UseCaseException uce
                        && uce.failure() instanceof DomainFailure.NotFound notFound
                        && "user".equals(notFound.entity())
                        && "missing".equals(notFound.id()));
        Assertions.assertEquals(1, gatewayCalls.get());
    }

    @Test
    void should_signal_forbidden_when_user_is_not_allowed() {
        StepVerifier.create(useCaseWith(id -> Mono.just(NOT_ALLOWED))
                        .execute(new FeatureRequest(NOT_ALLOWED.id(), "Jonathan")))
                .verifyErrorMatches(e -> e instanceof UseCaseException uce
                        && uce.failure() instanceof DomainFailure.Forbidden);
        Assertions.assertEquals(1, gatewayCalls.get());
    }
}
