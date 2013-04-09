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

/**
 * The base implementation of the {@link ResourcePool} interface, providing
 * bookkeeping for the {@link managed()} method and support for sharing
 * resources amongst multiple pools.
 */
public abstract class BaseResourcePool<T> implements ResourcePool<T> {
    long managedSize;
    final long maxSize;
    final ResourcePool sharedParent;

    protected BaseResourcePool() {
        this(null, Long.MAX_VALUE);
    }

    protected BaseResourcePool(long max) {
        this(null, max);
    }

    protected BaseResourcePool(ResourcePool parent) {
        this(parent, parent.max());
    }

    protected BaseResourcePool(ResourcePool parent, long max) {
        this.sharedParent = parent;
        this.maxSize = ((parent == null)
                        ? max
                        : Math.min(parent.max(), max));
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
        if (used() + size <= target()) return true;
        ManagedResource.cleanup(this, ManagedResource.PruningLevel.OBSOLETE);
        if (used() + size <= target()) return true;
        ManagedResource.cleanup(this, ManagedResource.PruningLevel.UNINTERESTING);
        if (used() + size <= target()) return true;
        ManagedResource.cleanup(this, ManagedResource.PruningLevel.ALL_UNLOCKED);
        return (used() + size <= max());
    }

    @Override
    public final void recordAllocated(long size) {
        managedSize += size;
    }

    @Override
    public final void resourceManaged(T resource) {
        recordAllocated(size(resource));
    }

    @Override
    public final void resourceFreed(T resource) {
        recordFree(size(resource));
    }

    @Override
    public final void recordFree(long size) {
        managedSize -= size;
        if (managedSize < 0) {
            throw new IllegalStateException("Negative resource amount");
        }
    }
}
