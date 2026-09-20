package com.ass1.server;

import com.ass1.proxy.ProxyInterface;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * One zone server. Answers the 4 statistics queries from the assignment.
 * Request flow: an RMI call runs on its own thread (given by Java RMI),
 * sleeps 80ms, adds a Task to the queue, then waits for an answer. One
 * background thread (started by us) processes the queue in FIFO order
 * and completes each task's answer. RMI already gives us threads for
 * "receiving" requests - we only need to build the one execution thread.
 */
public class Server extends UnicastRemoteObject implements ServerInterface {

    private static final int SIMULATED_NETWORK_DELAY_MS = 80;

    private final int zone;
    private final String datasetPath;

    // 2.4: each server has its own zone-specific waiting list.
    // BlockingQueue is thread-safe: many RMI threads can put() while our
    // one background thread take()s, at the same time, safely.
    private final BlockingQueue<Task> waitingList = new LinkedBlockingQueue<>();

    // Logs "timestamp,queueSize" on every queue change - used for the required graphs.
    private final PrintWriter queueLog;

    public Server(int zone, String datasetPath) throws RemoteException, IOException {
        super(); // exports this object over RMI
        this.zone = zone;
        this.datasetPath = datasetPath;
        this.queueLog = new PrintWriter(new FileWriter("server_zone_" + zone + "_queue_log.txt", true));

        // 2.4.c: two thread groups - one executes tasks, others accept new
        // tasks. The "accept" side is free (Java RMI's own thread pool);
        // this starts the one execution thread we do need to build.
        Thread executionThread = new Thread(this::processQueueForever, "execution-thread-zone-" + zone);
        executionThread.setDaemon(true);
        executionThread.start();
    }

    // 2.4.a: FIFO execution loop - always takes the oldest task first.
    private void processQueueForever() {
        while (true) {
            try {
                Task task = waitingList.take(); // blocks until a task exists, FIFO order
                logQueueLength();

                long waitingTimeMs = System.currentTimeMillis() - task.getArrivalTimeMs();

                long execStart = System.currentTimeMillis();
                Object rawResult;
                try {
                    rawResult = task.getWork().call();
                } catch (Exception e) {
                    // One bad task must never stop this loop.
                    task.getFuture().completeExceptionally(e);
                    continue;
                }
                long executionTimeMs = System.currentTimeMillis() - execStart;

                task.getFuture().complete(new QueryResult(rawResult, waitingTimeMs, executionTimeMs, zone));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return; // only happens on shutdown
            }
        }
    }

    // synchronized: many threads call this at once, so writes must not interleave.
    private synchronized void logQueueLength() {
        queueLog.println(System.currentTimeMillis() + "," + waitingList.size());
        queueLog.flush();
    }

    // 2.3: pause 80ms before adding to the queue, to simulate network delay.
    // Shared by all four query methods below: (1) simulate delay,
    // (2) enqueue, (3) wait for and return the answer.
    private QueryResult submitAndWait(Callable<Object> work) throws RemoteException {
        try {
            Thread.sleep(SIMULATED_NETWORK_DELAY_MS); // must happen BEFORE enqueueing

            Task task = new Task(work);

            // 2.4.b: put() is safe even while the execution thread is busy -
            // BlockingQueue handles that safety automatically.
            waitingList.put(task);
            logQueueLength();

            return task.getFuture().get(); // waits for this specific task's answer
        } catch (Exception e) {
            throw new RemoteException("Task failed", e);
        }
    }

    // 2.1 (a-d): RMI wrapper around the four required query methods.
    // The real algorithms live in Dataset.java - each method here just
    // loads the data (naive mode: re-parses the CSV every call) and calls
    // the matching Dataset method, wrapped by submitAndWait() above.

    // 2.1.a
    @Override
    public QueryResult getPopulationofCountry(String countryName) throws RemoteException {
        return submitAndWait(() -> {
            List<CityRecord> cities = Dataset.parse(datasetPath);
            return Dataset.getPopulationofCountry(cities, countryName);
        });
    }

    // 2.1.b
    @Override
    public QueryResult getNumberofCities(String countryName, int threshold, String comp) throws RemoteException {
        return submitAndWait(() -> {
            List<CityRecord> cities = Dataset.parse(datasetPath);
            return Dataset.getNumberofCities(cities, countryName, threshold, comp);
        });
    }

    // 2.1.c
    @Override
    public QueryResult getNumberofCountries(int cityCount, int threshold, String comp) throws RemoteException {
        return submitAndWait(() -> {
            List<CityRecord> cities = Dataset.parse(datasetPath);
            return Dataset.getNumberofCountries(cities, cityCount, threshold, comp);
        });
    }

    // 2.1.d
    @Override
    public QueryResult getNumberofCountriesMM(int cityCount, int minPopulation, int maxPopulation) throws RemoteException {
        return submitAndWait(() -> {
            List<CityRecord> cities = Dataset.parse(datasetPath);
            return Dataset.getNumberofCountriesMM(cities, cityCount, minPopulation, maxPopulation);
        });
    }

    /** Used by the proxy to check this server's current load. */
    @Override
    public int getWaitingListSize() throws RemoteException {
        return waitingList.size();
    }

    /** Used by the proxy/client to know which zone answered. */
    @Override
    public int getZone() throws RemoteException {
        return zone;
    }

    // 2.2.a: port/zone come from args[] instead of being hardcoded, so
    // several Server instances can run at once, each with its own port
    // and unique registry name.
    public static void main(String[] args) {
        try {
            int port = args.length > 0 ? Integer.parseInt(args[0]) : 2000;
            int zone = args.length > 1 ? Integer.parseInt(args[1]) : 1;
            String datasetPath = args.length > 2 ? args[2] : "data/exercise_1_dataset.csv";

            Server server = new Server(zone, datasetPath);

            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind("Server-Zone-" + zone, server);

            System.out.println("Server for zone " + zone + " running on port " + port + "...");
            System.out.println("Bound as 'Server-Zone-" + zone + "' in the RMI registry.");

            // Tell the proxy we exist, so it can start sending clients our way. The proxy's
            // own registry always lives on a fixed port (1100), no matter which port/zone
            // this particular server instance is using.
            Registry proxyRegistry = LocateRegistry.getRegistry("localhost", 1100);
            ProxyInterface proxy = (ProxyInterface) proxyRegistry.lookup("ProxyService");

            boolean accepted = proxy.registerServer("localhost", port, zone);
            if (!accepted) {
                // Either the zone number is out of range, or another server already grabbed
                // it first. We keep running anyway (still reachable directly), just flag it.
                System.out.println("[SERVER] WARNING: proxy rejected zone " + zone + " - already taken or out of range?");
            } else {
                System.out.println("[SERVER] Registered with proxy as zone " + zone);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}