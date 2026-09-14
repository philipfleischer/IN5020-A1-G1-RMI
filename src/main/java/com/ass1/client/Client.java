package com.ass1.client;
import com.ass1.server.ServerInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.List;


// The client reads a input file, sends it to the server using RMI and writes results to an output file.
// Line parsing (Query.parseLine / Query.readQueries) lives in Query.java, next to this file.
public class Client {
    public static void main(String[] args) throws Exception {
        Registry registry = LocateRegistry.getRegistry(1099);
        ServerInterface server = (ServerInterface) registry.lookup("StatisticsServer");

        List<Query> queries = Query.readQueries("input/exercise_1_input.txt");
        System.out.println("[CLIENT] -- Loaded " + queries.size() + " queries.");

        for (Query query : queries) {
            Thread.sleep(50); // T = 50ms between each query, T = 20ms for tests later

            long start = System.currentTimeMillis();
            String result = invoke(server, query);
            long turnaroundTime = System.currentTimeMillis() - start;

            System.out.println(result + " " + query.rawLine + " (turnaround time: " + turnaroundTime + " ms)");
        }
    }

    // invoke() is the dispatch method
    private static String invoke(ServerInterface server, Query query) throws Exception {
        switch (query.methodName) {

            case "getPopulationofCountry" -> {
                String countryName = String.join(" ", query.argTokens);

                return String.valueOf(server.getPopulationofCountry(countryName));
            }

            case "getNumberofCities" -> {
                String comp = query.argTokens[query.argTokens.length - 1];
                long threshold = Long.parseLong(query.argTokens[query.argTokens.length - 2]);
                String countryName = String.join(" ",
                        Arrays.copyOfRange(query.argTokens, 0, query.argTokens.length - 2));

                return String.valueOf(server.getNumberofCities(countryName, threshold, comp));
            }

            case "getNumberofCountries" -> {
                int cityCount = Integer.parseInt(query.argTokens[0]);
                long threshold = Long.parseLong(query.argTokens[1]);
                String comp = query.argTokens[2];

                return String.valueOf(server.getNumberofCountries(cityCount, threshold, comp));
            }

            case "getNumberofCountriesMM" -> {
                int cityCount = Integer.parseInt(query.argTokens[0]);
                long minPop = Long.parseLong(query.argTokens[1]);
                long maxPop = Long.parseLong(query.argTokens[2]);

                return String.valueOf(server.getNumberofCountriesMM(cityCount, minPop, maxPop));
            }

            default -> throw new IllegalArgumentException("Unknown method: " + query.methodName);
        }
    }
}
