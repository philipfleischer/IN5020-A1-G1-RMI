package com.ass1.client;
import com.ass1.server.ServerInterface;

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
        // Connect to the registry Main.java (server side) created, and get the remote stub.
        Registry registry = LocateRegistry.getRegistry(1099);
        ServerInterface server = (ServerInterface) registry.lookup("StatisticsServer");

        List<Query> queries = Query.readQueries("input/exercise_1_input.txt");
        System.out.println("[CLIENT] -- Loaded " + queries.size() + " queries.");

        List<String> outputLines = new ArrayList<>();
        // Turnaround times per method name, used to build the avg/min/max summary lines below.
        Map<String, List<Long>> turnaroundByMethod = new LinkedHashMap<>();

        for (Query query : queries) {
            Thread.sleep(50); // T = 50ms between each query, T = 20ms for tests later

            // Time only the remote call itself -> this is the query's turnaround time.
            long start = System.currentTimeMillis();
            String result = invoke(server, query);
            long turnaroundTime = System.currentTimeMillis() - start;

            // Placeholder until Server has a real request queue,
            // TODO: Should come back from the server instead of being hardcoded here.
            long executionTime = 0;
            long waitingTime = 0;

            outputLines.add(result + " " + query.rawLine
                    + " (turnaround time: " + turnaroundTime + " ms, execution time: " + executionTime
                    + " ms, waiting time: " + waitingTime + " ms, processed by Server 1)");

            turnaroundByMethod.computeIfAbsent(query.methodName, k -> new ArrayList<>()).add(turnaroundTime);
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

                writer.println(method + " avg turn-around time: " + String.format(java.util.Locale.US, "%.4f", avgTurnaround)
                        + " ms, avg execution time: 0 ms, avg waiting time: 0 ms, min turn-around time: "
                        + minTurnaround + " ms, max turn-around time: " + maxTurnaround + " ms");
            }
        }

        System.out.println("[CLIENT] --> Wrote: " + outputLines.size() + " results to naive_server.txt");
    }

    // The dispatch picks the matching remote method and pulls its arguments out of argTokens.
    // Each case splits argTokens differently, since the argument count differs per method.
    private static String invoke(ServerInterface server, Query query) throws Exception {
        switch (query.methodName) {

            case "getPopulationofCountry" -> {
                // Every token is part of the country name. Can contain spaces, "French Guiana".
                String countryName = String.join(" ", query.argTokens);

                return String.valueOf(server.getPopulationofCountry(countryName));
            }

            case "getNumberofCities" -> {
                // Last 2 tokens are threshold or comp, everything before that is the country name.
                String comp = query.argTokens[query.argTokens.length - 1];
                long threshold = Long.parseLong(query.argTokens[query.argTokens.length - 2]);
                String countryName = String.join(" ",
                        Arrays.copyOfRange(query.argTokens, 0, query.argTokens.length - 2));

                return String.valueOf(server.getNumberofCities(countryName, threshold, comp));
            }

            case "getNumberofCountries" -> {
                // Fixed order: cityCount, threshold, comp.
                int cityCount = Integer.parseInt(query.argTokens[0]);
                long threshold = Long.parseLong(query.argTokens[1]);
                String comp = query.argTokens[2];

                return String.valueOf(server.getNumberofCountries(cityCount, threshold, comp));
            }

            case "getNumberofCountriesMM" -> {
                // Fixed order: cityCount, minPopulation, maxPopulation.
                int cityCount = Integer.parseInt(query.argTokens[0]);
                long minPop = Long.parseLong(query.argTokens[1]);
                long maxPop = Long.parseLong(query.argTokens[2]);

                return String.valueOf(server.getNumberofCountriesMM(cityCount, minPop, maxPop));
            }

            default -> throw new IllegalArgumentException("Unknown method: " + query.methodName);
        }
    }

    // Plain average, used for the "avg turn-around time" summary lines.
    private static double average(List<Long> values) {
        return values.stream().mapToLong(Long::longValue).average().orElse(0);
    }
}
