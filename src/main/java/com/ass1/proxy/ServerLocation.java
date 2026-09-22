package com.ass1.proxy;

import java.io.Serializable;

// This is a small object the proxy hands back to the client, that is just enough for the client to know
// where to send its actual request. Has to be Serializable since it travels over RMI.
public class ServerLocation implements Serializable {
    public final String host;
    public final int port;
    public final int zone;

    // Milliseconds client should simulate in addition to 80ms network delay, based on "distance".
    // Formula: same zone = 80ms total, neighbour zone = 80 + X*30 ms
    public final int extraNetworkDelayMs;

    public ServerLocation(String host, int port, int zone, int extraNetworkDelayMs) {
        this.host = host;
        this.port = port;
        this.zone = zone;
        this.extraNetworkDelayMs = extraNetworkDelayMs;
    }
}
