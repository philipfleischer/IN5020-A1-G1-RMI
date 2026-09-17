# Part1_Explained.md

> **Dette dokumentet er skrevet av en AI (Claude)** som har fulgt arbeidet med Client/Server-delen
> av oppgaven steg for steg. Det går gjennom hver fil som er laget så langt, forklarer hva hver
> funksjon gjør og hvorfor, og beskriver hvordan alt henger sammen. Formålet er at dere skal kunne
> bruke dette til å øve til presentasjonen — les det, still spørsmål til hverandre, og sørg for at
> dere kan forklare hver del med egne ord uten å måtte se på koden.

## 1. Hva er gjort så langt?

Vi har implementert **steg 1 fra "Recommended workload split"**: Client + en enkel (naiv) Server,
uten proxy, uten kø-system, uten cache, uten Docker. Målet med denne branchen
(`feature/client-server-naive`) var å få hele kjeden til å fungere end-to-end:

```
Client leser input-fil -> gjør RMI-kall til Server -> Server regner ut svar -> 
Client tar tid på kallet -> Client skriver resultatet til naive_server.txt
```

"Naiv" betyr her at Serveren ikke har noen cache og ikke har noe kø-system ennå — den bare løper
gjennom hele datasettet (140 574 byer) på nytt for hver eneste forespørsel. Det er akkurat det
oppgaveteksten ber om som første steg ("Naive Implementation").

## 2. Arkitekturoversikt

Java RMI (Remote Method Invocation) lar et program (`Client`) kalle metoder på et objekt som
faktisk kjører i en helt annen Java-prosess (`Server`) — nesten som om objektet var lokalt. Slik
henger delene sammen:

1. **`Server`** implementerer `ServerInterface` — dette er "kontrakten" som sier hvilke metoder som
   kan kalles eksternt.
2. **`Main`** (server-siden) starter en RMI-registry (en slags "telefonkatalog" for RMI-objekter) på
   port 1099, og registrerer `Server`-objektet under navnet `"StatisticsServer"`.
3. **`Client`** slår opp `"StatisticsServer"` i registeret, får en "stub" (en stedfortreder-versjon
   av Server-objektet), og kaller metoder på den akkurat som om det var et lokalt objekt — RMI tar
   seg av å sende kallet over nettverket i bakgrunnen.

```
   ┌──────────┐        1. lookup("StatisticsServer")        ┌──────────────┐
   │  Client  │ ────────────────────────────────────────▶  │ RMI-registry │
   │          │ ◀────────────────────────────────────────  │  (port 1099) │
   │          │        2. returnerer "stub" til Server      └──────────────┘
   │          │
   │          │  3. server.getPopulationofCountry("Norway")
   │          │ ─────────────────────────────────────────▶  ┌──────────────┐
   │          │ ◀─────────────────────────────────────────  │    Server    │
   └──────────┘  4. returnerer resultatet (f.eks 3162856)    └──────────────┘
```

## 3. Filene, én etter én

### `src/main/java/com/ass1/common/City.java`

En enkel dataklasse (POJO) som representerer **én rad** i datasettet — én by. Har felter for
`geonameId`, `name`, `countryCode`, `countryName`, `population`, `timezone`, `latitude`,
`longitude`. Ligger i `common`-pakken fordi både server- og klientkoden potensielt trenger å vite
hva en "by" er (i praksis er det bare serveren som bruker den akkurat nå).

- Implementerer `Serializable` — **viktig for RMI**: alt som sendes over nettverket via RMI må
  kunne serialiseres (gjøres om til bytes og tilbake). `City` sendes riktignok ikke over RMI ennå
  (bare tallresultater gjør det), men det er lurt å ha det klart for senere.
- `toString()` er overstyrt til å vise et lesbart format (`"Fleron (Belgium), population
  num=15994"`), brukt i test-utskrifter.

### `src/main/java/com/ass1/server/DatasetLoader.java`

**Funksjon:** `List<City> load(String filePath)`
Leser hele `exercise_1_dataset.csv` (140 574 linjer) inn i minnet, én gang, og returnerer en liste
av `City`-objekter.

- Bruker `BufferedReader` fordi filen er stor — leser linje for linje i stedet for å laste alt på
  én gang som rå tekst.
- Hopper over den første linjen (kolonneoverskriftene).
- Hopper over tomme linjer og linjer som ikke lar seg parse (fanger exception per linje, så én
  ødelagt linje ikke krasjer hele innlastingen), og logger hvor mange som ble hoppet over.

**Funksjon:** `private City parseCSVLine(String line)`
Splitter én CSV-linje på `;` og bygger et `City`-objekt av feltene. Bruker `split(";", -1)` (med
`-1`) for at et tomt felt på slutten (f.eks. manglende timezone) ikke skal føre til at feltet
forsvinner fra arrayet. Kolonnene splittes videre — koordinatene er kommaseparert internt
(`"50.61516,5.68062"`), så de splittes én gang til.

### `src/main/java/com/ass1/server/ServerInterface.java`

Dette er **RMI-kontrakten** — et `interface` som arver fra `java.rmi.Remote`. Alle metoder som skal
kunne kalles eksternt over RMI må stå her, og må kaste `RemoteException` (fordi et nettverkskall
alltid kan feile, i motsetning til et vanlig lokalt metodekall).

De fire metodene er hentet direkte fra oppgaveteksten:

| Metode | Hva den gjør |
|---|---|
| `getPopulationofCountry(countryName)` | Summerer populasjonen til alle byer i et land |
| `getNumberofCities(countryName, threshold, comp)` | Teller byer i et land med befolkning ≥/≤ threshold |
| `getNumberofCountries(cityCount, threshold, comp)` | Teller land som har minst `cityCount` byer med befolkning ≥/≤ threshold |
| `getNumberofCountriesMM(cityCount, minPop, maxPop)` | Teller land som har minst `cityCount` byer med befolkning **mellom** minPop og maxPop |

`comp` er alltid strengen `"min"` (minst/at least) eller `"max"` (høyst/at most).

### `src/main/java/com/ass1/server/Server.java`

Selve implementasjonen av `ServerInterface`. Arver fra `UnicastRemoteObject` — det er dette som
gjør objektet "RMI-klart" (lar det eksportere seg selv som en fjernstyrt tjeneste når det
konstrueres).

- **Konstruktør** `Server(List<City> cities)`: tar imot den ferdig innlastede bylisten (fra
  `DatasetLoader`) og lagrer den. Kaster `RemoteException` fordi `super()`-kallet til
  `UnicastRemoteObject` kan feile (f.eks. hvis nettverksoppsettet er ugyldig).
- **`simulateNetworkLatency()`**: en privat hjelpemetode som sover i 80ms (`Thread.sleep(80)`).
  Dette er *ikke* en tilfeldig treg kode — oppgaveteksten (avsnitt 2.3) **krever** at hver
  fjernkalt metode pauser i 80ms før den "legges i kø", for å simulere ekte nettverkslatens siden
  vi kjører alt lokalt på én maskin.
- **`isMin(comp)`**: liten hjelpemetode som returnerer `true` hvis `comp` er `"min"`, ellers
  `false`. Brukes til å velge mellom `>=` og `<=` i filtrene under.
- **De fire metodene** bruker alle Java Streams (`cities.stream().filter(...)...`) for å gå
  gjennom hele bylisten og regne ut svaret — dette er selve den "naive" delen: ingen indeksering,
  ingen cache, bare et rått gjennomløp hver gang.
  - `getPopulationofCountry`: filtrer på land, summer populasjon (`mapToLong().sum()`).
  - `getNumberofCities`: filtrer på land OG på threshold/comp, tell antall.
  - `getNumberofCountries` og `getNumberofCountriesMM`: dette er de vanskeligste. De må
    **gruppere** byer per land (`Collectors.groupingBy(...)`) og telle hvor mange land som endte
    opp med nok kvalifiserende byer. Se kodekommentarene i filen for stegvis forklaring.

**Viktig detalj:** konstruktøren er `protected`, ikke `public`. Det betyr at kun kode i samme
pakke (`com.ass1.server`) kan lage en `Server`-instans — det er derfor `Main.java` **må** ligge i
`com.ass1.server`-pakken (dette skapte faktisk en reell bug tidlig i prosessen, se avsnitt 6).

### `src/main/java/com/ass1/server/Main.java`

Dette er programmet som faktisk **starter** en server-instans. Kjøres med:
```
java -cp target/classes com.ass1.server.Main
```
Gjør tre ting, i rekkefølge:
1. Laster datasettet med `DatasetLoader.load(...)`.
2. Lager en `Server`-instans med den innlastede bylisten.
3. Starter en RMI-registry på port 1099 (`LocateRegistry.createRegistry(port)`) og registrerer
   Server-objektet under navnet `"StatisticsServer"` (`registry.rebind(...)`). Dette navnet er
   nøkkelen som `Client.java` senere bruker for å finne igjen akkurat dette objektet.

### `src/main/java/com/ass1/client/Query.java`

En liten dataklasse som representerer **én parset linje** fra input-filen
(`input/exercise_1_input.txt`). Hver linje ser sånn ut:
```
getNumberofCities Norway 568422 min Zone:4
```
og deles opp i:
- `rawLine` — hele den originale linjen (trengs igjen senere til output-formatet).
- `methodName` — f.eks. `"getNumberofCities"`.
- `argTokens` — alt mellom metodenavnet og `Zone:#`, **usplittet** (`["Norway", "568422", "min"]`
  i eksempelet over). Grunnen til at det ikke splittes ferdig her: landnavn kan bestå av flere
  ord (f.eks. `"French Guiana"`, `"Equatorial Guinea"`), så antall tokens varierer avhengig av
  hvilken metode det gjelder — den riktige tolkningen skjer først i `Client.invoke()`, som vet
  hvilken metode som kalles.
- `zone` — tallet fra `Zone:#`-taggen (brukes senere når Proxy-serveren kommer inn i bildet).

**Funksjon:** `static Query parseLine(String line)`
Splitter linjen på mellomrom, plukker ut metodenavn (første token), zone-nummer (siste token,
splittet på `:`), og lar alt i midten være `argTokens`.

**Funksjon:** `static List<Query> readQueries(String filePath)`
Leser hele input-filen linje for linje (samme `BufferedReader`-mønster som `DatasetLoader`) og
returnerer en liste med `Query`-objekter, ett per linje.

### `src/main/java/com/ass1/client/Client.java`

Hovedprogrammet på klientsiden. Kjøres med:
```
java -cp target/classes com.ass1.client.Client
```
`main()`-metoden gjør, i rekkefølge:

1. **Kobler til serveren**: slår opp `"StatisticsServer"` i RMI-registeret på port 1099 og får en
   `ServerInterface`-referanse (`server`) å kalle metoder på.
2. **Leser input-filen** med `Query.readQueries(...)` — 3166 spørringer.
3. **Løkke over alle spørringene**:
   - Venter `T = 50ms` (`Thread.sleep(50)`) mellom hver spørring, som oppgaveteksten krever.
   - Tar tid **før og etter** selve RMI-kallet (`invoke(server, query)`) — differansen er
     **turnaround time**.
   - Bygger en output-linje i formatet oppgaven spesifiserer, og legger den i `outputLines`.
   - Lagrer turnaround-tiden i et map (`turnaroundByMethod`), gruppert per metodenavn — dette
     trengs for oppsummeringsstatistikken på slutten.
4. **Skriver til `naive_server.txt`**: først alle enkeltresultatene, deretter fire
   oppsummeringslinjer (én per metodetype) med gjennomsnittlig/minste/høyeste turnaround-tid,
   slik oppgaveteksten krever (side 5, punkt 4).

**Funksjon:** `private static String invoke(ServerInterface server, Query query)`
Dette er "dispatcheren" — den ser på `query.methodName` og avgjør hvilken metode på `server` som
skal kalles, og hvordan `argTokens` skal tolkes for akkurat den metoden:
- `getPopulationofCountry`: **alle** tokens slås sammen til landnavnet (`String.join(" ",
  argTokens)`), siden hele argumentlisten er landnavnet.
- `getNumberofCities`: de to **siste** tokens er alltid `threshold` og `comp`, alt før det er
  landnavnet.
- `getNumberofCountries` / `getNumberofCountriesMM`: faste posisjoner (ingen landnavn involvert),
  så `argTokens[0]`, `[1]`, `[2]` leses direkte.

**Funksjon:** `private static double average(List<Long> values)`
Enkel gjennomsnittsberegning med Java Streams, brukt til oppsummeringslinjene.

### `src/test/java/com/ass1/server/DatasetLoaderTest.java` og `src/test/java/com/ass1/client/QueryParserTest.java`

Dette er **ikke** JUnit-tester (prosjektet har ingen testrammeverk satt opp i `pom.xml` ennå) —
det er enkle klasser med en `main()`-metode som kjøres manuelt for å verifisere at ting fungerer,
uavhengig av RMI:
- `DatasetLoaderTest`: laster datasettet og kaller `Server`-metodene direkte (uten RMI) og
  sammenligner mot fasitverdiene fra oppgaveteksten (Norge = 3 162 856, osv.).
- `QueryParserTest`: leser hele input-filen med `Query.readQueries(...)` og skriver ut eksempler,
  for å sjekke at parsingen (spesielt flerords-landnavn) fungerer riktig.

De ligger i `src/test/java/` (Maven sin standard for testkode) i stedet for `src/main/java/`, slik
at de ikke havner i den ferdige `.jar`-fila som skal leveres.

## 4. Hele kjeden i praksis (slik kjører dere det)

```bash
# 1) Bygg
mvn compile

# 2) Start serveren (ett terminalvindu)
java -cp target/classes com.ass1.server.Main

# 3) Start klienten (et annet terminalvindu) - dette tar ca 6-7 minutter
#    for hele input-filen (3166 spørringer x 50ms ventetid + 80ms serverlatens hver)
java -cp target/classes com.ass1.client.Client
```
Resultatet havner i `naive_server.txt` i prosjektroten.

## 5. Hva outputformatet faktisk betyr

Eksempellinje:
```
9362428 getPopulationofCountry Sweden Zone:1 (turnaround time: 95 ms, execution time: 0 ms, waiting time: 0 ms, processed by Server 1)
```
- `9362428` — selve svaret fra serveren.
- `getPopulationofCountry Sweden Zone:1` — den originale spørringen, hentet fra `query.rawLine`.
- `turnaround time` — total tid fra klienten sendte kallet til svaret kom tilbake (målt i
  `Client.java` med `System.currentTimeMillis()` rundt selve kallet).
- `execution time` / `waiting time` — **placeholder-verdier (0) foreløpig**. Disse skal egentlig
  komme fra serveren og reflektere hvor lenge forespørselen lå i kø og hvor lenge den faktisk
  tok å behandle — men det krever kø-systemet som ikke er bygget ennå (det er en annen del av
  oppgaven, se avsnitt 7).
- `processed by Server 1` — også placeholder, siden vi bare har én server og ingen proxy ennå.

## 6. Bugs vi møtte på underveis (nyttig å kunne forklare på presentasjonen!)

Dette er gode eksempler på "vanlige feil" som er verdt å kunne forklare hvis dere blir spurt om
hvordan utviklingen gikk:

1. **Pakke/filplassering-mismatch**: `Main.java` lå i `com/ass1/` men hadde
   `package com.ass1.server;` — Maven godtok det, men `Server`-konstruktøren er `protected`
   (kun tilgjengelig fra samme pakke), så `new Server(cities)` feilet med en tilgangsfeil helt til
   `Main.java` ble flyttet til riktig mappe (`com/ass1/server/Main.java`) for å faktisk stemme med
   pakke-deklarasjonen.
2. **`indexOf(';')` i stedet for `indexOf(':')`** i `Query.parseLine()`: `"Zone:4"` inneholder
   ikke noe semikolon, så dette ville kastet `NumberFormatException` på hver eneste linje i
   input-filen. Fikset til `':'`.
3. **`String.join("", argTokens)`** i stedet for `String.join(" ", argTokens)`: manglet
   mellomrom i separatoren, så `["French", "Guiana"]` ble til `"FrenchGuiana"` i stedet for
   `"French Guiana"` — som selvfølgelig ikke matchet noe land i datasettet, og ga `0` i svar for
   **alle** land med flerords-navn (mens ett-ords land som `"Canada"` fungerte helt fint, siden
   separatoren ikke spiller noen rolle når det bare er ett element).
4. **Manglende variabeldeklarasjon**: `outputLines` ble brukt i koden (`.add(...)`) uten å noen
   gang være deklarert med `List<String> outputLines = new ArrayList<>();` — ren
   kompileringsfeil, fanget opp av `mvn compile`.
5. **Manglende `boolean atLeast = isMin(comp);`** i `getNumberofCountries()` — variabelen ble
   brukt lenger ned i metoden uten å være satt, også en kompileringsfeil.

Poenget med å liste disse opp: det er helt normalt å møte denne typen feil, og det er ofte lurt å
kunne forklare *hvorfor* de oppsto og hvordan dere fant dem (stort sett: `mvn compile` og å faktisk
kjøre koden mot kjente fasitverdier fra oppgaveteksten).

## 7. Hva er IKKE gjort ennå (viktig kontekst for presentasjonen)

Dette er delt inn per gruppemedlem, jf. README:

- **Proxy/load-balancer** (resten av "Client and Proxy server"-delen): klienten snakker i dag
  direkte med én hardkodet server på port 1099 og ignorerer `Zone:#`-verdien helt. Proxyen skal
  etter planen ta imot forespørsler fra klienten, se på `Zone:#`, og returnere adressen til riktig
  server (evt. en nabosone-server hvis den lokale er overbelastet).
- **Kø-system og tråder** (Anwars del): `Server` skal ha en FIFO-kø per sone og én dedikert
  execution-tråd, og faktisk returnere ekte `execution time`/`waiting time` — i dag er dette
  hardkodet til `0`.
- **Cache** (Håkons del): både server-side (opptil 150 resultater) og client-side (opptil 45
  resultater) cache, med FIFO- eller LRU-utkasting.
- **Docker** (Mateus' del): containerisering av serveren.

---
*Sist oppdatert av AI basert på koden i `feature/client-server-naive`-branchen. Oppdater gjerne
dette dokumentet selv etter hvert som resten av oppgaven bygges ut.*
