package com.ass1.server;

import com.ass1.proxy.ProxyInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Starts one Server instance:
 *  1) Register with the proxy, telling it which zone we want to be.
 *  2) If the proxy accepts (zone was free), start our own RMI registry
 *     and bind ourselves under "Server-Zone-<zone>" - that's the name
 *     both the Client and the Proxy's background load-refresh look us
 *     up by (see Proxy.refreshLoad()).
 *
 * Usage: java com.ass1.server.Main <zone> [port] [datasetPath] [cacheMode]
 * cacheMode is "none", "FIFO", or "OLDEST" - see Server.java.
 */
public class Main {
    private static final String PROXY_HOST = "localhost";
    private static final int PROXY_PORT = 1100; // matches proxy/Main.java

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java com.ass1.server.Main <zone> [port] [datasetPath] [cacheMode]");
            return;
        }

        int zone = Integer.parseInt(args[0]);
        int myPort = args.length > 1 ? Integer.parseInt(args[1]) : 1099;
        String datasetPath = args.length > 2 ? args[2] : "data/exercise_1_dataset.csv";
        String cacheMode = args.length > 3 ? args[3] : "none";
        String myHost = "localhost";

        // 1) Ask the proxy to claim this zone for us.
        Registry proxyRegistry = LocateRegistry.getRegistry(PROXY_HOST, PROXY_PORT);
        ProxyInterface proxy = (ProxyInterface) proxyRegistry.lookup("ProxyService");
        boolean accepted = proxy.registerServer(myHost, myPort, zone);

        if (!accepted) {
            System.out.println("Proxy rejected zone " + zone + " (out of range or already taken). Exiting.");
            return;
        }
        System.out.println("Registered with proxy for zone " + zone);

        // 2) Create the server and start serving.
        Server server = new Server(zone, datasetPath, cacheMode);

        Registry myRegistry = LocateRegistry.createRegistry(myPort);
        myRegistry.rebind("Server-Zone-" + zone, server);

        System.out.println("Server for zone " + zone + " running on port " + myPort
                + " (cache: " + cacheMode + ")...");
    }
}