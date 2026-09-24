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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The client reads an input file, sends it to the server using RMI and writes results to an output file.
 * Line parsing (Query.parseLine / Query.readQueries) lives in Query.java, next to this file.
 **/
public class Client {

    /**
     * This class is what one query produced, once it's done. Filled in by
     * whichever thread ran that query, then read back in input-file order on the
     * main thread once every query has finished. methodName == null means the
     * query failed (ERROR line) and not counted in the avg/min/max stats below.
    **/
    private static class LineResult {
        String outputLine;
        String methodName;
        long turnaround;
        long execution;
        long waiting;
    }

    /**
     * This class is what we store in the client-side cache: the answer
     * itself, plus which server originally produced it, needed so a cache
     * hit can still print "processed by Server <server#>".
    **/
    private static class CachedAnswer {
        final Object value;
        final int servedByZone;

        CachedAnswer(Object value, int servedByZone) {
            this.value = value;
            this.servedByZone = servedByZone;
        }
    }

    public static void main(String[] args) throws Exception {
        /**
         * First arg picks the run mode, -> decides client cache and output file call
         * naive        -> NO client cache, NO server cache,  writes naive_server.txt
         * server-cache -> NO client cache, Yes server cache, writes server_cache.txt
         * client-cache -> YES client cache, YES server cache,  writes client_cache.txt
        **/

        String mode = args.length > 0 ? args[0] : "naive";
        // "FIFO" or "OLDEST"
        String evictionMethod = args.length > 1 ? args[1] : "FIFO";

        // T=50 or T=20
        int delayMs = args.length > 2 ? Integer.parseInt(args[2]) : 50;

        /**
         * Optional 4th argument for the input file path, so the same jar/image can
         * be pointed at a different file without editing code. For inside Docker,
         * where the container's working directory is not the project root.
        **/
        String inputPath = args.length > 3 ? args[3] : "input/exercise_1_input.txt";

        // Suffixed with the T value so a T=50 run and a T=20 run do not overwrite each other.
        String outputFile = switch (mode) {
            case "naive" -> "naive_server_T" + delayMs + ".txt";
            case "server_cache" -> "server_cache_T" + delayMs + ".txt";
            case "client_cache" -> "client_cache_T" + delayMs + ".txt";
            default -> throw new IllegalArgumentException("Unknown mode: " + mode + " (expected naive/server_cache/client_cache)");
        };

        boolean clientCacheEnabled = mode.equals("client_cache");

        /**
         * Set by docker-compose to the proxy service's container name. "localhost" only
         * works when everything runs on one machine, since inside a container "localhost"
         * means that container itself, not wherever the proxy actually is.
        **/
        String proxyHost = System.getenv().getOrDefault("PROXY_HOST", "localhost");
        Registry proxyRegistry = LocateRegistry.getRegistry(proxyHost, 1100);
        ProxyInterface proxy = (ProxyInterface) proxyRegistry.lookup("ProxyService");

        List<Query> queries = Query.readQueries(inputPath);
        System.out.println("[CLIENT] -- Loaded " + queries.size() + " queries. Mode: " + mode);

        // Client cache
        final int MAX_CACHE_ENTRIES = 45;
        Cache<String, CachedAnswer> cache = clientCacheEnabled ? new Cache<>(MAX_CACHE_ENTRIES, evictionMethod) : null;

        /**
         * One slot per query, filled in by the thread that runs it. Indexing by
         * the query's position is what keeps the output file in the same order as
         * the input file, even though queries now run concurrently and can finish in any order.
        **/
        LineResult[] results = new LineResult[queries.size()];

        /**
         * Each query gets handed to its own thread, and we only sleep T ms
         * between starting queries, meaning that we never wait for one to finish before
         * firing the next query. A cached thread pool grows or shrinks as needed and
         * reuses the threads once a query is completed.
        **/
        ExecutorService executor = Executors.newCachedThreadPool();

        for (int i = 0; i < queries.size(); i++) {
            Thread.sleep(delayMs); // T = 50ms or T = 20ms between each start

            int index = i;
            Query query = queries.get(i);
            executor.submit(() -> results[index] = runQuery(query, proxy, cache));
        }

        /**
         * At this point all queries have been started, one at every T ms. Now we wait here
         * for the slowest ones still in flight to actually finish before writing the file.
        **/
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.MINUTES);

        List<String> outputLines = new ArrayList<>();
        Map<String, List<Long>> turnaroundByMethod = new LinkedHashMap<>();
        Map<String, List<Long>> executionByMethod = new LinkedHashMap<>();
        Map<String, List<Long>> waitingByMethod = new LinkedHashMap<>();

        for (LineResult r : results) {
            outputLines.add(r.outputLine);
            if (r.methodName != null) {
                turnaroundByMethod.computeIfAbsent(r.methodName, k -> new ArrayList<>()).add(r.turnaround);
                executionByMethod.computeIfAbsent(r.methodName, k -> new ArrayList<>()).add(r.execution);
                waitingByMethod.computeIfAbsent(r.methodName, k -> new ArrayList<>()).add(r.waiting);
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

    /**
     * Runs exactly one query end-to-end (cache check, RMI call, formatting) and
     * returns its result row. It is called from a worker thread, so everything it
     * touches (the client cache, the RMI stubs) are safe to call concurrently
     * from many queries at once.
    **/
    private static LineResult runQuery(Query query, ProxyInterface proxy, Cache<String, CachedAnswer> cache) {
        LineResult r = new LineResult();

        // Timing the remote call itself -> this is for the query's turnaround time measurement.
        long start = System.currentTimeMillis();

        /**
         * One bad line in the input file (e.g. a missing country name) must not take down the other queries.
         * Catch anything that goes wrong for this single query, log it, and move on.
        **/
        try {
            String key = getCacheKey(query);

            // check client cache
            CachedAnswer cached = (cache != null) ? cache.get(key) : null;

            if (cached != null) {
                // CACHE HIT, print results
                System.out.println("CLIENT CACHE HIT: " + key);

                long turnaroundTime = System.currentTimeMillis() - start;

                r.methodName = query.methodName;
                r.turnaround = turnaroundTime;
                r.execution = 0;
                r.waiting = 0;
                // Same output format as a real remote answer: "processed by Server <server#>".
                r.outputLine = cached.value + " " + query.rawLine
                        + " (turnaround time: " + turnaroundTime
                        + " ms, execution time: 0 ms, waiting time: 0 ms, processed by Server "
                        + cached.servedByZone + ")";
                return r;
            }

            // CACHE MISS... continue
            // Ask the proxy which server should actually handle this query's zone.
            ServerLocation location = proxy.getServerForZone(query.zone);

            /**
             * Simulate the extra distance cost of a neighbor-zone request (0ms if it's
             * the same zone, see Proxy.toLocation()). The base 80ms network delay is
             * already simulated server-side, inside Server.submitAndWait().
            **/
            if (location.extraNetworkDelayMs > 0) {
                Thread.sleep(location.extraNetworkDelayMs);
            }

            // Connect to whichever server the proxy picked, and make the real call.
            Registry serverRegistry = LocateRegistry.getRegistry(location.host, location.port);
            ServerInterface server = (ServerInterface) serverRegistry.lookup("Server-Zone-" + location.zone);
            QueryResult result = invoke(server, query);

            long turnaroundTime = System.currentTimeMillis() - start;
            long executionTime = result.getExecutionTimeMs();
            long waitingTime = result.getWaitingTimeMs();
            int servedByZone = result.getServedByZone();

            // cache == null -> skip
            if (cache != null) {
                cache.put(key, new CachedAnswer(result.getValue(), servedByZone));
            }

            r.methodName = query.methodName;
            r.turnaround = turnaroundTime;
            r.execution = executionTime;
            r.waiting = waitingTime;
            r.outputLine = result.getValue() + " " + query.rawLine
                    + " (turnaround time: " + turnaroundTime + " ms, execution time: " + executionTime
                    + " ms, waiting time: " + waitingTime + " ms, processed by Server " + servedByZone + ")";
            return r;

        } catch (Exception e) {
            /**
             * It is just marked as an error instead of a real result, and left out of the
             * avg/min/max stats below since there's no valid timing data for a failed call.
            **/
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            String reason = rootCause.getMessage() != null ? rootCause.getMessage() : rootCause.toString();
            reason = reason.replaceAll("\\s+", " ").trim();

            r.methodName = null; // excluded from stats
            r.outputLine = "ERROR " + query.rawLine + " (" + reason + ")";
            System.out.println("[CLIENT] -- Query failed, skipping: " + query.rawLine + " -> " + e.getMessage());
            return r;
        }
    }

    /**
     * The dispatch picks the matching remote method and pulls its arguments out of argTokens.
     * Each case splits argTokens differently, since the argument count differs per method.
     * Returns the full QueryResult now (value + timing info).
    **/
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
