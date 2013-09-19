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
    private static WeakLinkedList resourceHead = new WeakLinkedList();

    public static enum PruningLevel {
        OBSOLETE, UNINTERESTING, ALL_UNLOCKED
    }

    public static void cleanup(ResourcePool pool, PruningLevel plevel) {
        if (PrismSettings.poolDebug) {
            switch (plevel) {
                case OBSOLETE: System.err.print("Pruning"); break;
                case UNINTERESTING: System.err.print("Cleaning up"); break;
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
            if (mr == null || !mr.isValid()) {
                if (PrismSettings.poolDebug) {
                    System.err.println("pruning: "+mr+" ("+cur.size+") "+
                                       ((mr == null) ? "" :
                                        ((mr.isPermanent() ? " perm " : "") +
                                         (mr.isLocked() ? " lock " : "") +
                                         (mr.isInteresting() ? " int " : ""))));
                }
                cur.pool.recordFree(cur.size);
                cur = cur.next;
                prev.next = cur;
            } else if (plevel != PruningLevel.OBSOLETE &&
                       mr.getPool() == pool &&
                       !mr.isPermanent() &&
                       !mr.isLocked() &&
                       (plevel == PruningLevel.ALL_UNLOCKED || !mr.isInteresting()))
            {
                if (PrismSettings.poolDebug) {
                    System.err.println("disposing: "+mr+" ("+cur.size+") "+
                                       (mr.isPermanent() ? " perm " : "") +
                                       (mr.isLocked() ? " lock " : "") +
                                       (mr.isInteresting() ? " int " : ""));
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

    public static boolean anyLockedResources() {
        for (WeakLinkedList cur = resourceHead.next; cur != null; cur = cur.next) {
            ManagedResource mr = cur.getResource();
            if (mr != null && mr.isValid() && !mr.isPermanent() && mr.isLocked()) {
                return true;
            }
        }
        return false;
    }

    public static void printSummary() {
        printSummary(true);
    }

    public static void printSummary(boolean printlocksources) {
        int numgone = 0;
        int numlocked = 0;
        int numpermanent = 0;
        int numinteresting = 0;
        int total = 0;
        HashMap<ResourcePool, ResourcePool> poolsSeen = new HashMap<ResourcePool, ResourcePool>();
        for (WeakLinkedList cur = resourceHead.next; cur != null; cur = cur.next) {
            ManagedResource mr = cur.getResource();
            total++;
            if (mr == null || !mr.isValid()) {
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
        System.err.println(total+" total resources being managed");
        System.err.println(numpermanent+" permanent resources ("+
                           Math.round(numpermanent * 1000.0 / total)/10.0+"%)");
        System.err.println(numlocked+" resources locked ("+
                           Math.round(numlocked * 1000.0 / total)/10.0+"%)");
        System.err.println(numinteresting+" resources contain interesting data ("+
                           Math.round(numinteresting * 1000.0 / total)/10.0+"%)");
        System.err.println(numgone+" resources disappeared ("+
                           Math.round(numgone * 1000.0 / total)/10.0+"%)");
        System.err.println();
    }

    protected T resource;
    private final ResourcePool<T> pool;
    private int lockcount;
    private int employcount;
    ArrayList<Throwable> lockedFrom;
    private boolean permanent;

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
        return resource != null;
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
        if (resource != null) {
            free();
            resource = null;
            unlink();
        }
    }

    public final void makePermanent() {
        assertLocked();
        permanent = true;
    }

    public final T lock() {
        lockcount++;
        if (trackLockSources && !permanent) {
            lockedFrom.add(new Throwable(Integer.toString(lockcount)));
        }
        return resource;
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
