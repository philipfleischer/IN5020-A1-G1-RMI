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

        // Register servers into SPECIFIC zones now, instead of letting Proxy hand out
        // zone numbers automatically. This lets us deliberately recreate the exact
        // "gap" example from the assignment PDF: zones 1, 3, 4, 5, 7 have a server,
        // but 2, 6 and 8 do NOT. registerServer() now returns true/false depending on
        // whether the zone was actually free to claim.
        registerAndCheck(proxy, "localhost", 5001, 1);
        registerAndCheck(proxy, "localhost", 5003, 3);
        registerAndCheck(proxy, "localhost", 5004, 4);
        registerAndCheck(proxy, "localhost", 5005, 5);
        registerAndCheck(proxy, "localhost", 5007, 7);

        System.out.println("--- Test: registering a zone that's already taken should fail ---");
        boolean shouldFail = proxy.registerServer("localhost", 9999, 3); // zone 3 is already taken above
        System.out.println((!shouldFail ? "[PASS] " : "[FAIL] ")
                + "Registering an already-taken zone was rejected: " + !shouldFail);

        System.out.println("--- Test 1: home zone is not overloaded ---");
        // Nobody's queue length has been touched, so every server defaults to 0 -
        // meaning "not overloaded". A client asking for zone 3 should just get zone 3
        // back.
        ServerLocation result1 = proxy.getServerForZone(3);
        printResult("Client asks for zone 3, expect zone 3", result1, 3);

        System.out.println("--- Test 2: home zone IS overloaded, one clear best neighbor ---");
        // Fake zone 1 being overloaded (18+), and give zone 3 the lowest queue length
        // of the remaining registered servers. Zone 3 should win, since it has the
        // fewest requests waiting.
        proxy.forceQueueLengthForTesting(1, 20); // overloaded
        proxy.forceQueueLengthForTesting(3, 3); // lowest queue among the rest
        proxy.forceQueueLengthForTesting(4, 10);
        proxy.forceQueueLengthForTesting(5, 10);
        proxy.forceQueueLengthForTesting(7, 10);
        ServerLocation result2 = proxy.getServerForZone(1);
        printResult("Client asks for zone 1 (overloaded), expect zone 3 (lowest queue)", result2, 3);

        System.out.println("--- Test 3: tie on queue length, break tie by closest clockwise ---");
        // Zone 1 overloaded again. This time zones 4 and 7 are tied at the same queue
        // length. Going clockwise from zone 1 (out of TOTAL_ZONES=8), zone 4 is closer
        // (distance 3) than zone 7 (distance 6), so zone 4 should win the tie-break.
        proxy.forceQueueLengthForTesting(1, 20); // overloaded
        proxy.forceQueueLengthForTesting(3, 15);
        proxy.forceQueueLengthForTesting(4, 5); // tied for lowest...
        proxy.forceQueueLengthForTesting(5, 15);
        proxy.forceQueueLengthForTesting(7, 5); // ...with this one, but zone 4 is closer
        ServerLocation result3 = proxy.getServerForZone(1);
        printResult("Client asks for zone 1, tie between zone 4 and 7, expect zone 4 (closer clockwise)", result3, 4);

        System.out.println("--- Test 4: every server overloaded, fall back to home zone ---");
        // All 5 registered zones overloaded now. Per the assignment, if everyone is
        // overloaded we just give up and send the client to their own home zone server
        // anyway.
        proxy.forceQueueLengthForTesting(1, 25);
        proxy.forceQueueLengthForTesting(3, 25);
        proxy.forceQueueLengthForTesting(4, 25);
        proxy.forceQueueLengthForTesting(5, 25);
        proxy.forceQueueLengthForTesting(7, 25);
        ServerLocation result4 = proxy.getServerForZone(4);
        printResult("Client asks for zone 4, everyone overloaded, expect zone 4 (fallback)", result4, 4);

        System.out.println("--- Test 5: requested zone doesn't exist, resolve clockwise ---");
        // Reset everyone back to not-overloaded so this test isn't polluted by Test 4's
        // state.
        proxy.forceQueueLengthForTesting(1, 0);
        proxy.forceQueueLengthForTesting(3, 0);
        proxy.forceQueueLengthForTesting(4, 0);
        proxy.forceQueueLengthForTesting(5, 0);
        proxy.forceQueueLengthForTesting(7, 0);

        // Zone 6 has no server. This is EXACTLY the example from the assignment PDF:
        // "client requests from Zone 6 will be automatically changed to 7".
        ServerLocation result5 = proxy.getServerForZone(6);
        printResult("Client asks for zone 6 (doesn't exist), expect it to resolve to zone 7", result5, 7);

        // Zone 8 has no server either, and there's nothing bigger than 8 to wrap to, so
        // it
        // should wrap all the way back around to the smallest registered zone, which is
        // 1.
        // Also exactly the PDF's example: "client requests from Zone 8 will be...
        // changed to Zone 1".
        ServerLocation result6 = proxy.getServerForZone(8);
        printResult("Client asks for zone 8 (doesn't exist), expect it to resolve to zone 1", result6, 1);
    }

    // Small helper so registering doesn't need 3 lines of boilerplate every time
    // above.
    private static void registerAndCheck(Proxy proxy, String host, int port, int zone) throws Exception {
        boolean accepted = proxy.registerServer(host, port, zone);
        System.out.println((accepted ? "[PASS] " : "[FAIL] ")
                + "Registered " + host + ":" + port + " into zone " + zone + " -> accepted: " + accepted);
    }

    // Small helper just so we don't repeat the same print/compare logic several
    // times above.
    private static void printResult(String description, ServerLocation actual, int expectedZone) {
        boolean passed = actual.zone == expectedZone;
        System.out.println((passed ? "[PASS] " : "[FAIL] ") + description
                + " -> got zone " + actual.zone + " (" + actual.host + ":" + actual.port + ")");
    }
}
