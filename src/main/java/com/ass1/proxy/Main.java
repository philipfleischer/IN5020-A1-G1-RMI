package com.ass1.proxy;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/** Starts the Proxy on its own RMI registry, separate from any of the Servers.
 * Both the Servers (to register themselves) and the
 * Client (to ask for a server per zone) need to know this port to find the proxy.
 **/
public class Main {
    public static void main(String[] args) throws Exception {
        /* Has to be a different port than the servers (they all sit on 1099), otherwise this
            registry would collide with whichever server happens to start first on this machine. */
        int port = 1100;

        Proxy proxy = new Proxy();

        /**
         * Start a fresh registry and bind our object under a name, so other JVMs
         * (the Servers registering, the Client asking for zones) can look it up
         * by that name instead of needing to know anything about the Proxy class itself.
        **/
        Registry registry = LocateRegistry.createRegistry(port);
        registry.rebind("ProxyService", proxy);

        System.out.println("Proxy running and waiting for servers/clients, port " + port);
    }
}
