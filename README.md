# IN5020-A1-G1-RMI

IN5020: Assignment 1 and Group 1

**The Java-RMI International Statistics Service**.

A distributed statistics service over a dataset of approximately 140,000 cities: a client sends queries through a load-balancing proxy to one of several zone servers, with optional server-side and client-side caching. Built with plain Java RMI (no external libraries), Maven and Docker.

See `Assignment1_Group1_Report.pdf` for the full design, build, implementation, user guide, screenshots, workload split and test results.

## Build & run

```bash
mvn clean package

# 1) Proxy
java -cp target/solution.jar com.ass1.proxy.Main

# 2)
# One or more servers args: port datasetPath cacheMode
# CacheModes are: "none", "FIFO" or "OLDEST".
# The Proxy assigns each server's zone number automatically (ascending) as it registers.
java -cp target/solution.jar com.ass1.server.Server 2000 data/exercise_1_dataset.csv none

# 3) Client args: mode evictionMethod T(ms) inputFile
#    modes are: "naive", "server_cache" or "client_cache"
java -cp target/solution.jar com.ass1.client.Client naive FIFO 50 input/exercise_1_input.txt
```

Toolchain used: **JDK 17**, **Maven 3.9**, **Docker**.

## Run everything automatically

```bash
chmod +x run_all.sh
./run_all.sh
# all 10 configs, starts 5 zone servers each run, clears output/ first

pip install matplotlib
python3 plot_graphs.py
# reads output/, writes graphs to output/graphs/
```

## Docker

One image runs every process, the command picks proxy, server or client. Compose starts the proxy and one server per zone (1-5) on a shared network, so containers reach each other by service name.

```bash
# build the shared image once
docker compose build
# Not using `up -d --build`, since it can race when 6 services share one image

# starts proxy and one server per zone (1-5)
docker compose up -d

# runs the client once, results land in ./out
docker compose run --rm client
docker compose down
```

`PROXY_HOST` tells servers and the client where the proxy is.
`SERVER_HOST` is the name a server registers with and puts in its RMI stub (`java.rmi.server.hostname`).
Compose sets it to the service name. Both default to `localhost`, so plain local runs are unchanged.

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

| Member | Responsibility |
|---|---|
| **Philip** | Client and Proxy server |
| **Anwar** | Processing server, including queue technique |
| **Håkon** | Cache technique |
| **Mateus** | Docker/Container |
