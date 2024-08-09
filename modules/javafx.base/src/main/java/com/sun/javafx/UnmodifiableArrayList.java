/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx;

import java.util.AbstractList;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * An unmodifiable array-based List implementation. This is essentially like the
 * package private UnmodifiableRandomAccessList of the JDK, and helps us to
 * avoid having to do a lot of conversion work when we want to pass an array
 * into an unmodifiable list implementation (otherwise we would have to create
 * a temporary list that is then passed to Collections.unmodifiableList).
 */
public class UnmodifiableArrayList<T> extends AbstractList<T> implements RandomAccess {
    private T[] elements;
    private final int size;

    /**
     * The given elements are used directly (a defensive copy is not made),
     * and the given size is used as the size of this list. It is the callers
     * responsibility to make sure the size is accurate.
     *
     * @param elements    The elements to use.
     * @param size        The size must be <= the length of the elements array
     */
    public UnmodifiableArrayList(T[] elements, int size) {
        assert elements == null ? size == 0 : size <= elements.length;
        this.size = size;
        this.elements = elements;
    }

    @Override public T get(int index) {
        return elements[index];
    }

    @Override public int size() {
        return size;
    }

    /**
     * Converts the specified list into an unmodifiable list that does not contain {@code null} values.
     * The returned list is a copy of, and not a wrapper around the specified list.
     *
     * @param <T> the type of elements in the list
     * @param list the list, not {@code null}
     * @return an unmodifiable list that does not contain null values
     */
    public static <T> UnmodifiableArrayList<T> copyOfNullFiltered(List<T> list) {
        Objects.requireNonNull(list, "list cannot be null");

        int numNonNullValues = 0;

        @SuppressWarnings("unchecked")
        T[] newValues = (T[])new Object[list.size()];

        if (list instanceof RandomAccess) {
            // Prevents the iterator allocation for random-access lists.
            for (int i = 0, max = list.size(); i < max; ++i) {
                T value = list.get(i);
                if (value != null) {
                    newValues[numNonNullValues++] = value;
                }
            }
        } else {
            for (T value : list) {
                if (value != null) {
                    newValues[numNonNullValues++] = value;
                }
            }
        }

        return new UnmodifiableArrayList<>(newValues, numNonNullValues);
    }

    /**
     * Converts the specified array into an unmodifiable list that does not contain {@code null} values.
     * The returned list is a copy of, and not a wrapper around the specified array.
     *
     * @param <T> the type of elements in the array
     * @param elements the array, not {@code null}
     * @return an unmodifiable list that does not contain null values
     */
    public static <T> UnmodifiableArrayList<T> copyOfNullFiltered(T[] elements) {
        Objects.requireNonNull(elements, "elements cannot be null");

        int numNonNullValues = 0;

        @SuppressWarnings("unchecked")
        T[] newValues = (T[])new Object[elements.length];

        for (int i = 0; i < elements.length; ++i) {
            if (elements[i] != null) {
                newValues[numNonNullValues++] = elements[i];
            }
        }

        return new UnmodifiableArrayList<>(newValues, numNonNullValues);
    }
}
