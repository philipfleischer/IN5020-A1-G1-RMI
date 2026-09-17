package com.ass1.server;

import com.ass1.common.City;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The "UnicastRemoteObject" is used by inheriting from the class so that the object
 * is done available to the RMI/ServerInterface.java.
 * A "stub" ("stedfortreder") that the clients can call methods on over the network remotely.
 *
 * Naive implementation: every method scans the full "cities" list from scratch, no caching.
 * TODo: per-server FIFO queue + single execution thread, real execution/waiting time.
 */
public class Server extends UnicastRemoteObject implements ServerInterface {

    // Loading the entire dataset
    private final List<City> cities;

    protected Server(List<City> cities) throws RemoteException {
        super(); // The call that "transfers?" the object call to the RMI
        this.cities = cities;
    }

    // Blocks 80ms per call to simulate the network delay the assignment requires
    private void simulateNetworkLatency() {
        try {
            Thread.sleep(80);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Letting the Thread itself know if the interruption exception
        }
    }

    // true = "min" (at least threshold), false = "max" (at most threshold).
    private static boolean isMin(String comp) {
        return comp.equalsIgnoreCase("min");
    }

    @Override
    public long getPopulationofCountry(String countryName) throws RemoteException {

        simulateNetworkLatency();

        // Sum the population of every city whose country matches.
        long totalPopulation = cities.stream()
        .filter(city -> city.countryName.equalsIgnoreCase(countryName))
        .mapToLong(city -> city.population).sum();

        return totalPopulation;
    }

    @Override
    public int getNumberofCities(String countryName, long threshold, String comp) throws RemoteException {

        simulateNetworkLatency();

        // Count cities in the country that pass the threshold or comp filter.
        boolean atLeast = isMin(comp);

        return (int) cities.stream()
        .filter(city -> city.countryName.equalsIgnoreCase(countryName))
        .filter(city -> atLeast ? city.population >= threshold : city.population <= threshold)
        .count();
    }

    @Override
    public int getNumberofCountries(int cityCount, long threshold, String comp) throws RemoteException {

        simulateNetworkLatency();

        // 1. Keep cities passing threshold/comp. 2. Group by country, count cities per country.
        // 3. Count how many countries reached at least cityCount qualifying cities.
        boolean atLeast = isMin(comp);

        Map<String, Long> qualifyingCitiesPerCountry = cities.stream()
        .filter(city -> atLeast ? city.population >= threshold : city.population <= threshold)
        .collect(Collectors.groupingBy(city -> city.countryName, Collectors.counting()));

        return (int) qualifyingCitiesPerCountry.values()
        .stream()
        .filter(count -> count >= cityCount)
        .count();
    }

    @Override
    public int getNumberofCountriesMM(int cityCount, long minPopulation, long maxPopulation) throws RemoteException {

        simulateNetworkLatency();

        // Same as getNumberofCountries, but the filter is a population range instead of threshold/comp.
        Map<String, Long> qualifyingCitiesPerCountry = cities.stream()
        .filter(city -> city.population >= minPopulation && city.population <= maxPopulation)
        .collect(Collectors.groupingBy(city -> city.countryName, Collectors.counting()));

        return (int) qualifyingCitiesPerCountry.values()
        .stream()
        .filter(count -> count >= cityCount)
        .count();
    }
}
