/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit;

/**
 * A collection of static methods for page cache management.
 */
public final class PageCache {

    /**
     * The private default constructor. Ensures non-instantiability.
     */
    private PageCache() {
        throw new AssertionError();
    }


    /**
     * Returns the capacity of the page cache.
     * @return the current capacity of the page cache, in pages.
     */
    public static int getCapacity() {
        return twkGetCapacity();
    }

    /**
     * Sets the capacity of the page cache.
     * @param capacity specifies the new capacity of the page cache, in pages.
     * @throws IllegalArgumentException if {@code capacity} is negative.
     */
    public static void setCapacity(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException(
                    "capacity is negative:" + capacity);
        }
        twkSetCapacity(capacity);
    }

    native private static int twkGetCapacity();
    native private static void twkSetCapacity(int capacity);
}
