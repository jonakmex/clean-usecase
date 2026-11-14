package com.usecase;

import com.usecase.model.request.FeatureRequest;
import com.usecase.model.response.FeatureResponse;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class AbstractUseCaseTest {

    private final UseCase<FeatureRequest, FeatureResponse> useCase =
            new AbstractUseCase<>() {
                @Override
                protected Mono<FeatureResponse> process(FeatureRequest request) {
                    return Mono.just(new FeatureResponse.Success("id", request.name()));
                }
            };

    @Test
    void should_signal_error_instead_of_throwing_when_request_is_null() {
        StepVerifier.create(useCase.execute(null))
                .verifyErrorMatches(e -> e instanceof NullPointerException
                        && "request must not be null".equals(e.getMessage()));
    }
}
