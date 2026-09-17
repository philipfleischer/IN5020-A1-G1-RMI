package com.ass1.proxy;

import java.io.Serializable;

// This is a small object the proxy hands back to the client, that is just enough for the client to know
// where to send its actual request. Has to be Serializable since it travels over RMI.
public class ServerLocation implements Serializable {
    public final String host;
    public final int port;
    public final int zone;

    public ServerLocation(String host, int port, int zone) {
        this.host = host;
        this.port = port;
        this.zone = zone;
    }
}
