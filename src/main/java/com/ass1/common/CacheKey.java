package com.ass1.common;

public class CacheKey {
    public static String makeKey(String methodName, Object... parameters) {

        StringBuilder key = new StringBuilder(methodName);

        for (Object parameter : parameters) {
            key.append("|").append(parameter);
        }

        return key.toString();
    }
}
