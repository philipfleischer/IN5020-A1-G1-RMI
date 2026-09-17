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
 * Plain static functions not tied to caching or threading.
 */
public class Dataset {

    public static List<CityRecord> parse(String csvPath) throws IOException {
        List<CityRecord> cities = new ArrayList<>(140_600);

        try (BufferedReader reader = Files.newBufferedReader(Path.of(csvPath))) {
            String line = reader.readLine(); // header - skip
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

    // 2.1.a - Kept separate from RMI/queue logic on purpose: this is a pure,
    // static, testable function with no thread/queue/RMI dependency, so it
    // could be verified against the PDF's expected answers on its own.
    public static long getPopulationofCountry(List<CityRecord> cities, String countryName) {
        long sum = 0;
        for (CityRecord c : cities) {
            if (c.countryName().equalsIgnoreCase(countryName)) {
                sum += c.population();
            }
        }
        return sum;
    }

    // 2.1.b - isMin/matches are shared by all four methods instead of
    // repeating the same if/else logic - one place to fix if a bug appears.
    public static int getNumberofCities(List<CityRecord> cities, String countryName, int threshold, String comp) {
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

    // 2.1.c - Single pass over all cities (O(n)) using a map to count matches
    // per country, instead of looping countries x cities (O(n^2)). Matters
    // here since naive mode already re-parses 140k rows on every call.
    public static int getNumberofCountries(List<CityRecord> cities, int cityCount, int threshold, String comp) {
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

    // 2.1.d - Same as 2.1.c, but with a population range instead of a single threshold.
    public static int getNumberofCountriesMM(List<CityRecord> cities, int cityCount, int minPopulation, int maxPopulation) {
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

    private static boolean isMin(String comp) {
        return "min".equalsIgnoreCase(comp);
    }

    private static boolean matches(long population, int threshold, boolean min) {
        return min ? population >= threshold : population <= threshold;
    }
}