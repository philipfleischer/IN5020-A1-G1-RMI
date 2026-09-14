package com.ass1.server;

import com.ass1.common.City;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;
import java.util.List;

/**
 * The "UnicastRemoteObject" is used by inheriting from the class so that the object
 * is done available to the RMI/ServerInterface.java.
 * A "stub" ("stedfortreder") that the clients can call methods on over the network remotely.
 *
 * TODO: Implement the FIFO queue, threads and add the 80ms network delay.
 * This version is just a simple server so that it works before we implement the rest.
 */
public class Server extends UnicastRemoteObject implements ServerInterface {

    // Loading the entire dataset
    private final List<City> cities;

    protected Server(List<City> cities) throws RemoteException {
        super(); // The call that "transfers?" the object call to the RMI
        this.cities = cities;
    }

    // This is jsut to simulate the 80ms network delay that the assignment asks for, but this is a naive/crude way to do it, I think this is someone elses responsibility in the group.
    private void simulateNetworkLatency() {
        try {
            Thread.sleep(80);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Letting the Thread itself know if the interruption exception
        }
    }

    // Is "min" true?
    private static boolean isMin(String comp) {
        return comp.equalsIgnoreCase("min");
    }

    @Override
    public long getPopulationofCountry(String countryName) throws RemoteException {
        // TODO: Run through the cities List and count matches - The Naive emthod counting again.
        long norgePop = cities.stream().
        filter(city -> city.countryName.equalsIgnoreCase(countryName)).
        mapToLong(city -> city.population).sum();

        return norgePop;
    }

    @Override
    public int getNumberofCities(String countryName, long threshold, String comp) throws RemoteException {
        // TODO: Iterate cities and count for "countryName" and population for threshold comp.
        boolean atLeast = isMin(comp);

        return (int) cities.stream().
        filter(city -> city.countryName.equalsIgnoreCase(countryName)).
        filter(city -> atLeast ? city.population >= threshold : city.population <= threshold).
        count();
    }

    @Override
    public int getNumberofCountries(int cityCount, long threshold, String comp) throws RemoteException {
        // TODO: Group cities for the countries and count for threshold/comp and where num is >= cityCount.
        // 1. Filter cities on thres/comp, 2. Group remaining cities per country and count, 3. Count countries with atLeast min cityCount cities
        // Using Java Streams for this
        return 0;
    }

    @Override
    public int getNumberofCountriesMM(int cityCount, long minPopulation, long maxPopulation) throws RemoteException {
        // TODO: The same as above only BETWEEN min and max.
        return 0;
    }
}



//TODO: Use this, from the TA?
// public class Server implements ServerInterface{
//     public int Add(int num1, int num2) {
//         return num1 + num2;
//     }

//     public static void main(String[] args){
//         try {
//             Registry registry = LocateRegistry.getRegistry();
//             Server server = new Server();
//             ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(server, 0);
//             registry.bind("server", serverStub);
//         } catch (RemoteException | AlreadyBoundException e) {
//             e.printStackTrace();
//         }

//     }
// }
