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

## Members (TODO: Write names):

Philip Elias Fleischer: philipef@uio.no

: anwarahe@uio.no

Håkon Gulliksrud: haakgull@uio.no

: matande@uio.no


## Workload split

Member -> Responsibility -> Where the code goes

**Name_1** -> Client and Proxy server -> com.ass1.client, ...

**Name_2** -> Processing server, including queue technique -> com.ass1.server

**Name_3** -> Cache technique -> ?

**Name_4** -> Docker/Container -> root


## Deadline

**Deadline:** 23:59, 24 September 2026 (Devilry).
