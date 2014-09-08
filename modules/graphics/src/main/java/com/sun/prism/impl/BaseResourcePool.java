/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.prism.impl;

import java.lang.ref.WeakReference;

/**
 * The base implementation of the {@link ResourcePool} interface, providing
 * bookkeeping for the {@link managed()} method and support for sharing
 * resources amongst multiple pools.
 * @param <T> the type of objects stored in this resource pool
 */
public abstract class BaseResourcePool<T> implements ResourcePool<T> {
    // Number of calls to checkAndDispose() before we consider a resource to
    // have not been used in a hypothetical "FOREVER".
    private static final int FOREVER = 1024;

    /* 
     * Which resources to clean up in a call to {@link cleanup}.
     */
    public static enum PruningLevel {
        /**
         * Only resources that have been reclaimed through GC or other
         * asynchronous disposal mechanisms should be pruned.
         * This basically makes sure that our resource accounting is up
         * to date.
         */
        OBSOLETE,

        /**
         * Only (unlocked, nonpermanent) resources that have not been used in
         * a very long time should be pruned.
         */
        UNUSED_FOREVER,

        /**
         * Only (unlocked, nonpermanent) resources that hold no interesting
         * data should be pruned.
         */
        UNINTERESTING,

        /**
         * Any resource that is unlocked and nonpermanent should be pruned.
         */
        ALL_UNLOCKED
    }

    long managedSize;
    final long maxSize;
    final ResourcePool<T> sharedParent;
    private final Thread managerThread;
    private WeakLinkedList<T> resourceHead;

    protected BaseResourcePool() {
        this(null, Long.MAX_VALUE);
    }

    protected BaseResourcePool(long max) {
        this(null, max);
    }

    protected BaseResourcePool(ResourcePool<T> parent) {
        this(parent, parent.max());
    }

    protected BaseResourcePool(ResourcePool<T> parent, long max) {
        this.resourceHead = new WeakLinkedList<>();
        this.sharedParent = parent;
        this.maxSize = ((parent == null)
                        ? max
                        : Math.min(parent.max(), max));
        managerThread = Thread.currentThread();
    }

    /**
     * Clean up the resources in the indicated pool using a standard
     * algorithm until at least the specified amount of resource units
     * have been reclaimed.
     * The standard algorithm uses the following stages until it obtains
     * enough room in the pool:
     * <ol>
     * <li> Prune any resources which are already free, but have not been
     *      accounted for yet.
     * <li> Prune any resources that have not been used in a very long time.
     * <li> Go through 2 more passes cleaning out any resources that have
     *      not been used in a long time with decreasing cutoff limits for
     *      the maximum age of the resource.
     * <li> Finally, prune any resources that are not currently being used
     *      (i.e. locked or permanent).
     * </ol>
     * 
     * @param needed
     * @return boolean indicating if the requested space is now available
     */
    public boolean cleanup(long needed) {
        if (used() + needed <= target()) return true;
        cleanup(PruningLevel.OBSOLETE, FOREVER);
        if (used() + needed <= target()) return true;
        cleanup(PruningLevel.UNUSED_FOREVER, FOREVER);
        if (used() + needed <= target()) return true;
        cleanup(PruningLevel.UNUSED_FOREVER, FOREVER/2);
        if (used() + needed <= target()) return true;
        cleanup(PruningLevel.UNUSED_FOREVER, FOREVER/4);
        if (used() + needed <= target()) return true;
        cleanup(PruningLevel.UNINTERESTING, FOREVER);
        if (used() + needed <= target()) return true;
        System.gc();
        cleanup(PruningLevel.ALL_UNLOCKED, FOREVER);
        if (used() + needed <= max()) return true;
        // Our alternative is to return false and cause an allocation
        // failure which is usually bad news for any SG, so it is worth
        // sleeping to give the GC some time to find a dead resource that
        // was dropped on the floor...
        System.gc();
        try { Thread.sleep(20); } catch (InterruptedException e) { }
        cleanup(PruningLevel.ALL_UNLOCKED, FOREVER);
        return (used() + needed <= max());
    }

    private void cleanup(PruningLevel plevel, int max_age) {
        if (PrismSettings.poolDebug) {
            switch (plevel) {
                case OBSOLETE: System.err.print("Pruning"); break;
                case UNUSED_FOREVER: System.err.print("Cleaning up unused in "+max_age); break;
                case UNINTERESTING: System.err.print("Cleaning up uninteresting"); break;
                case ALL_UNLOCKED: System.err.print("Aggressively cleaning up"); break;
                default: throw new InternalError("Unrecognized pruning level: "+plevel);
            }
            System.err.println(" pool: "+this);
            printSummary(false);
        }
        long wasused = used();
        WeakLinkedList<T> prev = resourceHead;
        WeakLinkedList<T> cur = prev.next;
        while (cur != null) {
            ManagedResource<?> mr = cur.getResource();
            if (ManagedResource._isgone(mr)) {
                if (PrismSettings.poolDebug) {
                    System.err.println("pruning: "+mr+" ("+cur.size+")"+
                                       ((mr == null) ? "" :
                                        ((mr.isPermanent() ? " perm" : "") +
                                         (mr.isLocked() ? " lock" : "") +
                                         (mr.isInteresting() ? " int" : ""))));
                }
                recordFree(cur.size);
                cur = cur.next;
                prev.next = cur;
            } else if (plevel != PruningLevel.OBSOLETE &&
                       !mr.isPermanent() &&
                       !mr.isLocked() &&
                       (plevel == PruningLevel.ALL_UNLOCKED ||
                        (plevel == PruningLevel.UNINTERESTING && !mr.isInteresting()) ||
                        (/* plevel == PruningLevel.UNUSED_FOREVER && */ mr.getAge() >= max_age)))
            {
                if (PrismSettings.poolDebug) {
                    System.err.println("disposing: "+mr+" ("+cur.size+") age="+mr.getAge()+
                                       (mr.isPermanent() ? " perm" : "") +
                                       (mr.isLocked() ? " lock" : "") +
                                       (mr.isInteresting() ? " int" : ""));
                }
                mr.free();
                mr.resource = null;
                recordFree(cur.size);
                cur = cur.next;
                prev.next = cur;
            } else {
                prev = cur;
                cur = cur.next;
            }
        }
        if (PrismSettings.poolDebug) {
            long isused = used();
            System.err.println("cleaned up "+(wasused - isused)+" from pool: "+this);
            printSummary(false);
        }
    }

    /**
     * Check that all resources are in the correct state for an idle condition
     * and free any resources which were disposed from a non-resource thread.
     * This method must be called on a thread that is appropriate for disposing
     * and managing resources for the resource pools.
     * The boolean {@code forgiveStaleLocks} parameter is used to indicate that
     * an exceptional condition occurred which caused the caller to abort a
     * cycle of resource usage, potentially with outstanding resource locks.
     * This method will unlock all non-permanent resources that have outstanding
     * locks if {@code forgiveStaleLocks} is {@code true}, or it will print out
     * a warning and a resource summary if that parameter is {@code false}.
     * 
     * @param forgiveStaleLocks {@code true} if the caller wishes to forgive
     *         and unlock all outstanding locks on non-permanent resources
     */
    @Override
    public void freeDisposalRequestedAndCheckResources(boolean forgiveStaleLocks) {
        boolean anyLockedResources = false;
        WeakLinkedList<T> prev = resourceHead;
        WeakLinkedList<T> cur = prev.next;
        while (cur != null) {
            ManagedResource<?> mr = cur.getResource();
            if (ManagedResource._isgone(mr)) {
                recordFree(cur.size);
                cur = cur.next;
                prev.next = cur;
            } else {
                if (!mr.isPermanent()) {
                    if (mr.isLocked() && !mr.wasMismatched()) {
                        if (forgiveStaleLocks) {
                            mr.unlockall();
                        } else {
                            mr.setMismatched();
                            anyLockedResources = true;
                        }
                    }
                    mr.bumpAge(FOREVER);
                }
                prev = cur;
                cur = cur.next;
            }
        }

        if (PrismSettings.poolStats || anyLockedResources) {
            if (anyLockedResources) {
                System.err.println("Outstanding resource locks detected:");
            }
            printSummary(true);
        }
    }

    public void printSummary(boolean printlocksources) {
        int numgone = 0;
        int numlocked = 0;
        int numpermanent = 0;
        int numinteresting = 0;
        int nummismatched = 0;
        int numancient = 0;
        long total_age = 0;
        int total = 0;
        boolean trackLockSources = ManagedResource.trackLockSources;

        double percentUsed = used() * 100.0 / max();
        double percentManaged = managed() * 100.0 / max();
        String str =
            String.format("%s: %,d used (%.1f%%), %,d managed (%.1f%%), %,d total",
                          this, used(), percentUsed,
                          managed(), percentManaged,
                          max());
        System.err.println(str);

        for (WeakLinkedList<T> cur = resourceHead.next; cur != null; cur = cur.next) {
            ManagedResource<T> mr = cur.getResource();
            total++;
            if (mr == null || !mr.isValid() || mr.isDisposalRequested()) {
                numgone++;
            } else {
                int a = mr.getAge();
                total_age += a;
                if (a >= FOREVER) {
                    numancient++;
                }
                if (mr.wasMismatched()) {
                    nummismatched++;
                }
                if (mr.isPermanent()) {
                    numpermanent++;
                } else if (mr.isLocked()) {
                    numlocked++;
                    if (trackLockSources && printlocksources) {
                        for (Throwable th : mr.lockedFrom) {
                            th.printStackTrace(System.err);
                        }
                        mr.lockedFrom.clear();
                    }
                }
                if (mr.isInteresting()) {
                    numinteresting++;
                }
            }
        }

        double avg_age = ((double) total_age) / total;
        System.err.println(total+" total resources being managed");
        System.err.println(String.format("average resource age is %.1f frames", avg_age));
        printpoolpercent(numancient, total, "at maximum supported age");
        printpoolpercent(numpermanent, total, "marked permanent");
        printpoolpercent(nummismatched, total, "have had mismatched locks");
        printpoolpercent(numlocked, total, "locked");
        printpoolpercent(numinteresting, total, "contain interesting data");
        printpoolpercent(numgone, total, "disappeared");
        System.err.println();
    }

    private static void printpoolpercent(int stat, int total, String desc) {
        double percent = stat * 100.0 / total;
        String str = String.format("%,d resources %s (%.1f%%)", stat, desc, percent);
        System.err.println(str);
    }

    @Override
    public boolean isManagerThread() {
        return Thread.currentThread() == managerThread;
    }

    @Override
    public final long managed() {
        return managedSize;
    }

    @Override
    public long used() {
        if (sharedParent != null) {
            return sharedParent.used();
        }
        return managedSize;
    }

    @Override
    public final long max() {
        return maxSize;
    }

    @Override
    public boolean prepareForAllocation(long size) {
        return cleanup(size);
    }

    @Override
    public final void recordAllocated(long size) {
        managedSize += size;
    }

    @Override
    public final void resourceManaged(ManagedResource<T> mr) {
        long size = size(mr.resource);
        resourceHead.insert(mr, size);
        recordAllocated(size);
    }

    @Override
    public final void resourceFreed(ManagedResource<T> freed) {
        WeakLinkedList<T> prev = resourceHead;
        WeakLinkedList<T> cur = prev.next;
        while (cur != null) {
            ManagedResource<T> res = cur.getResource();
            if (res == null || res == freed) {
                recordFree(cur.size);
                cur = cur.next;
                prev.next = cur;
                if (res == freed) {
                    return;
                }
            } else {
                prev = cur;
                cur = cur.next;
            }
        }
        throw new IllegalStateException("unmanaged resource freed from pool "+this);
    }

    @Override
    public final void recordFree(long size) {
        managedSize -= size;
        if (managedSize < 0) {
            throw new IllegalStateException("Negative resource amount");
        }
    }

    static class WeakLinkedList<T> {
        final WeakReference<ManagedResource<T>> theResourceRef;
        final long size;
        WeakLinkedList<T> next;

        WeakLinkedList() {
            this.theResourceRef = null;
            this.size = 0L;
        }

        WeakLinkedList(ManagedResource<T> mresource, long size, WeakLinkedList<T> next) {
            this.theResourceRef = new WeakReference<>(mresource);
            this.size = size;
            this.next = next;
        }

        void insert(ManagedResource<T> mresource, long size) {
            this.next = new WeakLinkedList<>(mresource, size, next);
        }

        ManagedResource<T> getResource() {
            return theResourceRef.get();
        }
    }
}
