package com.usecase.model.request;

import com.usecase.model.shared.Complex;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.time.LocalDate;

@SuperBuilder
@NoArgsConstructor
public class FeatureRequest extends Request {
    // String
    public String name;
    // primitives
    public int age;
    public boolean active;
    public double score;
    // wrappers
    public Integer code;
    public Boolean verified;
    public Long timestamp;
    // date
    public LocalDate birthDate;
    // complex object
    public Complex complex;
}

