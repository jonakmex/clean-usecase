package com.usecase.feature;

import com.usecase.AbstractUseCase;
import com.usecase.model.request.FeatureRequest;
import com.usecase.model.response.FeatureResponse;
import com.usecase.shared.DomainFailure;
import com.usecase.shared.UseCaseException;
import reactor.core.publisher.Mono;
import java.util.UUID;

public class FeatureUseCase extends AbstractUseCase<FeatureRequest, FeatureResponse> {

    private final UserGateway userGateway;

    public FeatureUseCase(UserGateway userGateway) {
        this.userGateway = userGateway;
    }

    // ── Guard: only checks that need no fetch ─────────────────────────────────

    @Override
    protected Mono<FeatureRequest> guard(FeatureRequest request) {
        return Mono.just(request)
                .flatMap(this::userIdMustBePresent)
                .flatMap(this::nameMustBePresent);
    }

    private Mono<FeatureRequest> userIdMustBePresent(FeatureRequest request) {
        if (request.userId() == null) {
            return Mono.error(new UseCaseException(new DomainFailure.MissingField("userId")));
        }
        return Mono.just(request);
    }

    private Mono<FeatureRequest> nameMustBePresent(FeatureRequest request) {
        if (request.name() == null) {
            return Mono.error(new UseCaseException(new DomainFailure.MissingField("name")));
        }
        return Mono.just(request);
    }

    // ── Process: one fetch, existence and permission resolved on its result ──

    @Override
    protected Mono<FeatureResponse> process(FeatureRequest request) {
        return userGateway.findById(request.userId())
                .switchIfEmpty(Mono.error(() -> new UseCaseException(
                        new DomainFailure.NotFound("user", request.userId()))))
                .flatMap(user -> user.canCreateFeatures()
                        ? Mono.just(success(request))
                        : Mono.error(new UseCaseException(
                                new DomainFailure.Forbidden("user cannot create features"))));
    }

    // ── Response builders ─────────────────────────────────────────────────────

    private FeatureResponse success(FeatureRequest request) {
        return new FeatureResponse.Success(UUID.randomUUID().toString(), request.name());
    }
}
