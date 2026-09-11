package com.ass1.server;
import com.ass1.common.City;
import java.util.List;

/**
 * TODO: Fjern denne midlertidige test-klassen før levering
 * Denne klassen er for å teste datasett parsingen.
 * Bra å få alt til å fungere før RMI
 * Sammenligner med assignment: Norge = 3 162 856 and Sverige = 9 362 428
 */

public class DatasetLoaderTest {
    public static void main(String[] args) throws Exception {
        List<City> cities = DatasetLoader.load("data/exercise_1_dataset.csv");

        System.out.println("Antall byer lastet: " + cities.size());
        // Forventet: 140 574

        long norgePop = cities.stream().filter(country -> country.countryName.equalsIgnoreCase("Norway")).mapToLong(country -> country.population).sum();
        System.out.println("Norges befolkning (sum av byer): " + norgePop);
        // Forventet: 3 162 856

        long sverigePop = cities.stream().filter(country -> country.countryName.equalsIgnoreCase("Sweden")).mapToLong(country -> country.population).sum();
        System.out.println("Norges befolkning (sum av byer): " + sverigePop);
        // Forventet: 9 362 428

        // tilfeldige byer for sikkerhetsskyld
        cities.stream().limit(3).forEach(System.out::println);
    }
}



/**
 * OUTPUT:
The topLine is read and printed here: Geoname ID;Name;Country Code;Country name EN;Population;Timezone;Coordinates
Antall byer lastet: 140574
Norges befolkning (sum av byer): 3162856
Norges befolkning (sum av byer): 9362428
Fleron (Belgium), population num=15994
Fauvillers (Belgium), population num=1952
Ettelgem (Belgium), population num=1181
 */
