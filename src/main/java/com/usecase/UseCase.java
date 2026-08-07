package com.usecase;

import com.usecase.model.request.Request;
import com.usecase.model.response.Response;
import reactor.core.publisher.Mono;

public interface UseCase<R extends Request, S extends Response> {
    Mono<S> execute(R request);
}

