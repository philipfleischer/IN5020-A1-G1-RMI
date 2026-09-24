package com.ass1.common;
import java.util.LinkedHashMap;
import java.util.Map;


public class Cache<K, V> {

    private int maxEntries; // max entries is Client=45 | Server=150
    private String method; // choose between methods "FIFO" or "OLDEST"

    private Map<K, CacheEntry<V>> cache;

    public Cache(int maxEntries, String method) {
        this.maxEntries = maxEntries;
        this.method = method;
        this.cache = new LinkedHashMap<>();
    }

    /**
     * synchronized: the server side only ever touches this from its one execution
     * thread, but the client now fires off queries concurrently (see Client.java),
     * so several threads can call get()/put() on the same client-side cache at once.
     * Plain LinkedHashMap is not thread-safe under concurrent access.
    **/
    public synchronized V get(K key) {
        CacheEntry<V> entry = cache.get(key);

        // cache miss
        if (entry == null) {
            return null;
        }

        // CACHE HIT
        entry.setLastUsed(System.currentTimeMillis());

        return entry.getValue();
    }

    // delete latest added cache entry
    private void deleteFirst(){
        K firstKey = cache.keySet().iterator().next();
        cache.remove(firstKey);
    }

    // delete oldest used cache entry
    private void deleteOldest(){
        K oldestKey = null;
        long oldestTime = Long.MAX_VALUE;

        for (Map.Entry<K, CacheEntry<V>> entry : cache.entrySet()) {

            if (entry.getValue().getLastUsed() < oldestTime) {
                oldestTime = entry.getValue().getLastUsed();
                oldestKey = entry.getKey();
            }
        }

        if (oldestKey != null) {
            cache.remove(oldestKey);
        }
    }

    // if full -> oldest/first is deleted and newest is added
    public synchronized void put(K key, V value) {

        if (!cache.containsKey(key) && cache.size() >= maxEntries) {

            if (method.equals("FIFO")) {
                deleteFirst();

            } else if (method.equals("OLDEST")) {
                deleteOldest();
            }
        }

        cache.put(key, new CacheEntry<>(value));
    }

    // returns true or false if a entry is in the cache
    public synchronized boolean contains(K key) {
        return cache.containsKey(key);
    }
}
