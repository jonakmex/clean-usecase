package com.usecase.feature;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface UserGateway {
    Mono<User> findById(String id);
}
