package com.ass1.proxy;

import java.rmi.Remote;
import java.rmi.RemoteException;

// This is a remote interface for the Proxy (the load balancer). Two different callers use this:
//   1) Every Server calls registerServer() once on startup so the proxy knows it exists.
//   2) The Client calls getServerForZone() once per query, to find out which actual
//      server it should send the real request to.
public interface ProxyInterface extends Remote {

    // Called by a Server (from Main.java) when it boots up. The Proxy - not the
    // Server - decides which zone the new server gets, handing out zone numbers
    // in ascending order as servers register. Returns the assigned zone number,
    // or -1 if every zone (1..TOTAL_ZONES) is already taken.
    int registerServer(String host, int port) throws RemoteException;

    // Called by the Client once per query line, using the Zone:# from that line in the
    // input file. Returns the host+port of whichever server the client should actually talk to
    ServerLocation getServerForZone(int zone) throws RemoteException;
}
