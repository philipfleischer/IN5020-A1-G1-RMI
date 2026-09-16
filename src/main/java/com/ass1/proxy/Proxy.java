package com.ass1.proxy;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The actual proxy/load-balancer object. Keeps track of every server that has registered,
 * grouped by zone, and decides which server each client should actually be sent to.
 * TODO: getting the real queue length off a server needs some kind of getQueueLength() (or similar) on ServerInterface, which doesn't exist yet since the queue itself isn't built.
 */

public class Proxy extends UnicastRemoteObject implements ProxyInterface {

    // zone number -> the server registered in that zone. Map instead of an array/list since
    // zones don't have to be filled in order and some might not have a server at all.
    private final Map<Integer, ServerEntry> serversByZone = new ConcurrentHashMap<>();

    // Next zone number to hand out. Starts at 1, just counts up every time a new server registers.
    private int nextZoneNumber = 1;

    protected Proxy() throws RemoteException {
        super();
    }

    @Override
    public int registerServer(String host, int port) throws RemoteException {
        // TODO: build a new ServerEntry for this host/port, give it nextZoneNumber, put it into serversByZone, bump nextZoneNumber by one, then return the zone number we gave it.
        return 0;
    }

    @Override
    public ServerLocation getServerForZone(int zone) throws RemoteException {
        // TODO: this is the main decision logic described in the assignment (section "1 Proxy Server").
        // Steps:
        //   1) fix up "zone" with resolveZone(), in case nobody registered there
        //   2) if that zones server is nit overloaded, just send the client there
        //   3) if it is overloaded, check every other server and pick whichever has the
        //      fewest requests waiting, and break ties by picking whoever is closest clockwise
        //   4) if literally every server is overloaded, fall back to the original zone anyway
        //   5) call maybeRefreshLoad() on whichever server ends up getting picked
        return null;
    }

    // If nobody registered in "zone", we walk clockwise to the next zone number that does
    // have a server and use that one instead (wrapping back to zone 1 after the highest zone).
    // Called first thing inside getServerForZone.
    private int resolveZone(int zone) {
        // TODO: implement
        return zone;
    }

    // A server counts as "overloaded" once it has 18 or more requests sitting in its waiting list.
    private boolean isOverloaded(ServerEntry entry) {
        // TODO: implement
        return false;
    }

    // How many zones apart two zones are, going clockwise from fromZone to toZone. Used both
    // for the neighbor tie-break rule and for the simulated network delay formula
    // (80 + distance * 30 ms) that the client/server side needs for neighbor-zone requests.
    private int distanceClockwise(int fromZone, int toZone) {
        // TODO: implement
        return 0;
    }

    // Every 18th time we hand this particular server out to a client, we are supposed to go
    // check in on it and update lastKnownQueueLength, but that has to run on its own thread
    // so the client is not stuck waiting around for it. Called at the end of getServerForZone.
    private void maybeRefreshLoad(ServerEntry entry) {
        // TODO: implement
    }
}
