package com.ass1.server;
import com.ass1.common.City;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reading the whole exercise_1_dataset.csv file into memory, once, on server start.
 *
 * This is the "NAIVE" method that was mentioned in the assignment, to start and test.
 * We only run through all of it from top to bottom, without any caching on popular.
 *
 * FileFormat: Geoname ID;Name;Country Code;Country name EN;Population;Timezone;Coordinates
 */
public class DatasetLoader {
    /**
     * filePath is the path to the csv file.
     * return all the cities in the dataset as a city-obj list.
     */
    public static List<City> load(String filePath) throws IOException {
        List<City> cities = new ArrayList<>();

        /**
         * Using the try clausul in case anything breaks we get a safe outcome.
         * BufferedReader since the file has 140000+ lines.
         */
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
            // THe top line is just for readers to understand explicitly what we have, so we ignore it.
            String topLine = bufferedReader.readLine(); //the GEONAME line
            System.out.println("The topLine is read and printed here: " + topLine);

            String currentReadLine;
            int lineNumber = 0; // what we want to find
            int skippedLines = 0; // in case there are any empty lines, we can subtract from total to find real total.

            // While there are more lines to read
            while ((currentReadLine = bufferedReader.readLine()) != null) {
                lineNumber++;

                if (currentReadLine.isBlank()) {
                    skippedLines++;
                    continue;
                }

                try {
                    cities.add(parseCSVLine(currentReadLine));
                } catch (Exception e) {
                    // in case a single line is malformed, we catch and just log the deviation.
                    skippedLines++;
                    // Might add some more buffer here
                }
            }
            if (skippedLines > 0) {
                System.out.println("[NOTE] Skipped " + skippedLines + " lines, due to them being unreadable!");
            }
        }

        //Returning the cities array
        return cities;
    }

    /**
     * Takes a String from the csv file and parses it correctly into a City object to be put into the ArrayList cities.
     */
    private static City parseCSVLine(String currentParsableLine) {
        // Using the -1 here in case the Timezone is missing, so we dont discard the whole line in that case.
        String[] items = currentParsableLine.split(";", -1);

        long geonameId = Long.parseLong(items[0].trim());
        String name = items[1].trim();
        String countryCode = items[2].trim();
        String countryName = items[3].trim();
        long population;
        String populationInit = items[4].trim();
        if (populationInit.isEmpty()) {
            population = 0L;
        } else {
            population = Long.parseLong(populationInit);
        }
        String timezone = items[5].trim();

        String[] coordinates = items[6].trim().split(",");
        double latitude = Double.parseDouble(coordinates[0].trim());
        double longitude = Double.parseDouble(coordinates[1].trim());

        return new City(geonameId, name, countryCode, countryName, population, timezone, latitude, longitude);

    }
}
