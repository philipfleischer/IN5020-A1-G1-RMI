package com.ass1.server;
import com.ass1.common.City;
import com.ass1.server.Server;
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

        long totalPopulation = cities.stream().filter(country -> country.countryName.equalsIgnoreCase("Norway")).mapToLong(country -> country.population).sum();
        System.out.println("Norges befolkning (sum av byer): " + totalPopulation);
        // Forventet: 3 162 856

        long sverigePop = cities.stream().filter(country -> country.countryName.equalsIgnoreCase("Sweden")).mapToLong(country -> country.population).sum();
        System.out.println("Norges befolkning (sum av byer): " + sverigePop);
        // Forventet: 9 362 428

        // tilfeldige byer for sikkerhetsskyld
        cities.stream().limit(3).forEach(System.out::println);

        //Server test
        Server server = new Server(cities);
        System.out.println(server.getPopulationofCountry("Norway")); // 3 162 856
        System.out.println(server.getNumberofCities("Norway", 100000, "min")); // 4
        System.out.println(server.getNumberofCountries(2, 5000000, "min")); // 7
        System.out.println(server.getNumberofCountriesMM(30, 100000, 800000)); // 30
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
