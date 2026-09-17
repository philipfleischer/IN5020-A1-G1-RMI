package com.ass1.server;

import com.ass1.common.City;
import com.ass1.proxy.ProxyInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        String datasetPath = "data/exercise_1_dataset.csv";

        // Port and zone now come from the command line instead of hardcoded
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 1099;
        int zone = args.length > 1 ? Integer.parseInt(args[1]) : 1;

        List<City> cities = DatasetLoader.load(datasetPath);
        System.out.println("Server: loading in " + cities.size() + " cities.");

        Server server = new Server(cities);

        // "StatisticsServer" is the name Client.java looks this stub up by.
        Registry registry = LocateRegistry.createRegistry(port);
        registry.rebind("StatisticsServer", server);
        System.out.println("Server running and waiting on call, port " + port);

        // Tell the proxy we exist. The proxy Registry lives on port 1100
        // TODo: DOCKER for the hardcoded localhost, needs the IP address for the deployed container
        Registry proxyRegistry = LocateRegistry.getRegistry("localhost", 1100);
        ProxyInterface proxy = (ProxyInterface) proxyRegistry.lookup("ProxyService");

        boolean accepted = proxy.registerServer("localhost", port, zone);

        if (!accepted) {
            //if zone number out of range or some other server claimed it, we
            // keep open and running, but communicate it.
            System.out.println("[SERVER] - WARNING: Proxy rejected zone " + zone + " - already taken or out of range?");
        } else {
            System.out.println("[SERVER] - Registered with proxy as zone " + zone);
        }
    }
}
