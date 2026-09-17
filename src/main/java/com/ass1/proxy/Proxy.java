package com.ass1.proxy;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;

/**
 * The actual proxy/load-balancer object. Keeps track of every server that has registered,
 * grouped by zone, and decides which server each client should actually be sent to.
 * TODo: getting the real queue length off a server needs some kind of getQueueLength() (or similar) on ServerInterface, which doesn't exist yet since the queue itself isn't built.
 */
public class Proxy extends UnicastRemoteObject implements ProxyInterface {

    // zone number -> the server registered in that zone. Map instead of an array/list since
    // zones don't have to be filled in order and some might not have a server at all.
    private final Map<Integer, ServerEntry> serversByZone = new ConcurrentHashMap<>();

    // Next zone number to hand out. Starts at 1, just counts up every time a new server registers.
    private int nextZoneNumber = 1;

    // Protected here since we do not want anyone to create a Proxy instance.
    protected Proxy() throws RemoteException {
        super();
    }

    // Marked synchronized because multiple Servers could technically call this at the exact
    // same time (each on its own RMI thread) when the group is starting them all up together.
    // Without synchronized, two servers could both read nextZoneNumber before either one
    // increments it and end up getting handed the same zone number, which would break everything.
    @Override
    public synchronized int registerServer(String host, int port) throws RemoteException {
        int assignedZone = nextZoneNumber;
        nextZoneNumber++;

        ServerEntry entry = new ServerEntry(host, port, assignedZone);
        serversByZone.put(assignedZone, entry);

        System.out.println("[PROXY] New server registered: " + host + ":" + port + " -> zone " + assignedZone);

        // Server keeps this around too so it can print/log its own zone number if it wants to.
        return assignedZone;
    }

    @Override
    public ServerLocation getServerForZone(int zone) throws RemoteException {
        // TODo: this is the main decision logic described in the assignment (section "1 Proxy Server").
        // Steps:
        //   1) fix up "zone" with resolveZone(), in case nobody registered there
        //   2) if that zones server is nit overloaded, just send the client there
        //   3) if it is overloaded, check every other server and pick whichever has the
        //      fewest requests waiting, and break ties by picking whoever is closest clockwise
        //   4) if literally every server is overloaded, fall back to the original zone anyway
        //   5) call maybeRefreshLoad() on whichever server ends up getting picked

        if (serversByZone.isEmpty()) {
            // Nobody has registered at all yet, therefore there is nothing to do
            throw new RemoteException("No servers registered with the proxy-server as of this moment.");
        }

        // Step 1: If the requested zone does not actually have a server, we walk clockwise to find one that does.
        // If zone 6 does not exist but 7 does, then we treat this request as if it came from zone 7
        int actualZone = resolveZone(zone);
        ServerEntry homeServer = serversByZone.get(actualZone);

        // Step 2: Scenario A - server is not overloaded and we send client there
        if (!isOverloaded(homeServer)) {
            maybeRefreshLoad(homeServer);
            return toLocation(homeServer);
        }

        // Step 3: Home server is overloaded, we choose the one with least load/requests in queue.
        // If multiple have the same minimum load, then we choose the one physically closest
        ServerEntry bestCanditate = null;

        for (ServerEntry candidate : serversByZone.values()) {
            if (candidate == homeServer) {
                continue; // Checked in step 2, but oh well
            }
            if (isOverloaded(candidate)) {
                continue; // Overloaded as well, so we skip
            }

            boolean isBetter = bestCanditate == null
                    || candidate.lastKnownQueueLength < bestCanditate.lastKnownQueueLength
                    || (candidate.lastKnownQueueLength == bestCanditate.lastKnownQueueLength
                            && distanceClockwise(actualZone, candidate.zone) < distanceClockwise(actualZone,
                                    bestCanditate.zone));

            if (isBetter) {
                bestCanditate = candidate;
            }
        }

        // Step 4: Every server is currently overloaded, so we fall back to the original server we first started with for the process´s zone
        ServerEntry chosen = (bestCanditate != null) ? bestCanditate : homeServer;

        maybeRefreshLoad(chosen);
        return toLocation(chosen);
    }

    // Marshalling the bookkeeping object into a small Serializable object that can be sent as bytes, so that we can send it back to the client over RMI.
    private ServerLocation toLocation(ServerEntry entry) {
        return new ServerLocation(entry.host, entry.port, entry.zone);
    }

    // If nobody registered in "zone", we walk clockwise to the next zone number that does
    // have a server and use that one instead (wrapping back to zone 1 after the highest zone).
    // Called first thing inside getServerForZone.
    private int resolveZone(int zone) {
        // TODo: implement
        return zone;
    }

    // A server counts as "overloaded" once it has 18 or more requests sitting in its waiting list.
    private boolean isOverloaded(ServerEntry entry) {
        // TODo: implement
        return false;
    }

    // How many zones apart two zones are, going clockwise from fromZone to toZone. Used both
    // for the neighbor tie-break rule and for the simulated network delay formula
    // (80 + distance * 30 ms) that the client/server side needs for neighbor-zone requests.
    private int distanceClockwise(int fromZone, int toZone) {
        // TODo: implement
        return 0;
    }

    // Every 18th time we hand this particular server out to a client, we are supposed to go
    // check in on it and update lastKnownQueueLength, but that has to run on its own thread
    // so the client is not stuck waiting around for it. Called at the end of getServerForZone.
    private void maybeRefreshLoad(ServerEntry entry) {
        // TODo: implement
    }
}
