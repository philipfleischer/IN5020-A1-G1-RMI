package com.ass1.proxy;

import java.rmi.Remote;
import java.rmi.RemoteException;

// This is a remote interface for the Proxy (the load balancer). Two different callers use this:
//   1) Every Server calls registerServer() once on startup so the proxy knows it exists.
//   2) The Client calls getServerForZone() once per query, to find out which actual
//      server it should send the real request to.
public interface ProxyInterface extends Remote {

    // Called by a Server (from Main.java) when it boots up, telling the proxy which
    // zone it wants to represent. Returns true if the zone was free and got
    // claimed, false if some other server already grabbed that zone number first.
    boolean registerServer(String host, int port, int zone) throws RemoteException;

    // Called by the Client once per query line, using the Zone:# from that line in the
    // input file. Returns the host+port of whichever server the client should actually talk to
    ServerLocation getServerForZone(int zone) throws RemoteException;
}
