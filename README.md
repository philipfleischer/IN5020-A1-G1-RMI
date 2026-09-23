# IN5020-A1-G1-RMI

Assignment 1, IN5020 Group 1 - **The Java-RMI International Statistics Service**.

A distributed statistics service over a dataset of ~140,000 cities: a client sends
queries through a load-balancing proxy to one of several zone servers, with optional
server-side and client-side caching. Built with plain Java RMI (no external libraries),
Maven and Docker.

See `Assignment1_Report.docx` for the full design/implementation write-up, user guide,
screenshots, workload split and test results - this README is just a quick-start.

## Structure

```
pom.xml                          Maven project -> groupId com.ass1 / Java 17
src/main/java/com/ass1/
  client/                        Client + input-file query parsing (Philip)
  proxy/                         Load-balancing proxy (Philip)
  server/                        Zone server, request queue, dataset lookups (Anwar)
  common/                        Shared cache used by both server and client (Håkon)
src/test/java/                   Unit tests

data/exercise_1_dataset.csv      Course dataset
input/exercise_1_input.txt       Course client input file
docs/assignment/                 Original hand-out: task PDF/DOCX, presentation...
run_all.sh                       Runs all 10 required test configurations
plot_graphs.py                   Generates the required graphs from output/
output/                          Result files, queue logs and graphs from the last run
```

## Build & run

```bash
mvn clean package

# 1) Proxy
java -cp target/solution.jar com.ass1.proxy.Main

# 2) One or more servers - args: port  datasetPath  cacheMode
#    cacheMode is "none", "FIFO" or "OLDEST". The Proxy assigns each server's
#    zone number automatically (ascending) as it registers.
java -cp target/solution.jar com.ass1.server.Server 2000 data/exercise_1_dataset.csv none

# 3) Client - args: mode  evictionMethod  T(ms)  inputFile
#    mode is "naive", "server_cache" or "client_cache"
java -cp target/solution.jar com.ass1.client.Client naive FIFO 50 input/exercise_1_input.txt
```

Toolchain used: **JDK 17**, **Maven 3.9**, **Docker**.

## Run everything automatically

```bash
chmod +x run_all.sh
./run_all.sh              # all 10 configs, starts 5 zone servers each run, clears output/ first

pip install matplotlib
python3 plot_graphs.py    # reads output/, writes graphs to output/graphs/
```

## Docker (assignment point 4)

One image runs every process; the command picks proxy, server or client. Compose starts the
proxy and one server per zone (1-5) on a shared network, so containers reach each other by
service name.

```bash
docker compose build                  # build the shared image once - always do this first,
                                       # `up -d --build` can race when 6 services share one image
docker compose up -d                  # starts proxy and one server per zone (1-5)
docker compose run --rm client        # runs the client once, results land in ./out
docker compose down
```

`PROXY_HOST` tells servers and the client where the proxy is. `SERVER_HOST` is the name a
server registers with and puts in its RMI stub (`java.rmi.server.hostname`); compose sets it
to the service name. Both default to `localhost`, so plain local runs are unchanged.

To export the built image for delivery:

```bash
docker save in5020-a1-g1 | gzip > in5020-a1-g1-image.tar.gz
```

## Members

Philip Elias Fleischer: philipef@uio.no

Anwar Ahmed Hersi: anwarahe@uio.no

Håkon Gulliksrud: haakgull@uio.no

Mateus Boergeson: matande@uio.no

## Workload split

| Member | Responsibility | Where the code goes |
|---|---|---|
| **Philip** | Client and Proxy server | `com.ass1.client`, `com.ass1.proxy` |
| **Anwar** | Processing server, including queue technique | `com.ass1.server` |
| **Håkon** | Cache technique | `com.ass1.common` |
| **Mateus** | Docker/Container | `Dockerfile`, `docker-compose.yml` |

## Deadline

**Deadline:** 23:59, 24 September 2026 (Devilry).
