package com.usecase;

import com.usecase.model.request.Request;
import com.usecase.model.response.Response;
import reactor.core.publisher.Mono;

public abstract class AbstractUseCase<R extends Request, S extends Response> implements UseCase<R, S> {

    @Override
    public final Mono<S> execute(R request) {
        return Mono.just(request)
                .flatMap(this::guard)
                .flatMap(this::process);
    }

    protected Mono<R> guard(R request) {
        return Mono.just(request);
    }

    protected abstract Mono<S> process(R request);
}

