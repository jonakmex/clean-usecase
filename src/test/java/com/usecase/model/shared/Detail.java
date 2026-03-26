package com.usecase.model.shared;
import lombok.Builder;
@Builder
public class Detail {
    public String label;
    public boolean enabled;
    public Info info;
}
