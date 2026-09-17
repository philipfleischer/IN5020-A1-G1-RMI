package com.ass1.server;

import com.ass1.common.City;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

/**
 * Starting one Server instance:
 *  1) Loading the dataset to memory (DatasetLoader.java)
 *  2) Starting a RMI-Registry
 *  3) Registrering the server under a name or id in the register.
 *
 * TODo: Change from manual static port to the proxy servers port (once the Proxy exists)
 */

public class Main {
    public static void main(String[] args) throws Exception {
        // TODo: take datasetPath/port as args instead of hardcoding, once we need more than one server

        String datasetPath = "data/exercise_1_dataset.csv";
        int port = 1099; // A standard RMI port

        List<City> cities = DatasetLoader.load(datasetPath);
        System.out.println("Server: loading in " + cities.size() + " cities.");

        Server server = new Server(cities);

        // "StatisticsServer" is the name Client.java looks this stub up by.
        Registry registry = LocateRegistry.createRegistry(port);
        registry.rebind("StatisticsServer", server);

        System.out.println("Server running and waiting on call, port " + port);
    }
}
