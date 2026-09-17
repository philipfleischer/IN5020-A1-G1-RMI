package com.ass1.client;

import java.util.List;

/**
 * TODO: Fjern denne midlertidige test-klassen før levering
 * Denne klassen er for å teste at Query.parseLine/Query.readQueries
 * faktisk leser input/exercise_1_input.txt riktig.
 * Dette gjøres før vi kobler på RMI-kallene.
 */
public class QueryParserTest {
    public static void main(String[] args) throws Exception {
        List<Query> queries = Query.readQueries("input/exercise_1_input.txt");

        System.out.println("Antall queries lest: " + queries.size());
        // Forventet: 3165 (en per linje i input-filen)

        System.out.println("\nDe 5 forste linjene:");
        queries.stream().limit(5).forEach(System.out::println);

        // Spesifik sjekk her på linje med et land med flere ord som "French Guiana"
        Query multiWordCountry = queries.get(0);
        System.out.println("\nForste linje (2 argTokens - \"French\" og \"Guiana\"):");
        System.out.println("---> methodName = " + multiWordCountry.methodName);
        System.out.println("---> argTokens  = " + java.util.Arrays.toString(multiWordCountry.argTokens));
        System.out.println("---> zone       = " + multiWordCountry.zone);

        // Finn og skriv ut ett eksempel av hver av de 4 metodene, sa vi kan sjekke argTokens
        // for alle varianter (getNumberofCities har f.eks. "countryName threshold comp").
        System.out.println("\nEtt eksempel av hver metode:\n");
        for (String method : List.of("getPopulationofCountry", "getNumberofCities", "getNumberofCountries", "getNumberofCountriesMM")) {
            queries.stream()
                    .filter(q -> q.methodName.equals(method))
                    .findFirst()
                    .ifPresent(System.out::println);
        }
    }
}
