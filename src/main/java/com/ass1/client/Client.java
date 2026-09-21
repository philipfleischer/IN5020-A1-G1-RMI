package com.ass1.client;
import com.ass1.common.Cache;
import com.ass1.common.CacheKey;
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
        // First arg picks the run mode, -> decides client cache and output file call
        // naive        -> NO client cache, NO server cache,  writes naive_server.txt
        // server-cache -> NO client cache, Yes server cache, writes server_cache.txt
        // client-cache -> YES client cache, YES server cache,  writes client_cache.txt

        String mode = args.length > 0 ? args[0] : "naive";
        // "FIFO" or "OLDEST"
        String evictionMethod = args.length > 1 ? args[1] : "FIFO";

        // T=50 or T=20
        int delayMs = args.length > 2 ? Integer.parseInt(args[2]) : 50;

        // Suffixed with the T value so a T=50 run and a T=20 run don't overwrite each
        // other - we need both for the graphs, so losing one by accident would be annoying.
        String outputFile = switch (mode) {
            case "naive" -> "naive_server_T" + delayMs + ".txt";
            case "server_cache" -> "server_cache_T" + delayMs + ".txt";
            case "client_cache" -> "client_cache_T" + delayMs + ".txt";
            default -> throw new IllegalArgumentException("Unknown mode: " + mode + " (expected naive/server_cache/client_cache)");
        };

        boolean clientCacheEnabled = mode.equals("client_cache");

        Registry proxyRegistry = LocateRegistry.getRegistry("localhost", 1100);
        ProxyInterface proxy = (ProxyInterface) proxyRegistry.lookup("ProxyService");

        List<Query> queries = Query.readQueries("input/exercise_1_input.txt");
        System.out.println("[CLIENT] -- Loaded " + queries.size() + " queries. Mode: " + mode);

        List<String> outputLines = new ArrayList<>();
        Map<String, List<Long>> turnaroundByMethod = new LinkedHashMap<>();
        Map<String, List<Long>> executionByMethod = new LinkedHashMap<>();
        Map<String, List<Long>> waitingByMethod = new LinkedHashMap<>();

        // Client cache
        final int MAX_CACHE_ENTRIES = 45; // task says client max entries is 45
        Cache<String, Object> cache = clientCacheEnabled ? new Cache<>(MAX_CACHE_ENTRIES, evictionMethod) : null;

        for (Query query : queries) {
            Thread.sleep(delayMs); // T = 50ms or T = 20ms between each query, picked via args[2]

            // Time the remote call itself -> this is the query's turnaround time.
            long start = System.currentTimeMillis();

            // One bad line in the input file (e.g. a missing country name - the real
            // input file has a couple of these) must not take down the other ~3000 queries.
            // Catch anything that goes wrong for this single query, log it, and move on.
            try {

                // Make cachekey
                String key = getCacheKey(query);

                // check client cache
                Object cachedResult = (cache != null) ? cache.get(key) : null;

                if (cachedResult != null) {
                    // CACHE HIT, print results
                    System.out.println("CLIENT CACHE HIT: " + key);

                    long turnaroundTime = System.currentTimeMillis() - start;

                    long executionTime = 0;
                    long waitingTime = 0;

                    outputLines.add(
                            cachedResult + " " + query.rawLine
                            + " (turnaround time: " + turnaroundTime
                            + " ms, execution time: " + executionTime
                            + " ms, waiting time: " + waitingTime
                            + " ms, processed by Server cache)"
                    );

                    turnaroundByMethod
                            .computeIfAbsent(query.methodName, k -> new ArrayList<>())
                            .add(turnaroundTime);

                    executionByMethod
                            .computeIfAbsent(query.methodName, k -> new ArrayList<>())
                            .add(executionTime);

                    waitingByMethod
                            .computeIfAbsent(query.methodName, k -> new ArrayList<>())
                            .add(waitingTime);

                } else {
                    // CASHE MISS... continue
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

                    // cache == null -> skip
                    if (cache != null) {
                        // add result to cache
                        cache.put(key, result.getValue());
                    }

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
                }
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
        try (PrintWriter writer = new PrintWriter(outputFile)) {
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

        System.out.println("[CLIENT] --> Wrote: " + outputLines.size() + " results to " + outputFile);
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

    // helper function to get the cache keys for each method
    private static String getCacheKey(Query query) {

        switch (query.methodName) {

            case "getPopulationofCountry" -> {
                String countryName = String.join(" ", query.argTokens);

                return CacheKey.makeKey(
                        query.methodName,
                        countryName
                );
            }

            case "getNumberofCities" -> {
                String comp = query.argTokens[query.argTokens.length - 1];
                int threshold = Integer.parseInt(
                        query.argTokens[query.argTokens.length - 2]
                );

                String countryName = String.join(" ",
                        Arrays.copyOfRange(
                                query.argTokens,
                                0,
                                query.argTokens.length - 2
                        ));

                return CacheKey.makeKey(
                        query.methodName,
                        countryName,
                        threshold,
                        comp
                );
            }

            case "getNumberofCountries" -> {
                int cityCount = Integer.parseInt(query.argTokens[0]);
                int threshold = Integer.parseInt(query.argTokens[1]);
                String comp = query.argTokens[2];

                return CacheKey.makeKey(
                        query.methodName,
                        cityCount,
                        threshold,
                        comp
                );
            }

            case "getNumberofCountriesMM" -> {
                int cityCount = Integer.parseInt(query.argTokens[0]);
                int minPop = Integer.parseInt(query.argTokens[1]);
                int maxPop = Integer.parseInt(query.argTokens[2]);

                return CacheKey.makeKey(
                        query.methodName,
                        cityCount,
                        minPop,
                        maxPop
                );
            }

            default -> throw new IllegalArgumentException(
                    "Unknown method: " + query.methodName
            );
        }
    }

    // Plain average, used for the "avg turn-around time" summary lines.
    private static double average(List<Long> values) {
        return values.stream().mapToLong(Long::longValue).average().orElse(0);
    }
}
