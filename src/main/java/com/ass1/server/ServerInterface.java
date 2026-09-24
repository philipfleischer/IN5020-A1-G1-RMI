package com.ass1.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface for a zone server.
 *
 * Every query method returns a QueryResult instead of a plain number,
 * since the client needs the waiting/execution time and serving zone
 * alongside the actual answer.
 */
public interface ServerInterface extends Remote {

    /* e.g. getPopulationofCountry("Norway") -> QueryResult wrapping 3162856L */
    QueryResult getPopulationofCountry(String countryName) throws RemoteException;

    /* Counts cities in countryName with population >= ("min") or <= ("max") threshold. */
    QueryResult getNumberofCities(String countryName, int threshold, String comp) throws RemoteException;

    /* Counts countries with at least cityCount cities matching the threshold/comp condition. */
    QueryResult getNumberofCountries(int cityCount, int threshold, String comp) throws RemoteException;

    /* Counts countries with at least cityCount cities whose population is between min and max. */
    QueryResult getNumberofCountriesMM(int cityCount, int minPopulation, int maxPopulation) throws RemoteException;

    /* Current number of tasks in this server's waiting list. Used by the proxy to check load. */
    int getWaitingListSize() throws RemoteException;

    /* The zone number this server belongs to. */
    int getZone() throws RemoteException;
}
