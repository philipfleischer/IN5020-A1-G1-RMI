package com.ass1.proxy;

/**
 * Manual test class - not part of the actual system, just something we run once
 * to sanity check that the zone routing logic in Proxy actually behaves the way
 * the assignment describes. We call Proxy directly here (not through RMI),
 * which
 * we can do since we're in the same package as Proxy and its constructor.
 *
 * This file should NOT be included in the final submission - it's a scratch
 * tool.
 */
public class ProxyLogicTest {

    public static void main(String[] args) throws Exception {
        Proxy proxy = new Proxy();

        // Register 5 servers, one after another. Since registerServer() hands out
        // zone numbers in the order it's called, this gives us zones 1, 2, 3, 4, 5 -
        // matching the 5-zone example from the assignment PDF (Figure 1).
        proxy.registerServer("localhost", 5001); // zone 1
        proxy.registerServer("localhost", 5002); // zone 2
        proxy.registerServer("localhost", 5003); // zone 3
        proxy.registerServer("localhost", 5004); // zone 4
        proxy.registerServer("localhost", 5005); // zone 5

        System.out.println("--- Test 1: home zone is not overloaded ---");
        // Nobody's queue length has been touched, so every server defaults to 0 -
        // meaning "not overloaded". A client asking for zone 3 should just get zone 3
        // back.
        ServerLocation result1 = proxy.getServerForZone(3);
        printResult("Client asks for zone 3, expect zone 3", result1, 3);

        System.out.println("--- Test 2: home zone IS overloaded, one clear best neighbor ---");
        // Fake zone 1 being overloaded (18+), and give zone 2 the lowest queue length
        // of the remaining servers. Zone 2 should win, since it has the fewest requests
        // waiting.
        proxy.forceQueueLengthForTesting(1, 20); // overloaded
        proxy.forceQueueLengthForTesting(2, 3); // lowest queue among the rest
        proxy.forceQueueLengthForTesting(3, 10);
        proxy.forceQueueLengthForTesting(4, 10);
        proxy.forceQueueLengthForTesting(5, 10);
        ServerLocation result2 = proxy.getServerForZone(1);
        printResult("Client asks for zone 1 (overloaded), expect zone 2 (lowest queue)", result2, 2);

        System.out.println("--- Test 3: tie on queue length, break tie by closest clockwise ---");
        // Zone 1 overloaded again. This time zones 3 and 5 are tied at the same queue
        // length. Going clockwise from zone 1, zone 3 is closer (distance 2) than zone
        // 5
        // (distance 4), so zone 3 should win the tie-break.
        proxy.forceQueueLengthForTesting(1, 20); // overloaded
        proxy.forceQueueLengthForTesting(2, 15);
        proxy.forceQueueLengthForTesting(3, 5); // tied for lowest...
        proxy.forceQueueLengthForTesting(4, 15);
        proxy.forceQueueLengthForTesting(5, 5); // ...with this one, but zone 3 is closer
        ServerLocation result3 = proxy.getServerForZone(1);
        printResult("Client asks for zone 1, tie between zone 3 and 5, expect zone 3 (closer clockwise)", result3, 3);

        System.out.println("--- Test 4: every server overloaded, fall back to home zone ---");
        // All 5 zones overloaded now. Per the assignment, if everyone is overloaded we
        // just give up and send the client to their own home zone server anyway.
        for (int zone = 1; zone <= 5; zone++) {
            proxy.forceQueueLengthForTesting(zone, 25);
        }
        ServerLocation result4 = proxy.getServerForZone(2);
        printResult("Client asks for zone 2, everyone overloaded, expect zone 2 (fallback)", result4, 2);

        System.out.println("--- Test 5: requested zone doesn't exist, resolve clockwise ---");
        // We only registered zones 1-5, so asking for zone 8 should wrap around and
        // land
        // on the smallest zone we have, which is zone 1 (same example logic as the
        // PDF).
        ServerLocation result5 = proxy.getServerForZone(8);
        printResult("Client asks for zone 8 (doesn't exist), expect it to resolve to zone 1", result5, 1);
    }

    // Small helper just so we don't repeat the same print/compare logic 5 times
    // above.
    private static void printResult(String description, ServerLocation actual, int expectedZone) {
        boolean passed = actual.zone == expectedZone;
        System.out.println((passed ? "[PASS] " : "[FAIL] ") + description
                + " -> got zone " + actual.zone + " (" + actual.host + ":" + actual.port + ")");
    }
}
