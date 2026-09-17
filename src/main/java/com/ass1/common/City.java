package com.ass1.common;

import java.io.Serializable;

// Just the City object we create in the DatasetLoader class, since it was better having it
// separated into a new file, I put it into the common folder, Think this is correct practice.
public class City implements Serializable {
    public final long geonameId;
    public final String name;
    public final String countryCode;
    public final String countryName;
    public final long population;
    public final String timezone;
    public final double latitude;
    public final double longitude;

    public City(long geonameId, String name, String countryCode, String countryName, long population, String timezone, double latitude, double longitude) {
        this.geonameId = geonameId;
        this.name = name;
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.population = population;
        this.timezone = timezone;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Modified the toString to have only what we want to know, the name and population.
    @Override
    public String toString() {
        return name + " (" + countryName + "), population num=" + population;
    }
}
