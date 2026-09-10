# IN5020-A1-G1-RMI

Assignment 1, IN5020 Group 1 — **The Java-RMI International Statistics Service**.

This repo holds the **project skeleton** so the four of us can start our parts in parallel
without setting up the plumbing first. It is exactly the Maven + RMI starter from the group
session presentation (`docs/assignment/Java RMI.pptx`, slides 5–10) — the `Add(a, b)` demo —
nothing from the assignment itself is implemented yet.

## Structure

```
pom.xml                          Maven project — groupId com.ass1 / artifactId solution / Java 17
src/main/java/com/ass1/
  Main.java                      Hello-world entry point (from the tutorial project)
  server/ServerInterface.java    RMI remote interface        (presentation slide 6)
  server/Server.java             RMI server + rmiregistry    (presentation slide 7)
  client/Client.java             RMI client                  (presentation slide 8)
src/main/resources/              (empty — Maven layout)
src/test/java/                   (empty — put JUnit tests here)

data/exercise_1_dataset.csv      Course dataset, 140 574 cities  (semicolon-separated)
input/exercise_1_input.txt       Course client input file, 3 165 queries
docs/assignment/                 Original hand-out: task PDF/DOCX, presentation, zone figure,
                                 and the untouched tutorial starter (Ass1Tutorial.zip)
```

## Build & run (verified working — prints `30`)

```bash
mvn compile

# start the RMI registry from the compiled classes, then the server and client
cd target/classes && rmiregistry &
java -cp target/classes com.ass1.server.Server &
java -cp target/classes com.ass1.client.Client        # -> 30

# optional jar:  mvn package   (add the shade/assembly plugin first if you want a fat jar)
```

Toolchain used: **JDK 17**, **Maven 3.9**, **Docker** (Docker Desktop — start the app before
building images).

## Workload split (assignment, "Recommendation for splitting the workload")

| Member | Responsibility | Where the code goes |
|--------|----------------|---------------------|
| 1 | Client + Proxy (load-balancing) server | `com.ass1.client`, new `com.ass1.proxy` |
| 2 | Processing server + queue technique | `com.ass1.server` |
| 3 | Cache technique (server-side + client-side) | new `com.ass1.server.cache`, `com.ass1.client.cache` |
| 4 | Docker / Container | new `Dockerfile`, `docker-compose.yml` (repo root) |

Agree on the shared RMI interfaces and the query/result data classes together **before**
splitting up, so everyone codes against the same contract.

**Deadline:** 23:59, 24 September 2026 (Devilry).
