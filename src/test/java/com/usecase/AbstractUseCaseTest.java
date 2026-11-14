package com.usecase;

import com.usecase.model.request.FeatureRequest;
import com.usecase.model.response.FeatureResponse;
import com.usecase.shared.DomainFailure;
import com.usecase.shared.UseCaseException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class AbstractUseCaseTest {

    private final UseCase<FeatureRequest, FeatureResponse> useCase =
            new AbstractUseCase<>() {
                @Override
                protected Mono<FeatureResponse> process(FeatureRequest request) {
                    return Mono.just(new FeatureResponse.Created("id", request.name()));
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
                return Mono.just(new FeatureResponse.Created("id", request.name()));
            }
        };

        StepVerifier.create(emptyGuard.execute(new FeatureRequest("u-1", "name")))
                .verifyErrorMatches(e -> e instanceof IllegalStateException
                        && "guard completed empty".equals(e.getMessage()));
        assertFalse(processInvoked.get());
    }

    @Test
    void should_pass_request_unchanged_to_process_when_guard_is_not_overridden() {
        AtomicReference<FeatureRequest> received = new AtomicReference<>();
        UseCase<FeatureRequest, FeatureResponse> defaultGuard = new AbstractUseCase<>() {
            @Override
            protected Mono<FeatureResponse> process(FeatureRequest request) {
                received.set(request);
                return Mono.just(new FeatureResponse.Created("id", request.name()));
            }
        };
        FeatureRequest request = new FeatureRequest("u-1", "name");

        StepVerifier.create(defaultGuard.execute(request))
                .expectNextCount(1)
                .verifyComplete();
        assertSame(request, received.get());
    }

    @Test
    void should_propagate_guard_error_and_skip_process() {
        UseCaseException failure = new UseCaseException(new DomainFailure.MissingField("name"));
        AtomicReference<FeatureRequest> received = new AtomicReference<>();

        StepVerifier.create(recording(request -> Mono.error(failure), received)
                        .execute(new FeatureRequest("u-1", null)))
                .verifyErrorMatches(e -> e == failure);
        assertNull(received.get());
    }

    @Test
    void should_hand_process_what_guard_emitted() {
        FeatureRequest normalized = new FeatureRequest("u-1", "normalized");
        AtomicReference<FeatureRequest> received = new AtomicReference<>();

        StepVerifier.create(recording(request -> Mono.just(normalized), received)
                        .execute(new FeatureRequest("u-1", "  raw  ")))
                .expectNextCount(1)
                .verifyComplete();
        assertSame(normalized, received.get());
    }

    private static UseCase<FeatureRequest, FeatureResponse> recording(
            Function<FeatureRequest, Mono<FeatureRequest>> guard,
            AtomicReference<FeatureRequest> received) {
        return new AbstractUseCase<>() {
            @Override
            protected Mono<FeatureRequest> guard(FeatureRequest request) {
                return guard.apply(request);
            }

            @Override
            protected Mono<FeatureResponse> process(FeatureRequest request) {
                received.set(request);
                return Mono.just(new FeatureResponse.Created("id", request.name()));
            }
        };
    }
}
