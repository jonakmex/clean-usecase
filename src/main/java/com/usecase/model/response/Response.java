package com.usecase.model.response;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
public class Response {
    public String status;
    public String error;
}

