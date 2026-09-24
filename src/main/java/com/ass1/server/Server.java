package com.ass1.server;

import com.ass1.common.Cache;
import com.ass1.common.CacheKey;
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
 * Request flow: an RMI call runs on its own thread, sleeps 80ms,
 * adds a Task to the queue, then waits for an answer.
 * One background thread processes the queue in FIFO order
 * and completes each tasks answer. RMI already gives us threads for
 * "receiving" requests, so we only need to build the one execution thread.
 */
public class Server extends UnicastRemoteObject implements ServerInterface {

    private static final int SIMULATED_NETWORK_DELAY_MS = 80;

    private final int zone;
    private final String datasetPath;

    // Each server has its own zone-specific waiting list.
    // BlockingQueue is thread-safe: many RMI threads can put() while our
    // one background thread takes, at the same time, safely.
    private final BlockingQueue<Task> waitingList = new LinkedBlockingQueue<>();

    // Logs "timestamp,queueSize" on every queue change, using this for the graphs plotting.
    private final PrintWriter queueLog;

    // Server cache
    private Cache<String, Object> cache;
    private final int MAX_CACHE_ENTRIES = 150;

    // CacheMode is either "none", "FIFO" or "OLDEST". none means no server-side cache -> naive_server.txt
    // Passed from main as args[3]
    public Server(int zone, String datasetPath, String cacheMode) throws RemoteException, IOException {
        super(); // exports this object over RMI
        this.zone = zone;
        this.datasetPath = datasetPath;
        this.queueLog = new PrintWriter(new FileWriter("server_zone_" + zone + "_queue_log.txt", true));
        // Creating a cache if flag says to do so, skip past when it is null
        this.cache = cacheMode.equalsIgnoreCase("none") ? null : new Cache<>(MAX_CACHE_ENTRIES, cacheMode);

        /** Two thread groups: one executes tasks, the others accept new tasks.
         * The "accept" side is free (Java RMI's own thread pool).
         * This starts the one execution thread we do need to build.
        **/
        Thread executionThread = new Thread(this::processQueueForever, "execution-thread-zone-" + zone);
        executionThread.setDaemon(true);
        executionThread.start();
        }

        // FIFO execution loop: always takes the oldest task first.
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

        // synchronized: many threads call this at once.
        private synchronized void logQueueLength() {
        queueLog.println(System.currentTimeMillis() + "," + waitingList.size());
        queueLog.flush();
        }

        // Pause 80ms before adding to the queue, to simulate network delay.
        // Shared by all four query methods below: (1) simulate delay,
        // (2) enqueue, (3) wait for and return the answer.
        private QueryResult submitAndWait(Callable<Object> work) throws RemoteException {
        try {
            Thread.sleep(SIMULATED_NETWORK_DELAY_MS);

            Task task = new Task(work);

            // The put() call is safe even while the execution thread is busy, BlockingQueue handles that safety automatically.
            waitingList.put(task);
            logQueueLength();

            return task.getFuture().get(); // waits for this specific tasks answer
        } catch (Exception e) {
            throw new RemoteException("Task failed", e);
        }
        }

        /** RMI wrapper around the four required query methods.
         * The algorithms are in the Dataset.java file.
         * Each method here just loads the data and calls the matching
         * Dataset method, wrapped by submitAndWait().
        **/

        // RMI Function 1
        @Override
        public QueryResult getPopulationofCountry(String countryName) throws RemoteException {
        return submitAndWait(() -> {
            String key = CacheKey.makeKey("getPopulationofCountry", countryName); // make key for the map

            // cache == null -> skip
            if (cache != null) {
                Object cachedResult = cache.get(key);

                // CACHE HIT
                if (cachedResult != null) {
                    return cachedResult;
                }
            }

            List<CityRecord> cities = Dataset.parse(datasetPath);
            long result = Dataset.getPopulationofCountry(cities, countryName);

            if (cache != null) {
                cache.put(key, result);
            }

            return result;
        });
        }

        // RMI Function 2
        @Override
        public QueryResult getNumberofCities(String countryName, int threshold, String comp) throws RemoteException {
        return submitAndWait(() -> {
            String key = CacheKey.makeKey("getNumberofCities", countryName, threshold, comp); // make key for the map

            // cache == null -> skip
            if (cache != null) {
                Object cachedResult = cache.get(key);

                // CACHE HIT
                if (cachedResult != null) {
                    return cachedResult;
                }
            }

            List<CityRecord> cities = Dataset.parse(datasetPath);
            int result = Dataset.getNumberofCities(cities, countryName, threshold, comp);

            if (cache != null) {
                cache.put(key, result);
            }

            return result;
        });
        }

        // RMI Function 3
        @Override
        public QueryResult getNumberofCountries(int cityCount, int threshold, String comp) throws RemoteException {
        return submitAndWait(() -> {
            String key = CacheKey.makeKey("getNumberofCountries", cityCount, threshold, comp); // make key for the map

            // cache == null -> skip
            if (cache != null) {
                Object cachedResult = cache.get(key);

                // CACHE HIT
                if (cachedResult != null) {
                    return cachedResult;
                }
            }

            List<CityRecord> cities = Dataset.parse(datasetPath);
            int result = Dataset.getNumberofCountries(cities, cityCount, threshold, comp);

            if (cache != null) {
                cache.put(key, result);
            }

            return result;
        });
        }

        // RMI Function 4
        @Override
        public QueryResult getNumberofCountriesMM(int cityCount, int minPopulation, int maxPopulation) throws RemoteException {
        return submitAndWait(() -> {
            String key = CacheKey.makeKey("getNumberofCountriesMM", cityCount, minPopulation, maxPopulation); // make key for the map

            // cache == null -> skip
            if (cache != null) {
                Object cachedResult = cache.get(key);

                // CACHE HIT
                if (cachedResult != null) {
                    return cachedResult;
                }
            }

            List<CityRecord> cities = Dataset.parse(datasetPath);
            int result = Dataset.getNumberofCountriesMM(cities, cityCount, minPopulation, maxPopulation);

            if (cache != null) {
                cache.put(key, result);
            }

            return result;
        });
        }

    /* Used by the proxy to check this server's current load. */
    @Override
    public int getWaitingListSize() throws RemoteException {
        return waitingList.size();
    }

    /* Used by the proxy/client to know which zone answered. */
    @Override
    public int getZone() throws RemoteException {
        return zone;
    }

    /** port comes from args[], so several Server instances can run at once,
     * each with its own port and unique registry name.
     * The Proxy assigns the zone (in ascending order, on registration).
    **/
    public static void main(String[] args) {
        try {
            int port = args.length > 0 ? Integer.parseInt(args[0]) : 2000;
            String datasetPath = args.length > 1 ? args[1] : "data/exercise_1_dataset.csv";
            String cacheMode = args.length > 2 ? args[2] : "none";

            // Set by docker-compose. Inside a container "localhost" is only the container itself.
            String proxyHost = System.getenv().getOrDefault("PROXY_HOST", "localhost");
            String serverHost = System.getenv().getOrDefault("SERVER_HOST", "localhost");
            System.setProperty("java.rmi.server.hostname", serverHost);

            // Register with the proxy first and let it tell us which zone we are.
            Registry proxyRegistry = LocateRegistry.getRegistry(proxyHost, 1100);
            ProxyInterface proxy = (ProxyInterface) proxyRegistry.lookup("ProxyService");

            int zone = proxy.registerServer(serverHost, port);
            if (zone <= 0) {
                // Every zone (1..TOTAL_ZONES) is already taken, so there is nothing useful this server instance can do, so it does not start at all.
                System.out.println("[SERVER] FATAL: proxy rejected registration - no zones left. Exiting.");
                return;
            }
            System.out.println("[SERVER] Registered with proxy as zone " + zone);

            Server server = new Server(zone, datasetPath, cacheMode);
            System.out.println("[SERVER] Cache mode: " + cacheMode);

            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind("Server-Zone-" + zone, server);

            System.out.println("Server for zone " + zone + " running on port " + port + "...");
            System.out.println("Bound as 'Server-Zone-" + zone + "' in the RMI registry.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
