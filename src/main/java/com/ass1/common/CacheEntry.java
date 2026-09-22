package com.ass1.common;

public class CacheEntry<V> {

    private V value; // value from query
    private long lastUsed;

    public CacheEntry(V value) {
        this.value = value;
        this.lastUsed = System.currentTimeMillis();
    }

    // Getter for value
    public V getValue() {
        return value;
    }

    // Setter for value
    public void setValue(V value) {
        this.value = value;
    }

    // Getter for lastUsed
    public long getLastUsed() {
        return lastUsed;
    }

    // Setter for lastUsed
    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }

    // Praktisk metode når entry blir brukt
    public void updateLastUsed() {
        this.lastUsed = System.currentTimeMillis();
    }
}