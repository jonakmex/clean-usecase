package com.usecase;


import com.usecase.feature.FeatureUseCase;

public class UseCaseFactoryImpl implements UseCaseFactory {

    @Override
    public UseCase get(String useCaseName) {
        if ("FeatureUseCase".equals(useCaseName)) {
            return new FeatureUseCase();
        }
        throw new IllegalArgumentException("Unknown use case: " + useCaseName);
    }
}

