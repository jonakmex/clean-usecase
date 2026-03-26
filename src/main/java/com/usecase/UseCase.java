package com.usecase;


import com.usecase.model.request.Request;
import com.usecase.model.response.Response;
import reactor.core.publisher.Mono;

public interface UseCase {
    Mono<Response> execute(Request request);
}

