package com.usecase.model.request;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
public class FeatureRequest extends Request {
    public String name;
}

