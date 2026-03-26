package com.usecase.model.request;

import java.lang.reflect.Field;
import java.util.Map;

public class RequestFactoryImpl implements RequestFactory {

    private final String basePackage;

    public RequestFactoryImpl(String basePackage) {
        this.basePackage = basePackage;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Request get(String requestName, Map<String, Object> params) {
        try {
            Class<? extends Request> clazz =
                    (Class<? extends Request>) Class.forName(basePackage + "." + requestName);
            return populate(clazz.getDeclaredConstructor().newInstance(), params);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown request: " + requestName, e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not instantiate request: " + requestName, e);
        }
    }

    private <T extends Request> T populate(T request, Map<String, Object> params) {
        if (params == null) return request;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            try {
                Field field = request.getClass().getDeclaredField(entry.getKey());
                field.setAccessible(true);
                field.set(request, entry.getValue());
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }
        return request;
    }
}

