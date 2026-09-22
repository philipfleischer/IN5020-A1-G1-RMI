# IN5020-A1-G1-RMI

Assignment 1, IN5020 Group 1 — **The Java-RMI International Statistics Service**.

This repo holds the **project skeleton** so the four of us can start our parts in parallel
without setting up the plumbing first. This is the Maven and RMI starter pack from the group
session presentation and the Add(a, b) demo.

**Nothing** from the assignment itself is implemented yet!

## Structure

```
pom.xml                          Maven project -> groupId com.ass1 / Java 17
src/main/java/com/ass1/
  Main.java                      From the tutorial project
  server/ServerInterface.java    RMI remote interface
  server/Server.java             RMI server + rmiregistry
  client/Client.java             RMI client
src/main/resources/              (empty)
src/test/java/                   (empty)

data/exercise_1_dataset.csv      Course dataset
input/exercise_1_input.txt       Course client input file
docs/assignment/                 Original hand-out: task PDF/DOCX, presentation...
```

## Build & run

```bash
# 1)
mvn compile

# 2) start the RMI registry, then the server and client
cd target/classes && rmiregistry &
java -cp target/classes com.ass1.server.Server &
java -cp target/classes com.ass1.client.Client
```

Toolchain used: **JDK 17**, **Maven 3.9**, **Docker**.

## Docker (assignment point 4)

One image runs every process; the command picks proxy, server or client. Compose starts the
proxy and one server per zone (1-5) on a shared network, so containers reach each other by
service name.

```bash
docker compose up -d --build
docker compose run --rm client        # results and queue logs land in ./out
docker compose down
```

`PROXY_HOST` tells servers and the client where the proxy is. `SERVER_HOST` is the name a
server registers with and puts in its RMI stub (`java.rmi.server.hostname`); compose sets it
to the service name. Both default to `localhost`, so plain local runs are unchanged.

Extra zone server against a running stack, and image export for delivery:

```bash
docker run -d --network in5020-a1-g1-rmi_default -e PROXY_HOST=proxy -e SERVER_HOST=server6 \
  in5020-a1-g1 com.ass1.server.Server 2006 6 /app/data/exercise_1_dataset.csv
docker save in5020-a1-g1 | gzip > in5020-a1-g1-image.tar.gz
```

## Members (TODO: Write names):

Philip Elias Fleischer: philipef@uio.no

Anwar Ahmed Hersi : anwarahe@uio.no

Håkon Gulliksrud: haakgull@uio.no

: matande@uio.no


## Workload split

Member -> Responsibility -> Where the code goes

**Philip** -> Client and Proxy server -> com.ass1.client, ...

**Anwar** -> Processing server, including queue technique -> com.ass1.server

**Håkon** -> Cache technique -> ?

**Mateus** -> Docker/Container -> root


## Deadline

**Deadline:** 23:59, 24 September 2026 (Devilry).
