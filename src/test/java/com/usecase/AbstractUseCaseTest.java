package com.usecase;

import com.usecase.model.request.FeatureRequest;
import com.usecase.model.response.FeatureResponse;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;

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

    @Test
    void should_signal_error_and_skip_process_when_guard_completes_empty() {
        AtomicBoolean processInvoked = new AtomicBoolean(false);
        UseCase<FeatureRequest, FeatureResponse> emptyGuard = new AbstractUseCase<>() {
            @Override
            protected Mono<FeatureRequest> guard(FeatureRequest request) {
                return Mono.empty();
            }

            @Override
            protected Mono<FeatureResponse> process(FeatureRequest request) {
                processInvoked.set(true);
                return Mono.just(new FeatureResponse.Success("id", request.name()));
            }
        };

        StepVerifier.create(emptyGuard.execute(new FeatureRequest("name")))
                .verifyErrorMatches(e -> e instanceof IllegalStateException
                        && "guard completed empty".equals(e.getMessage()));
        assertFalse(processInvoked.get());
    }
}
