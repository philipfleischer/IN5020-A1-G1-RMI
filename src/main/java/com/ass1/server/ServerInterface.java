package com.ass1.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
* The RMI-interface that the server is exposed to.
*
* Everything "REMOTE" bust have methods that throws RemoteException.
* The method names are taken word for word from the TA.
*/
public interface ServerInterface extends Remote{

    // Sum of the population for the country, by adding all the cities.
    long getPopulationofCountry(String countryName) throws RemoteException;

    // Only Counting CITIES that has population between min and max values.
    int getNumberofCities(String countryName, long threshold, String comp) throws RemoteException;

    // Only Counting CONTRIES that has at least "cityCount" cities and the cities are between min and max threshold.
    int getNumberofCountries(int cityCount, long threshold, String comp) throws RemoteException;

    // Counting COUNTRIES that has "cityCount" cities with population between minPopulation and maxPopulation
    int getNumberofCountriesMM(int cityCount, long minPopulation, long maxPopulation) throws RemoteException;
}
