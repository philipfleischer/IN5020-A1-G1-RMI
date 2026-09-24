package com.ass1.server;

import java.io.Serializable;

/**
 * Wraps a query's answer together with the timing info the output format
 * requires (waiting time, execution time, serving zone).
 *
 * Implements Serializable so it travels by value back to the client over
 * RMI, so the client gets its own independent copy.
 *
 * "value" holds the actual answer: a Long for getPopulationofCountry,
 * an Integer for the other three methods. The client knows which method
 * it called, so it knows which type to expect.
 */
public class QueryResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Object value;
    private final long waitingTimeMs;
    private final long executionTimeMs;
    private final int servedByZone;

    public QueryResult(Object value, long waitingTimeMs, long executionTimeMs, int servedByZone) {
        this.value = value;
        this.waitingTimeMs = waitingTimeMs;
        this.executionTimeMs = executionTimeMs;
        this.servedByZone = servedByZone;
    }

    public Object getValue() {
        return value;
    }

    /* Time (ms) the task spent in the queue before execution started. */
    public long getWaitingTimeMs() {
        return waitingTimeMs;
    }

    /* Time (ms) spent actually computing the answer. */
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    /* Zone number of the server that processed this request. */
    public int getServedByZone() {
        return servedByZone;
    }

    @Override
    public String toString() {
        return value + " (waiting: " + waitingTimeMs + "ms, execution: " + executionTimeMs
                + "ms, server zone: " + servedByZone + ")";
    }
}
