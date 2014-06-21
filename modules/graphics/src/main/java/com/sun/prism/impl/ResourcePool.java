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

/**
 * An interface for tracking the usage of a particular resource from which
 * managed resources are allocated.
 * A given resource pool may share its underlying space with another
 * resource pool, such as the case of allocating textures from the Java
 * heap.
 * In the case of a shared resource, the {@link used()} and {@link max()}
 * methods will return absolute values indicating the amounts used or
 * available for all resources that come from the shared pool, but the
 * {@link managed()} method will only indicate the amount of the resource
 * used for managed resources and the {@link target()} method will return
 * the amount of the total shared resource that the allocation methods
 * should attempt to stay under.
 * <pre>
 *     long newsize = ...; // size of new resource allocation
 *     if (pool.used() + newsize >= pool.target()) {
 *         cleanup(pool);
 *         // possibly multiple passes of increasing aggressiveness
 *     }
 *     if (pool.used() + newsize >= pool.max()) {
 *         throw OOM();
 *     }
 *     allocate(newsize);
 * </pre>
 * The amounts and sizes returned from the methods should all be in the
 * same units, usually bytes.
 * 
 * @see ManagedResource
 */
public interface ResourcePool<T> {

    /**
     * True if Thread.currentThread() is a thread that created this ResourcePool
     */
    public boolean isManagerThread();

    /**
     * The amount of a resource currently being used to hold any kind of
     * resource, whether managed or not.
     */
    public long used();

    /**
     * The amount of this resource currently being used to hold managed
     * resources.
     * This amount may be less than the amount returned by the {@link used()}
     * method if the pool is shared amongst other resources.
     */
    public long managed();

    /**
     * The total space available in this pool for allocating any kind of
     * resource, even unmanaged resources, and including those resources
     * already allocated.
     */
    public long max();

    /**
     * The target of the maximum amount of space in this resource pool that
     * should be used so as to be friendly to other parts of the system.
     * This number must be less than or equal to the amount returned by the
     * {@link max()} method.
     */
    public long target();

    /**
     * The estimated size of the indicated resource.
     * 
     * @param resource the resource to be measured
     * @return the space within this resource pool that the object occupies.
     */
    public long size(T resource);

    /**
     * Record the indicated amount of the resource as being allocated for
     * a {@link ManagedResource}.
     * 
     * @param size the amount of the resource to be indicated as managed.
     */
    public void recordAllocated(long size);

    /**
     * Record the indicated amount of the resource as no longer being
     * held in a {@link ManagedResource}.
     * 
     * @param size the amount of the resource to remove from the managed amount.
     */
    public void recordFree(long size);

    /**
     * Record the indicated amount of the resource as being allocated for
     * a {@link ManagedResource}.
     * 
     * @param resource the resource that is now being managed
     */
    public void resourceManaged(T resource);

    /**
     * Record the indicated amount of the resource as no longer being
     * held in a {@link ManagedResource}.
     * 
     * @param resource the resource that is freed, no longer being managed
     */
    public void resourceFreed(T resource);

    /**
     * Prepare for an allocation of a resource from this pool of the
     * indicated size by freeing up uninteresting resources until the
     * allocation fits within the target() or max() sizes.
     * 
     * @param size the size of the resource that is about to be allocated
     * @return true if there is room for the indicated resource
     */
    public boolean prepareForAllocation(long size);
}
