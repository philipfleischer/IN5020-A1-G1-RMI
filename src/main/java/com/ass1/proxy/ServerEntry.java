package com.ass1.proxy;

// Everything the proxy needs to remember about one registered server.
// This is internal bookkeeping only and never gets sent over the network (what ServerLocation is for).
class ServerEntry {
    final String host;
    final int port;
    final int zone;

    // How many waiting requests this server had, last time we actually checked.
    // Only gets refreshed every 18 assignments
    int lastKnownQueueLength;

    // Counts how many times in a row we have handed this server out to a client since the last refresh. Back to 0 every time we refresh lastKnownQueueLength.
    int assignmentsSinceLastRefresh;

    ServerEntry(String host, int port, int zone) {
        this.host = host;
        this.port = port;
        this.zone = zone;
        this.lastKnownQueueLength = 0;
        this.assignmentsSinceLastRefresh = 0;
    }
}
