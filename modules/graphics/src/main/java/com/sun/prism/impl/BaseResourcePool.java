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
    // Number of calls to freeDisposalRequestedAndCheckResources() before we
    // consider a resource to have not been used in a hypothetical "FOREVER".
    private static final int FOREVER = 1024;
    // Number of calls to freeDisposalRequestedAndCheckResources() before we
    // consider a resource to have not been used "RECENTLY", with different
    // cutoffs for useful and unuseful textures.
    private static final int RECENTLY_USEFUL = 100;
    private static final int RECENT = 10;

    static interface Predicate {
        boolean test(ManagedResource<?> mr);
    }

    private static final Predicate stageTesters[];
    private static final String    stageReasons[];
    static {
        stageTesters = new Predicate[6];
        stageReasons = new String[6];
        stageTesters[0] = (mr) -> { return !mr.isInteresting() && mr.getAge() > FOREVER; };
        stageReasons[0] = "Pruning unuseful older than "+FOREVER;
        stageTesters[1] = (mr) -> { return !mr.isInteresting() && mr.getAge() > FOREVER/2; };
        stageReasons[1] = "Pruning unuseful older than "+FOREVER/2;
        stageTesters[2] = (mr) -> { return !mr.isInteresting() && mr.getAge() > RECENT; };
        stageReasons[2] = "Pruning unuseful older than "+RECENT;
        stageTesters[3] = (mr) -> { return mr.getAge() > FOREVER; };
        stageReasons[3] = "Pruning all older than "+FOREVER;
        stageTesters[4] = (mr) -> { return mr.getAge() > FOREVER/2; };
        stageReasons[4] = "Pruning all older than "+FOREVER/2;
        stageTesters[5] = (mr) -> { return mr.getAge() > RECENTLY_USEFUL; };
        stageReasons[5] = "Pruning all older than "+RECENTLY_USEFUL;
    }

    long managedSize;
    final long origTarget;
    long curTarget;
    final long maxSize;
    final ResourcePool<T> sharedParent;
    private final Thread managerThread;
    private WeakLinkedList<T> resourceHead;

    protected BaseResourcePool(long target, long max) {
        this(null, target, max);
    }

    protected BaseResourcePool(ResourcePool<T> parent) {
        this(parent, parent.target(), parent.max());
    }

    protected BaseResourcePool(ResourcePool<T> parent, long target, long max) {
        this.resourceHead = new WeakLinkedList<>();
        this.sharedParent = parent;
        this.origTarget = this.curTarget = target;
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
     * <li> Go through a few passes cleaning out any non-interesting resources
     *      that have not been used in a long time with decreasing cutoff
     *      limits for the maximum age of the resource.
     * <li> Go through more passes cleaning out even interesting resources that
     *      have not been used in a fairly long time with decreasing age limits.
     * <li> Attempt to grow the target to accommodate the new request.
     * <li> Finally, prune any resources that are not currently in the process
     *      of being used (i.e. locked or permanent).
     * </ol>
     *
     * @param needed
     * @return boolean indicating if the requested space is now available
     */
    public boolean cleanup(long needed) {
        if (used() + needed <= target()) return true;
        long wasused = used();
        long wanted = target() / 16;
        if (wanted < needed) {
            wanted = needed;
        }
        if (PrismSettings.poolDebug) {
            System.err.printf("Need %,d (hoping for %,d) from pool: %s\n", needed, wanted, this);
            printSummary(false);
        }

        try {
            // First cleanup pass is just for previously freed resources that
            // are in the Disposer queue already or were manually freed by
            // mechanisms and are still in the accounting list.
            // The pruner predicate choose no additional resources to free.
            Disposer.cleanUp();
            if (PrismSettings.poolDebug) System.err.println("Pruning obsolete in pool: "+this);
            cleanup((mr) -> { return false; });
            if (used() + wanted <= target()) return true;

            // Multiple stages of pruning useful and unuseful resources of
            // various ages as determined by the static initializer above.
            for (int stage = 0; stage < stageTesters.length; stage++) {
                if (PrismSettings.poolDebug) {
                    System.err.println(stageReasons[stage]+" in pool: "+this);
                }
                cleanup(stageTesters[stage]);
                if (used() + wanted <= target()) return true;
            }

            // Now look to grow the target if we can satisfy this allocation at
            // less than max().
            long rem = max() - used();
            if (wanted > rem) {
                wanted = needed;
            }
            if (wanted <= rem) {
                long grow = (max() - origTarget()) / 32;
                if (grow < wanted) {
                    grow = wanted;
                } else if (grow > rem) {
                    grow = rem;
                }
                setTarget(used() + grow);
                if (PrismSettings.poolDebug || PrismSettings.verbose) {
                    System.err.printf("Growing pool %s target to %,d\n", this, target());
                }
                return true;
            }

            // Finally, look to the garbage collector to dislodge some unreferenced
            // resources that we can free with a very aggressive age set of (0, 0)
            // which will target all unlocked/non-permanent textures.
            // Two tries, one with just a gc(), and a desperate one with a sleep...
            for (int i = 0; i < 2; i++) {
                pruneLastChance(i > 0);
                if (used() + needed <= max()) {
                    if (used() + needed > target()) {
                        setTarget(used() + needed);
                        if (PrismSettings.poolDebug || PrismSettings.verbose) {
                            System.err.printf("Growing pool %s target to %,d\n", this, target());
                        }
                    }
                    return true;
                }
            }

            // That was our last gasp, we either succeeded in making room under
            // the max() amount or we failed and need to return false.
            return false;
        } finally {
            if (PrismSettings.poolDebug) {
                System.err.printf("cleaned up %,d from pool: %s\n", wasused - used(), this);
                printSummary(false);
                System.err.println();
            }
        }
    }

    private void pruneLastChance(boolean desperate) {
        System.gc();
        if (desperate) {
            // Our alternative is to return false here and cause an allocation
            // failure which is usually bad news for any SG, so it is worth
            // sleeping on the second time around to give one last GC some time
            // to find a dead resource that was dropped on the floor...
            try { Thread.sleep(20); }
            catch (InterruptedException e) { }
        }
        Disposer.cleanUp();
        if (PrismSettings.poolDebug) {
            if (desperate) {
                System.err.print("Last chance pruning");
            } else {
                System.err.print("Pruning everything");
            }
            System.err.println(" in pool: "+this);
        }
        cleanup((mr) -> { return true; });
    }

    private void cleanup(Predicate predicate) {
        WeakLinkedList<T> prev = resourceHead;
        WeakLinkedList<T> cur = prev.next;
        while (cur != null) {
            ManagedResource<T> mr = cur.getResource();
            if (ManagedResource._isgone(mr)) {
                if (PrismSettings.poolDebug) showLink("unlinking", cur, false);
                recordFree(cur.size);
                cur = cur.next;
                prev.next = cur;
            } else if (!mr.isPermanent() &&
                       !mr.isLocked() &&
                       predicate.test(mr))
            {
                if (PrismSettings.poolDebug) showLink("pruning", cur, true);
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
    }

    static void showLink(String label, WeakLinkedList<?> cur, boolean showAge) {
        ManagedResource<?> mr = cur.getResource();
        System.err.printf("%s: %s (size=%,d)", label, mr, cur.size);
        if (mr != null) {
            if (showAge) {
                System.err.printf(" (age=%d)", mr.getAge());
            }
            if (mr.isPermanent())   System.err.print(" perm");
            if (mr.isLocked())      System.err.print(" lock");
            if (mr.isInteresting()) System.err.print(" int");
        }
        System.err.println();
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
            System.err.println();
        }
    }

    static String commas(long v) {
        return String.format("%,d", v);
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
        double percentTarget = target() * 100.0 / max();
        System.err.printf("%s: %,d used (%.1f%%), %,d target (%.1f%%), %,d max\n",
                          this, used(), percentUsed,
                          target(), percentTarget,
                          max());

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
        System.err.printf("average resource age is %.1f frames\n", avg_age);
        printpoolpercent(numancient, total, "at maximum supported age");
        printpoolpercent(numpermanent, total, "marked permanent");
        printpoolpercent(nummismatched, total, "have had mismatched locks");
        printpoolpercent(numlocked, total, "locked");
        printpoolpercent(numinteresting, total, "contain interesting data");
        printpoolpercent(numgone, total, "disappeared");
    }

    private static void printpoolpercent(int stat, int total, String desc) {
        double percent = stat * 100.0 / total;
        System.err.printf("%,d resources %s (%.1f%%)\n", stat, desc, percent);
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
    public final long origTarget() {
        return origTarget;
    }

    @Override
    public final long target() {
        return curTarget;
    }

    @Override
    public final void setTarget(long newTarget) {
        if (newTarget > maxSize) {
            throw new IllegalArgumentException("New target "+newTarget+
                                               " larger than max "+maxSize);
        }
        if (newTarget < origTarget) {
            throw new IllegalArgumentException("New target "+newTarget+
                                               " smaller than initial target "+origTarget);
        }
        curTarget = newTarget;
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
