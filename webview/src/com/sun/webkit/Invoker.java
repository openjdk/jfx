/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit;

import com.sun.webkit.perf.PerfLogger;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Invoker {
    private static Invoker instance;

    private static final PerfLogger locksLog = PerfLogger.getLogger("Locks");

    public static synchronized void setInvoker(Invoker invoker) {
        instance = invoker;
    }

    public static synchronized Invoker getInvoker() {
        return instance;
    }

    /*
     * Acquires the lock if it's not yet held by the current thread.
     *
     * @returns {@code true} if the lock has been acquired, otherwise {@code false}
     */
    protected boolean lock(ReentrantLock lock) {
        if (lock.getHoldCount() == 0) {

            lock.lock();
            locksLog.resumeCount(isEventThread() ? "EventThread" : "RenderThread");
            return true;
        }
        return false;
    }

    /*
     * Releases the lock if it's held by the current thread.
     *
     * @returns {@code true} if the lock has been released, otherwise {@code false}
     */
    protected boolean unlock(ReentrantLock lock) {
        if (lock.getHoldCount() != 0) {

            locksLog.suspendCount(isEventThread() ? "EventThread" : "RenderThread");
            lock.unlock();
            return true;
        }
        return false;
    }

    protected abstract boolean isEventThread();

    /**
     * Throws {@link IllegalStateException} if the current thread is not
     * the event thread.
     * @throws IllegalStateException if the current thread is not the event
     *         thread
     */
    protected void checkEventThread() {
        if (!isEventThread()) {
            throw new IllegalStateException("Current thread is not event thread"
                    + ", current thread: " + Thread.currentThread().getName());
        }
    }

    public abstract void invokeOnEventThread(Runnable r);

    public abstract void postOnEventThread(Runnable r);
}
