package com.usecase;

import com.usecase.model.request.Request;
import com.usecase.model.response.Response;
import reactor.core.publisher.Mono;

public abstract class AbstractUseCase<R extends Request> implements UseCase {

    @Override
    @SuppressWarnings("unchecked")
    public final Mono<Response> execute(Request request) {
        return Mono.just((R) request)
                .flatMap(this::guard)
                .flatMap(this::process);
    }

    protected Mono<R> guard(R request) {
        return Mono.just(request);
    }

    protected abstract Mono<Response> process(R request);
}

