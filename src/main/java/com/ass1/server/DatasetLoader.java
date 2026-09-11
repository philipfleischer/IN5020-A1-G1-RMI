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
     * @param filePath is the path to the csv file.
     * @return all the cities in the dataset as a city-obj list.
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
                    cities.add(currentReadLine);
                } catch (Exception e) {
                    // in case a single line is malformed, we catch and just log the deviation.
                    skippedLines++;
                    // Might add some more buffer here
                }
            }
        }

        if (skippedLines > 0) {
            System.out.println("[NOTE] Skipped " + skippedLines + " lines, due to them being unreadable!");
        }
        //Returning the cities array
        return cities;
    }

}
