package com.ass1.server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Starting one Server instance:
 *  1) Starting a RMI-Registry
 *  2) Registrering the server under a name or id in the register.
 *
 * Note: the dataset is no longer loaded once here - Server itself calls
 * Dataset.parse(datasetPath) fresh on every single query (naive mode,
 * matching "parses the whole dataset every time a request is made").
 *
 * TODo: Change from manual static port to the proxy servers port (once the Proxy exists)
 */
public class Main {
    public static void main(String[] args) throws Exception {
        // TODo: take datasetPath/port as args instead of hardcoding, once we need more than one server

        String datasetPath = "data/exercise_1_dataset.csv";
        int port = 1099; // A standard RMI port
        int zone = 1;    // single-server naive setup for now, before the proxy assigns real zones

        Server server = new Server(zone, datasetPath);

        // "StatisticsServer" is the name Client.java looks this stub up by.
        Registry registry = LocateRegistry.createRegistry(port);
        registry.rebind("StatisticsServer", server);

        System.out.println("Server running and waiting on call, port " + port);
    }
}