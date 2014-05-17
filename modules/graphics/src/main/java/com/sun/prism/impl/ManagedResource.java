/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.prism.GraphicsResource;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class ManagedResource<T> implements GraphicsResource {
    private static final boolean trackLockSources = false;
    // Number of calls to checkAndDispose() before we consider a resource to
    // have not been used in a hypothetical "FOREVER".
    private static final int FOREVER = 1024;

    private static WeakLinkedList resourceHead = new WeakLinkedList();

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

    /**
     * Clean up the resources in the indicated pool according to the
     * specified level of aggressiveness.
     * 
     * @param pool which pool to clean up resources from
     * @param plevel how aggressively to clean up the resources
     * @see PruningLevel
     */
    public static void cleanup(ResourcePool pool, PruningLevel plevel) {
        cleanup(pool, plevel, FOREVER);
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
     * @param pool
     * @param plevel
     * @param needed
     */
    public static boolean cleanup(ResourcePool pool, long needed) {
        if (pool.used() + needed <= pool.target()) return true;
        cleanup(pool, PruningLevel.OBSOLETE, FOREVER);
        if (pool.used() + needed <= pool.target()) return true;
        cleanup(pool, PruningLevel.UNUSED_FOREVER, FOREVER);
        if (pool.used() + needed <= pool.target()) return true;
        cleanup(pool, PruningLevel.UNUSED_FOREVER, FOREVER/2);
        if (pool.used() + needed <= pool.target()) return true;
        cleanup(pool, PruningLevel.UNUSED_FOREVER, FOREVER/4);
        if (pool.used() + needed <= pool.target()) return true;
        cleanup(pool, PruningLevel.UNINTERESTING, FOREVER);
        if (pool.used() + needed <= pool.target()) return true;
        System.gc();
        cleanup(pool, PruningLevel.ALL_UNLOCKED, FOREVER);
        if (pool.used() + needed <= pool.max()) return true;
        // Our alternative is to return false and cause an allocation
        // failure which is usually bad news for any SG, so it is worth
        // sleeping to give the GC some time to find a dead resource that
        // was dropped on the floor...
        System.gc();
        try { Thread.sleep(20); } catch (InterruptedException e) { }
        cleanup(pool, PruningLevel.ALL_UNLOCKED, FOREVER);
        return (pool.used() + needed <= pool.max());
    }

    private static boolean _isgone(ManagedResource mr) {
        if (mr == null) return true;
        if (mr.disposalRequested) {
            mr.free();
            mr.resource = null;
            mr.disposalRequested = false;
            return true;
        }
        return !mr.isValid();
    }

    private static void cleanup(ResourcePool pool, PruningLevel plevel, int max_age) {
        if (PrismSettings.poolDebug) {
            switch (plevel) {
                case OBSOLETE: System.err.print("Pruning"); break;
                case UNUSED_FOREVER: System.err.print("Cleaning up unused in "+max_age); break;
                case UNINTERESTING: System.err.print("Cleaning up uninteresting"); break;
                case ALL_UNLOCKED: System.err.print("Aggressively cleaning up"); break;
                default: throw new InternalError("Unrecognized pruning level: "+plevel);
            }
            System.err.println(" pool: "+pool);
            printSummary(false);
        }
        long wasused = pool.used();
        WeakLinkedList prev = resourceHead;
        WeakLinkedList cur = prev.next;
        while (cur != null) {
            ManagedResource mr = cur.getResource();
            if (_isgone(mr)) {
                if (PrismSettings.poolDebug) {
                    System.err.println("pruning: "+mr+" ("+cur.size+")"+
                                       ((mr == null) ? "" :
                                        ((mr.isPermanent() ? " perm" : "") +
                                         (mr.isLocked() ? " lock" : "") +
                                         (mr.isInteresting() ? " int" : ""))));
                }
                cur.pool.recordFree(cur.size);
                cur = cur.next;
                prev.next = cur;
            } else if (plevel != PruningLevel.OBSOLETE &&
                       mr.getPool() == pool &&
                       !mr.isPermanent() &&
                       !mr.isLocked() &&
                       (plevel == PruningLevel.ALL_UNLOCKED ||
                        (plevel == PruningLevel.UNINTERESTING && !mr.isInteresting()) ||
                        (/* plevel == PruningLevel.UNUSED_FOREVER && */ mr.age >= max_age)))
            {
                if (PrismSettings.poolDebug) {
                    System.err.println("disposing: "+mr+" ("+cur.size+") age="+mr.age+
                                       (mr.isPermanent() ? " perm" : "") +
                                       (mr.isLocked() ? " lock" : "") +
                                       (mr.isInteresting() ? " int" : ""));
                }
                mr.free();
                mr.resource = null;
                cur.pool.recordFree(cur.size);
                cur = cur.next;
                prev.next = cur;
            } else {
                prev = cur;
                cur = cur.next;
            }
        }
        if (PrismSettings.poolDebug) {
            long isused = pool.used();
            System.err.println("cleaned up "+(wasused - isused)+" from pool: "+pool);
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
    public static void freeDisposalRequestedAndCheckResources(boolean forgiveStaleLocks) {
        boolean anyLockedResources = false;
        WeakLinkedList prev = resourceHead;
        WeakLinkedList cur = prev.next;
        while (cur != null) {
            ManagedResource mr = cur.getResource();
            if (_isgone(mr)) {
                cur.pool.recordFree(cur.size);
                cur = cur.next;
                prev.next = cur;
            } else {
                if (!mr.isPermanent()) {
                    if (mr.isLocked() && !mr.mismatchDetected) {
                        if (forgiveStaleLocks) {
                            mr.unlockall();
                        } else {
                            mr.mismatchDetected = true;
                            anyLockedResources = true;
                        }
                    }
                    int age = mr.age;
                    if (age < FOREVER) {
                        mr.age = age + 1;
                    }
                }
                prev = cur;
                cur = cur.next;
            }
        }

        if (PrismSettings.poolStats || anyLockedResources) {
            if (anyLockedResources) {
                System.err.println("Outstanding resource locks detected:");
            }
            ManagedResource.printSummary();
        }
    }

    public static void printSummary() {
        printSummary(true);
    }

    public static void printSummary(boolean printlocksources) {
        int numgone = 0;
        int numlocked = 0;
        int numpermanent = 0;
        int numinteresting = 0;
        int nummismatched = 0;
        int numancient = 0;
        long total_age = 0;
        int total = 0;
        HashMap<ResourcePool, ResourcePool> poolsSeen = new HashMap<ResourcePool, ResourcePool>();
        for (WeakLinkedList cur = resourceHead.next; cur != null; cur = cur.next) {
            ManagedResource mr = cur.getResource();
            total++;
            if (mr == null || !mr.isValid() || mr.disposalRequested) {
                numgone++;
            } else {
                ResourcePool pool = mr.getPool();
                if (!poolsSeen.containsKey(pool)) {
                    poolsSeen.put(pool, pool);
                    double percentUsed = pool.used() * 100.0 / pool.max();
                    double percentManaged = pool.managed() * 100.0 / pool.max();
                    String str =
                        String.format("%s: %,d used (%.1f%%), %,d managed (%.1f%%), %,d total",
                                      pool, pool.used(), percentUsed,
                                      pool.managed(), percentManaged,
                                      pool.max());
                    System.err.println(str);
                }
                total_age += mr.age;
                if (mr.age >= FOREVER) {
                    numancient++;
                }
                if (mr.mismatchDetected) {
                    nummismatched++;
                }
                if (mr.isPermanent()) {
                    numpermanent++;
                } else if (mr.isLocked()) {
                    numlocked++;
                    if (trackLockSources && printlocksources) {
                        ArrayList<Throwable> list = mr.lockedFrom;
                        for (Throwable th : list) {
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

    protected T resource;
    private final ResourcePool<T> pool;
    private int lockcount;
    private int employcount;
    ArrayList<Throwable> lockedFrom;
    private boolean permanent;
    private boolean mismatchDetected;
    private boolean disposalRequested = false;
    private int age;

    protected ManagedResource(T resource, ResourcePool<T> pool) {
        this.resource = resource;
        this.pool = pool;
        if (trackLockSources) {
            this.lockedFrom = new ArrayList<Throwable>();
        }
        link();
        lock();
    }

    private void link() {
        resourceHead.insert(this);
    }

    private void unlink() {
        WeakLinkedList prev = resourceHead;
        WeakLinkedList cur = prev.next;
        while (cur != null) {
            ManagedResource mr = cur.getResource();
            if (mr == null || mr == this) {
                cur.pool.recordFree(cur.size);
                cur = cur.next;
                prev.next = cur;
                if (mr == this) {
                    break;
                }
            } else {
                prev = cur;
                cur = cur.next;
            }
        }
    }

    public final T getResource() {
        assertLocked();
        return resource;
    }

    public final ResourcePool getPool() {
        return pool;
    }

    public boolean isValid() {
        return resource != null && !disposalRequested;
    }

    public final boolean isLocked() {
        return lockcount > 0;
    }

    public final int getLockCount() {
        return lockcount;
    }

    public final void assertLocked() {
        if (lockcount <= 0) {
            throw new IllegalStateException("Operation requires resource lock");
        }
    }

    public final boolean isPermanent() {
        return permanent;
    }

    public final boolean isInteresting() {
        return employcount > 0;
    }

    public final int getInterestCount() {
        return employcount;
    }

    public void free() {
    }

    @Override
    public final void dispose() {
        if (pool.isManagerThread()) {
            if (resource != null) {
                free();
                disposalRequested = false;
                resource = null;
                unlink();
            }
        } else {
            disposalRequested = true;
        }
    }

    public final void makePermanent() {
        assertLocked();
        permanent = true;
    }

    public final T lock() {
        lockcount++;
        age = 0;
        if (trackLockSources && !permanent) {
            lockedFrom.add(new Throwable(Integer.toString(lockcount)));
        }
        return resource;
    }

    private void unlockall() {
        lockcount = 0;
        if (trackLockSources && !permanent) {
            lockedFrom.clear();
        }
    }

    public final void unlock() {
        assertLocked();
        lockcount--;
        if (trackLockSources && !permanent && lockcount == 0) {
            lockedFrom.clear();
        }
    }

    public final void contentsUseful() {
        assertLocked();
        employcount++;
    }

    public final void contentsNotUseful() {
        if (employcount <= 0) {
            throw new IllegalStateException("Resource obsoleted too many times");
        }
        employcount--;
    }

    static class WeakLinkedList {
        final WeakReference<ManagedResource> theResourceRef;
        final ResourcePool pool;
        final long size;
        WeakLinkedList next;

        WeakLinkedList() {
            this.theResourceRef = null;
            this.pool = null;
            this.size = 0L;
        }

        WeakLinkedList(ManagedResource mresource, WeakLinkedList next) {
            this.theResourceRef = new WeakReference<ManagedResource>(mresource);
            this.pool = mresource.pool;
            this.size = pool.size(mresource.resource);
            pool.recordAllocated(size);
            this.next = next;
        }

        void insert(ManagedResource resource) {
            this.next = new WeakLinkedList(resource, next);
        }

        ManagedResource getResource() {
            return theResourceRef.get();
        }
    }
}
