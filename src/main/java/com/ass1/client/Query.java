package com.ass1.client;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * One line from the input file: <method name> <args...> Zone:#
 * argTokens = raw args, split-by-method happens in Client.
**/
public class Query {
    final String rawLine; // Original line
    final String methodName; // The method in the .txt file, e.g. "getPopulationofCountry"
    final String[] argTokens; // All arguments between the method name and Zone, which can vary
    final int zone; // e.g. Zone 4

    Query(String rawLine, String methodName, String[] argTokens, int zone) {
        this.rawLine = rawLine;
        this.methodName = methodName;
        this.argTokens = argTokens;
        this.zone = zone;
    }

    // Parses one input line into a Query.
    static Query parseLine(String line) {
        String[] tokens = line.trim().split("\\s+");

        String methodName = tokens[0];

        // The last token is always "Zone:<number>".
        String zoneToken = tokens[tokens.length - 1];
        int zone = Integer.parseInt(zoneToken.substring(zoneToken.indexOf(':') + 1));

        // Everything between the method name and the Zone tag is the argument list.
        String[] argTokens = Arrays.copyOfRange(tokens, 1, tokens.length - 1);

        return new Query(line, methodName, argTokens, zone);
    }

    // Reads every non-blank line of the input file into a list of parsed Query objects.
    static List<Query> readQueries(String filePath) throws IOException {
        List<Query> queries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                queries.add(parseLine(line));
            }
        }
        return queries;
    }

    @Override
    public String toString() {
        return methodName + " " + Arrays.toString(argTokens) + " Zone:" + zone;
    }
}
