package com.ass1.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses the CSV dataset and answers the four statistics queries.
 *
 * Every public query method validates its arguments first and throws
 * IllegalArgumentException on bad input, instead of silently returning
 * a wrong answer or crashing. Server.java catches this per-task, so one
 * bad query never brings down the server or affects other queries.
 */
public class Dataset {

    public static List<CityRecord> parse(String csvPath) throws IOException {
        List<CityRecord> cities = new ArrayList<>(140_600);

        try (BufferedReader reader = Files.newBufferedReader(Path.of(csvPath))) {
            String line = reader.readLine(); // header, skip
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = line.split(";", -1);
                if (cols.length < 5) continue;

                String name = cols[1].trim();
                String countryName = cols[3].trim();
                long population;
                try {
                    population = Long.parseLong(cols[4].trim());
                } catch (NumberFormatException e) {
                    continue;
                }
                cities.add(new CityRecord(name, countryName, population));
            }
        }
        return cities;
    }

    // RMI Function Call 1
    public static long getPopulationofCountry(List<CityRecord> cities, String countryName) {
        validateCountryName(countryName);

        long sum = 0;
        for (CityRecord c : cities) {
            if (c.countryName().equalsIgnoreCase(countryName)) {
                sum += c.population();
            }
        }
        return sum; // 0 if the country doesn't exist, not an error, just no match
    }

    // RMI Function Call 2
    public static int getNumberofCities(List<CityRecord> cities, String countryName, int threshold, String comp) {
        validateCountryName(countryName);
        validateComp(comp);
        validateNonNegative(threshold, "threshold");

        boolean min = isMin(comp);
        int count = 0;
        for (CityRecord c : cities) {
            if (!c.countryName().equalsIgnoreCase(countryName)) continue;
            if (matches(c.population(), threshold, min)) {
                count++;
            }
        }
        return count;
    }

    // RMI Function Call 3
    public static int getNumberofCountries(List<CityRecord> cities, int cityCount, int threshold, String comp) {
        validateNonNegative(cityCount, "cityCount");
        validateNonNegative(threshold, "threshold");
        validateComp(comp);

        boolean min = isMin(comp);
        Map<String, Integer> matchingCitiesPerCountry = new HashMap<>();

        for (CityRecord c : cities) {
            if (matches(c.population(), threshold, min)) {
                matchingCitiesPerCountry.merge(c.countryName(), 1, Integer::sum);
            }
        }

        int result = 0;
        for (int n : matchingCitiesPerCountry.values()) {
            if (n >= cityCount) result++;
        }
        return result;
    }

    // RMI Function Call 4
    public static int getNumberofCountriesMM(List<CityRecord> cities, int cityCount, int minPopulation, int maxPopulation) {
        validateNonNegative(cityCount, "cityCount");
        validateNonNegative(minPopulation, "minPopulation");
        validateNonNegative(maxPopulation, "maxPopulation");
        if (minPopulation > maxPopulation) {
            throw new IllegalArgumentException(
                    "minPopulation (" + minPopulation + ") must not be greater than maxPopulation (" + maxPopulation + ")");
        }

        Map<String, Integer> matchingCitiesPerCountry = new HashMap<>();

        for (CityRecord c : cities) {
            long p = c.population();
            if (p >= minPopulation && p <= maxPopulation) {
                matchingCitiesPerCountry.merge(c.countryName(), 1, Integer::sum);
            }
        }

        int result = 0;
        for (int n : matchingCitiesPerCountry.values()) {
            if (n >= cityCount) result++;
        }
        return result;
    }

    // ================= Validation helpers =================

    private static void validateCountryName(String countryName) {
        if (countryName == null || countryName.isBlank()) {
            throw new IllegalArgumentException("countryName must not be empty");
        }
    }

    private static void validateComp(String comp) {
        if (comp == null || !(comp.equalsIgnoreCase("min") || comp.equalsIgnoreCase("max"))) {
            throw new IllegalArgumentException("comp must be 'min' or 'max', got: " + comp);
        }
    }

    private static void validateNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative, got: " + value);
        }
    }

    private static boolean isMin(String comp) {
        return "min".equalsIgnoreCase(comp);
    }

    private static boolean matches(long population, int threshold, boolean min) {
        return min ? population >= threshold : population <= threshold;
    }
}
