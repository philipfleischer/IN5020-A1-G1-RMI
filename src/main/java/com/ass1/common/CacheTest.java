package com.ass1.common;

public class CacheTest {

    public static void main(String[] args) throws InterruptedException {

        testBasicGet();
        testCacheMiss();
        testFIFO();
        testOldest();
        testOldestAfterAccess();
        testUpdateExisting();

    }

    public static void testUpdateExisting() {

        Cache<String, Integer> cache = new Cache<>(3, "FIFO");

        cache.put("A", 10);
        cache.put("B", 20);
        cache.put("C", 30);

        // Cache is full, but B already exists.
        // This should NOT remove A.
        cache.put("B", 999);

        System.out.println("TEST 6 - Update existing key");

        System.out.println("A should exist: true");
        System.out.println("A exists:       " + cache.contains("A"));

        System.out.println("B should be: 999");
        System.out.println("B is:        " + cache.get("B"));

        System.out.println("C should exist: true");
        System.out.println("C exists:       " + cache.contains("C"));

        System.out.println();
    }

    // Test normal put and get
    public static void testBasicGet() {

        Cache<String, Integer> cache = new Cache<>(3, "FIFO");

        cache.put("A", 10);
        cache.put("B", 20);

        System.out.println("TEST 1 - Basic get");
        System.out.println("Expected: 10");
        System.out.println("Actual:   " + cache.get("A"));
        System.out.println();
    }


    // Test getting something that doesn't exist
    public static void testCacheMiss() {

        Cache<String, Integer> cache = new Cache<>(3, "FIFO");

        cache.put("A", 10);

        System.out.println("TEST 2 - Cache miss");
        System.out.println("Expected: null");
        System.out.println("Actual:   " + cache.get("B"));
        System.out.println();
    }


    // FIFO should remove the FIRST inserted entry
    public static void testFIFO() {

        Cache<String, Integer> cache = new Cache<>(3, "FIFO");

        cache.put("A", 10);
        cache.put("B", 20);
        cache.put("C", 30);

        // Cache is full:
        // A, B, C
        //
        // Adding D should remove A.

        cache.put("D", 40);

        System.out.println("TEST 3 - FIFO");
        System.out.println("A should exist: false");
        System.out.println("A exists:       " + cache.contains("A"));

        System.out.println("B should exist: true");
        System.out.println("B exists:       " + cache.contains("B"));

        System.out.println("C should exist: true");
        System.out.println("C exists:       " + cache.contains("C"));

        System.out.println("D should exist: true");
        System.out.println("D exists:       " + cache.contains("D"));

        System.out.println();
    }


    // OLDEST without accessing anything again
    public static void testOldest() throws InterruptedException {

        Cache<String, Integer> cache = new Cache<>(3, "OLDEST");

        cache.put("A", 10);

        Thread.sleep(10);

        cache.put("B", 20);

        Thread.sleep(10);

        cache.put("C", 30);

        // A has the oldest lastUsed timestamp.
        // Adding D should therefore remove A.

        cache.put("D", 40);

        System.out.println("TEST 4 - OLDEST");
        System.out.println("A should exist: false");
        System.out.println("A exists:       " + cache.contains("A"));

        System.out.println("B should exist: true");
        System.out.println("B exists:       " + cache.contains("B"));

        System.out.println("C should exist: true");
        System.out.println("C exists:       " + cache.contains("C"));

        System.out.println("D should exist: true");
        System.out.println("D exists:       " + cache.contains("D"));

        System.out.println();
    }


    // Most important OLDEST test:
    // accessing A should update A's lastUsed time
    public static void testOldestAfterAccess() throws InterruptedException {

        Cache<String, Integer> cache = new Cache<>(3, "OLDEST");

        cache.put("A", 10);

        Thread.sleep(10);

        cache.put("B", 20);

        Thread.sleep(10);

        cache.put("C", 30);

        Thread.sleep(10);

        // A was originally oldest.
        // But now we access A again.
        cache.get("A");

        // A becomes newest.
        //
        // Times are now conceptually:
        //
        // B = oldest
        // C
        // A = newest

        cache.put("D", 40);

        System.out.println("TEST 5 - OLDEST after accessing A");

        System.out.println("A should exist: true");
        System.out.println("A exists:       " + cache.contains("A"));

        System.out.println("B should exist: false");
        System.out.println("B exists:       " + cache.contains("B"));

        System.out.println("C should exist: true");
        System.out.println("C exists:       " + cache.contains("C"));

        System.out.println("D should exist: true");
        System.out.println("D exists:       " + cache.contains("D"));

        System.out.println();
    }
}