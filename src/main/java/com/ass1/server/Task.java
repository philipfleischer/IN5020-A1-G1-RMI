package com.ass1.server;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * One task sitting in the server's waiting list.
 *
 * "work" describes WHAT to compute (e.g. "sum Norway's population") without
 * actually running it yet the execution thread runs it later, in order.
 *
 * "future" is how the RMI call-thread (which is blocked waiting for an
 * answer) gets notified once the execution thread has finished this task.
 * Think of it as a small mailbox: the calling thread checks the mailbox
 * (future.get()) and simply waits until the execution thread puts the
 * answer in it (future.complete(...)).
 */
public class Task {
    private final Callable<Object> work;
    private final long arrivalTimeMs;
    private final CompletableFuture<QueryResult> future = new CompletableFuture<>();

    public Task(Callable<Object> work) {
        this.work = work;
        this.arrivalTimeMs = System.currentTimeMillis();
    }

    public Callable<Object> getWork() {
        return work;
    }

    /** Timestamp (ms) when this task entered the waiting list. Used to compute waiting time. */
    public long getArrivalTimeMs() {
        return arrivalTimeMs;
    }

    public CompletableFuture<QueryResult> getFuture() {
        return future;
    }
}
