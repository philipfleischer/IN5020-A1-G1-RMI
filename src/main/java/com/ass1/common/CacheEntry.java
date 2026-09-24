package com.ass1.common;

/** One entry in the Cache: the stored value, plus when it was last read.
 * lastUsed is what the OLDEST eviction strategy looks at to find which entry
 * to remove. FIFO just ignores it.
**/
public class CacheEntry<V> {

    private V value;
    private long lastUsed;

    public CacheEntry(V value) {
        this.value = value;
        this.lastUsed = System.currentTimeMillis();
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }
}
