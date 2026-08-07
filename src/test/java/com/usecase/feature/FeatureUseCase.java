package com.usecase.feature;

import com.usecase.AbstractUseCase;
import com.usecase.model.request.FeatureRequest;
import com.usecase.model.response.FeatureResponse;
import com.usecase.shared.UseCaseException;
import com.usecase.shared.ValidationFailure;
import reactor.core.publisher.Mono;
import java.util.UUID;

public class FeatureUseCase extends AbstractUseCase<FeatureRequest, FeatureResponse> {

    // ── Guard ─────────────────────────────────────────────────────────────────

    @Override
    protected Mono<FeatureRequest> guard(FeatureRequest request) {
        return Mono.just(request)
                .flatMap(this::nameMustBePresent)
                .flatMap(this::userMustExist)
                .flatMap(this::userMustHavePermission);
    }

    private Mono<FeatureRequest> nameMustBePresent(FeatureRequest request) {
        if (request.name() == null) {
            return Mono.error(new UseCaseException(new ValidationFailure.MissingField("name")));
        }
        return Mono.just(request);
    }

    private Mono<FeatureRequest> userMustExist(FeatureRequest request) {
        // userGateway.exists(request.userId)
        return Mono.just(request);
    }

    private Mono<FeatureRequest> userMustHavePermission(FeatureRequest request) {
        // permissionGateway.hasPermission(request.userId, "feature:execute")
        return Mono.just(request);
    }

    // ── Process ───────────────────────────────────────────────────────────────

    @Override
    protected Mono<FeatureResponse> process(FeatureRequest request) {
        return Mono.just(success(request));
    }

    // ── Response builders ─────────────────────────────────────────────────────

    private FeatureResponse success(FeatureRequest request) {
        return new FeatureResponse.Success(UUID.randomUUID().toString(), request.name());
    }
}
