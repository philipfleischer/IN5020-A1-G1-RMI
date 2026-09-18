package com.ass1.client;
import com.ass1.proxy.ProxyInterface;
import com.ass1.proxy.ServerLocation;
import com.ass1.server.ServerInterface;
import com.ass1.server.QueryResult;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.List;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// The client reads a input file, sends it to the server using RMI and writes results to an output file.
// Line parsing (Query.parseLine / Query.readQueries) lives in Query.java, next to this file.
public class Client {
    public static void main(String[] args) throws Exception {
        // Proxy is the only fixed address the client needs to know, and
        // every server adress comes as the proxy instead, per query
        Registry proxyRegistry = LocateRegistry.getRegistry("localhost", 1100);
        ProxyInterface proxy = (ProxyInterface) proxyRegistry.lookup("ProxyService");

        List<Query> queries = Query.readQueries("input/exercise_1_input.txt");
        System.out.println("[CLIENT] -- Loaded " + queries.size() + " queries.");

        List<String> outputLines = new ArrayList<>();
        // Turnaround/execution/waiting times per method name, used to build
        // the avg/min/max summary lines below.
        Map<String, List<Long>> turnaroundByMethod = new LinkedHashMap<>();
        Map<String, List<Long>> executionByMethod = new LinkedHashMap<>();
        Map<String, List<Long>> waitingByMethod = new LinkedHashMap<>();

        for (Query query : queries) {
            Thread.sleep(50); // T = 50ms between each query, T = 20ms for tests later

            // Time the remote call itself -> this is the query's turnaround time.
            long start = System.currentTimeMillis();

            // One bad line in the input file (e.g. a missing country name - the real
            // input file has a couple of these) must not take down the other ~3000 queries.
            // Catch anything that goes wrong for this single query, log it, and move on.
            try {
                // Ask the proxy which server should actually handle this query's zone.
                ServerLocation location = proxy.getServerForZone(query.zone);

                // Simulate the extra distance cost of a neighbor-zone request (0ms if it's
                // the same zone - see Proxy.toLocation()). The base 80ms network delay is
                // already simulated server-side, inside Server.submitAndWait().
                if (location.extraNetworkDelayMs > 0) {
                    Thread.sleep(location.extraNetworkDelayMs);
                }

                // Connect to whichever server the proxy picked, and make the real call.
                // Bound as "Server-Zone-<zone>" - see Server.main().
                Registry serverRegistry = LocateRegistry.getRegistry(location.host, location.port);
                ServerInterface server = (ServerInterface) serverRegistry.lookup("Server-Zone-" + location.zone);
                QueryResult result = invoke(server, query);

                long turnaroundTime = System.currentTimeMillis() - start;

                // Real waiting/execution time and the actual serving zone come straight from
                // the server's own QueryResult now, instead of being hardcoded to 0.
                long executionTime = result.getExecutionTimeMs();
                long waitingTime = result.getWaitingTimeMs();
                int servedByZone = result.getServedByZone();

                outputLines.add(result.getValue() + " " + query.rawLine
                        + " (turnaround time: " + turnaroundTime + " ms, execution time: " + executionTime
                        + " ms, waiting time: " + waitingTime + " ms, processed by Server " + servedByZone + ")");

                turnaroundByMethod.computeIfAbsent(query.methodName, k -> new ArrayList<>()).add(turnaroundTime);
                executionByMethod.computeIfAbsent(query.methodName, k -> new ArrayList<>()).add(executionTime);
                waitingByMethod.computeIfAbsent(query.methodName, k -> new ArrayList<>()).add(waitingTime);
            } catch (Exception e) {
                // Still one output line per input line, per the assignment's format - just
                // marked as an error instead of a real result, and left out of the avg/min/max
                // stats below since there's no valid timing data for a failed call.
                //
                // RMI wraps the real error in layers of RemoteException, and the resulting
                // message string has embedded newlines - walk down to the root cause and
                // strip any leftover line breaks, so this is still exactly one output line.
                Throwable rootCause = e;
                while (rootCause.getCause() != null) {
                    rootCause = rootCause.getCause();
                }
                String reason = rootCause.getMessage() != null ? rootCause.getMessage() : rootCause.toString();
                reason = reason.replaceAll("\\s+", " ").trim();

                outputLines.add("ERROR " + query.rawLine + " (" + reason + ")");
                System.out.println("[CLIENT] -- Query failed, skipping: " + query.rawLine + " -> " + e.getMessage());
            }
        }

        // Writes one line per query, then one avg/min/max summary line per method name
        try (PrintWriter writer = new PrintWriter("naive_server.txt")) {
            for (String line : outputLines) {
                writer.println(line);
            }

            for (Map.Entry<String, List<Long>> entry : turnaroundByMethod.entrySet()) {
                String method = entry.getKey();
                List<Long> turnarounds = entry.getValue();

                double avgTurnaround = average(turnarounds);
                long minTurnaround = Collections.min(turnarounds);
                long maxTurnaround = Collections.max(turnarounds);
                double avgExecution = average(executionByMethod.get(method));
                double avgWaiting = average(waitingByMethod.get(method));

                writer.println(method + " avg turn-around time: " + String.format(java.util.Locale.US, "%.4f", avgTurnaround)
                        + " ms, avg execution time: " + String.format(java.util.Locale.US, "%.4f", avgExecution)
                        + " ms, avg waiting time: " + String.format(java.util.Locale.US, "%.4f", avgWaiting)
                        + " ms, min turn-around time: "
                        + minTurnaround + " ms, max turn-around time: " + maxTurnaround + " ms");
            }
        }

        System.out.println("[CLIENT] --> Wrote: " + outputLines.size() + " results to naive_server.txt");
    }

    // The dispatch picks the matching remote method and pulls its arguments out of argTokens.
    // Each case splits argTokens differently, since the argument count differs per method.
    // Returns the full QueryResult now (value + timing info), not just the raw answer.
    private static QueryResult invoke(ServerInterface server, Query query) throws Exception {
        switch (query.methodName) {

            case "getPopulationofCountry" -> {
                // Every token is part of the country name. Can contain spaces, "French Guiana".
                String countryName = String.join(" ", query.argTokens);

                return server.getPopulationofCountry(countryName);
            }

            case "getNumberofCities" -> {
                // Last 2 tokens are threshold or comp, everything before that is the country name.
                String comp = query.argTokens[query.argTokens.length - 1];
                int threshold = Integer.parseInt(query.argTokens[query.argTokens.length - 2]);
                String countryName = String.join(" ",
                        Arrays.copyOfRange(query.argTokens, 0, query.argTokens.length - 2));

                return server.getNumberofCities(countryName, threshold, comp);
            }

            case "getNumberofCountries" -> {
                // Fixed order: cityCount, threshold, comp.
                int cityCount = Integer.parseInt(query.argTokens[0]);
                int threshold = Integer.parseInt(query.argTokens[1]);
                String comp = query.argTokens[2];

                return server.getNumberofCountries(cityCount, threshold, comp);
            }

            case "getNumberofCountriesMM" -> {
                // Fixed order: cityCount, minPopulation, maxPopulation.
                int cityCount = Integer.parseInt(query.argTokens[0]);
                int minPop = Integer.parseInt(query.argTokens[1]);
                int maxPop = Integer.parseInt(query.argTokens[2]);

                return server.getNumberofCountriesMM(cityCount, minPop, maxPop);
            }

            default -> throw new IllegalArgumentException("Unknown method: " + query.methodName);
        }
    }

    // Plain average, used for the "avg turn-around time" summary lines.
    private static double average(List<Long> values) {
        return values.stream().mapToLong(Long::longValue).average().orElse(0);
    }
}
