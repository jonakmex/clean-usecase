package com.usecase.feature;

import com.usecase.model.request.FeatureRequest;
import com.usecase.model.response.FeatureResponse;
import com.usecase.shared.UseCaseException;
import com.usecase.shared.ValidationFailure;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class FeatureUseCaseTest {

    private final FeatureUseCase useCase = new FeatureUseCase();

    @Test
    void should_return_success_when_name_is_provided() {
        StepVerifier.create(useCase.execute(new FeatureRequest("Jonathan")))
                .assertNext(response -> {
                    Assertions.assertInstanceOf(FeatureResponse.Success.class, response);
                    FeatureResponse.Success success = (FeatureResponse.Success) response;
                    Assertions.assertNotNull(success.id());
                    Assertions.assertEquals("Jonathan", success.name());
                })
                .verifyComplete();
    }

    @Test
    void should_signal_error_when_name_is_null() {
        StepVerifier.create(useCase.execute(new FeatureRequest(null)))
                .verifyErrorMatches(e -> e instanceof UseCaseException uce
                        && uce.failure() instanceof ValidationFailure.MissingField missingField
                        && "name".equals(missingField.field()));
    }
}
