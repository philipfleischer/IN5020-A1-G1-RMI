package com.ass1.server;

/**
 * One row from the dataset. Immutable value holder.
 */
public record CityRecord(String name, String countryName, long population) {
}