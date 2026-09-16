package com.ass1.proxy;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/** Starts the Proxy on its own RMI registry, separate from any of the Servers.
 * Both the Servers (to register themselves) and the
 * Client (to ask for a server per zone) need to know this port to find the proxy.
 **/
public class Main {
    public static void main(String[] args) throws Exception {
        // TODO: pick port 1100 for example, that is different from the server port
        // TODO: create the Proxy object, start a registry on that port, and bind the Proxy under a name like "ProxyService" - same idea as how server/Main.java does it for "StatisticsServer".
    }
}
