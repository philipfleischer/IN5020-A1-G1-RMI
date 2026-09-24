package com.ass1.common;

/**
 * Builds a single string key for one query, so it can be used to look the
 * query up in Cache. Combines the method name with all its arguments,
 * separated by "|" (e.g. "getPopulationofCountry|Norway"), so two different
 * queries never collide and the same query always maps to the same entry.
**/
public class CacheKey {
    public static String makeKey(String methodName, Object... parameters) {

        StringBuilder key = new StringBuilder(methodName);

        for (Object parameter : parameters) {
            key.append("|").append(parameter);
        }

        return key.toString();
    }
}
