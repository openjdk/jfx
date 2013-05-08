/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
