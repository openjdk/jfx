/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Provides set implementations which have a fixed capacity. These are highly optimized
 * and only suitable for very specific use cases where the maximum size is known in advance
 * and when items are expected to never be removed from the sets. Fixed capacity sets will
 * throw {@link IllegalStateException} if adding an element would exceed the maximum capacity.
 *
 * <p>These sets do not allow {@code null} elements, and unless otherwise specified, passing
 * {@code null} for any argument will result in a {@link NullPointerException}.
 *
 * <p>Specifically, these sets are optimized for holding sets of style class names, which
 * have the following characteristics:
 *
 * <ul>
 * <li>Never {@code null}</li>
 * <li>Sets in almost all cases contain just 1 or 2 elements, only rarely containing 3 or more</li>
 * <li>Sets are often compared using containsAll; to avoid creating an iterator when the input is not
 *   a {@code FixedSizeSet}, the inverse function {@link #isSuperSetOf(Collection)} is provided</li>
 * </ul>
 *
 * The provided implementations are optimized for memory use, fast containsAll and
 * fast iteration. Generally, these sets will use half the memory of an equivalent
 * {@code HashSet} (and comparable to the immutable sets provided by {@code Set.of()})
 * as they do not require a wrapper to hold each element.
 *
 * <p>These sets can only be appended, reject {@code null}s, have a fixed maximum size (which will throw
 * an exception if exceeded), and can be frozen (made read-only without using a wrapper).
 *
 * <p>The fall back set implementation for large sets uses open addressing. It is
 * only lightly optimized as the expectation is that it will see little to no use.
 * It is still preferred over {@code HashSet} due to its low memory foot print, and
 * faster iteration.
 *
 * @param <T> the element type
 */
public sealed abstract class FixedCapacitySet<T> extends AbstractSet<T> {
    private static final FixedCapacitySet<?> EMPTY;

    static {
        EMPTY = new Single<>();

        EMPTY.freeze();
    }

    @SuppressWarnings("unchecked")
    private static <T> FixedCapacitySet<T> empty() {
        return (FixedCapacitySet<T>) EMPTY;
    }

    /**
     * Creates a new {@link FixedCapacitySet} with the given maximum capacity.
     * If the capacity is exceeded, fixed capacity sets do not grow, but instead
     * throw an {@link IllegalStateException}.
     *
     * @param <T> the element type
     * @param maximumCapacity the maximum possible number of elements the set can hold, cannot be negative
     * @return a new empty set, never {@code null}
     */
    public static <T> FixedCapacitySet<T> of(int maximumCapacity) {
        return maximumCapacity == 0 ? empty()
             : maximumCapacity == 1 ? new Single<>()
             : maximumCapacity == 2 ? new Duo<>()
             : maximumCapacity < 10 ? new Hashless<>(maximumCapacity)  // will reject negative values
                                    : new OpenAddressed<>(maximumCapacity);
    }

    private boolean frozen;

    /**
     * Checks if the given collection contains all elements
     * of this collection. This is the same as {@link #containsAll(Collection)}
     * with the source and target reversed, ie. {@code "a.containsAll(b)"} is equivalent
     * to {@code "b.isSuperSetOf(a)"}.
     *
     * <p>If the given collection is small, or has good {@link #contains(Object)}
     * performance, using this inverse function avoids creating an {@link Iterator}
     * for this collection.
     *
     * @param c a collection to check, cannot be {@code null}
     * @return {@code true} if the given collection contains all elements of this
     *   collection, otherwise {@code false}
     */
    public abstract boolean isSuperSetOf(Collection<?> c);

    /**
     * Freezes this collection, turning it permanently read-only. After freezing,
     * any method that would modify the collection will instead throw
     * {@link UnsupportedOperationException}.
     *
     * <p>This can be used to avoid wrapping the collection with an unmodifiable
     * collection or making a read-only copy.
     */
    public final void freeze() {
        this.frozen = true;
    }

    /**
     * Checks if the set is allowed to be mutated, and throws an
     * {@link UnsupportedOperationException} otherwise.
     */
    protected final void ensureNotFrozen() {
        if (frozen) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * A set that can hold 0 or 1 elements.
     *
     * @param <T> the element type
     */
    private static final class Single<T> extends FixedCapacitySet<T> {
        private T element;

        @Override
        public int size() {
            return element == null ? 0 : 1;
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<>() {
                private boolean hasNext = element != null;

                @Override
                public boolean hasNext() {
                    return hasNext;
                }

                @Override
                public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }

                    hasNext = false;

                    return element;
                }
            };
        }

        @Override
        public boolean isSuperSetOf(Collection<?> c) {
            return element == null || c.contains(element);
        }

        @Override
        public boolean contains(Object o) {
            return element != null && element.equals(o);
        }

        @Override
        public boolean add(T e) {
            ensureNotFrozen();

            if (contains(Objects.requireNonNull(e, "e"))) {
                return false;
            }

            if (element != null) {
                throw new IllegalStateException("set is full");
            }

            element = e;

            return true;
        }

        @Override
        public int hashCode() {
            return element == null ? 0 : element.hashCode();
        }
    }

    /**
     * A set that can hold 0, 1 or 2 elements.
     *
     * @param <T> the element type
     */
    private static final class Duo<T> extends FixedCapacitySet<T> {
        private T element1;
        private T element2;
        private int size;

        @Override
        public int size() {
            return size;
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<>() {
                private int index;

                @Override
                public boolean hasNext() {
                    return index < size;
                }

                @Override
                public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }

                    return index++ == 0 ? element1 : element2;
                }
            };
        }

        @Override
        public boolean isSuperSetOf(Collection<?> c) {
            return element1 == null || (c.contains(element1) && (element2 == null || c.contains(element2)));
        }

        @Override
        public boolean contains(Object o) {
            return (element1 != null && element1.equals(o)) || (element2 != null && element2.equals(o));
        }

        @Override
        public boolean add(T e) {
            ensureNotFrozen();

            if (contains(Objects.requireNonNull(e, "e"))) {
                return false;
            }

            if (size == 2) {
                throw new IllegalStateException("set is full");
            }

            if (size == 0) {
                element1 = e;
            }
            else {
                element2 = e;
            }

            size++;

            return true;
        }

        @Override
        public int hashCode() {
            return element1 == null ? 0 : element1.hashCode() + (element2 == null ? 0 : element2.hashCode());
        }
    }

    /**
     * A set which can hold a fixed maximum number of elements. This implementation
     * does not use hashing, but does eliminate duplicates (as per the set contract).
     * Performance is better than sets which use hashing when the number of elements
     * is small enough (cut off point is somewhere around 10 elements, but it will
     * depend on the cost of the hash function).
     *
     * @param <T> the element type
     */
    private static final class Hashless<T> extends FixedCapacitySet<T> {
        private final T[] elements;

        private int size;

        @SuppressWarnings("unchecked")
        private Hashless(int capacity) {
            this.elements = (T[]) new Object[capacity];
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<>() {
                private int index;

                @Override
                public boolean hasNext() {
                    return index < size;
                }

                @Override
                public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }

                    return elements[index++];
                }
            };
        }

        @Override
        public boolean contains(Object o) {
            for (int i = 0; i < size; i++) {
                if (elements[i].equals(o)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean isSuperSetOf(Collection<?> c) {
            for (int i = 0; i < size; i++) {
                if (!c.contains(elements[i])) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public boolean add(T e) {
            ensureNotFrozen();

            if (contains(Objects.requireNonNull(e, "e"))) {
                return false; // already present, set unchanged
            }

            if (size == elements.length) {
                throw new IllegalStateException("set is full");
            }

            elements[size++] = e;

            return true; // not present, set changed
        }

        @Override
        public int hashCode() {
            int h = 0;

            for (int i = 0; i < size; i++) {
                h += elements[i].hashCode();
            }

            return h;
        }
    }

    /**
     * A set which can hold a fixed maximum number of elements. This implementation
     * uses open addressing to handle hash collisions using linear probing. It has a
     * memory footprint which is much smaller than an equivalent {@code HashSet} but
     * has worse worst case performance (for modification and contains) when there
     * are many collisions. Iteration speed will be similar to other array based
     * collections (which is to say, faster than {@code HashSet}).
     *
     * @param <T> the element type
     */
    private static final class OpenAddressed<T> extends FixedCapacitySet<T> {
        private final T[] elements;
        private final int requestedCapacity;
        private final int mask;

        private int size;

        @SuppressWarnings("unchecked")
        private OpenAddressed(int capacity) {
            this.requestedCapacity = capacity;

            int shift = Integer.SIZE - Integer.numberOfLeadingZeros(capacity * 2 - capacity / 2);

            /*
             * The shift calculated ensures the elements array's size will be a power
             * of 2, and ensures that the load factor of this hash map will be roughly
             * between 0.3 and 0.7; high load factors are detrimental to performance,
             * while low load factors will consume more memory than necessary.
             */

            this.elements = (T[]) new Object[1 << shift];
            this.mask = (1 << shift) - 1;

            /*
             * Note: the size of the elements array MUST always be greater than the
             * requested capacity as the contains check relies on there always being
             * at least one empty bucket.
             */

            assert elements.length > requestedCapacity : "must have more buckets than capacity";
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<>() {
                private int index = findFilledBucket(0);

                private int findFilledBucket(int start) {
                    for (int i = start; i < elements.length; i++) {
                        if (elements[i] != null) {
                            return i;
                        }
                    }

                    return -1;
                }

                @Override
                public boolean hasNext() {
                    return index >= 0;
                }

                @Override
                public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }

                    T element = elements[index];

                    index = findFilledBucket(index + 1);

                    return element;
                }
            };
        }

        @Override
        public boolean contains(Object o) {
            int bucket = determineBucketIndex(o);

            /*
             * Note: because the open addressed set always has spare capacity
             * this loop will always exit because an unused bucket is encountered
             * at some point.
             */

            while (elements[bucket] != null) {
                if (elements[bucket].equals(o)) {
                    return true;
                }

                bucket++;  // linear probing for simplicity

                if (bucket >= elements.length) {
                    bucket = 0;
                }
            }

            return false;  // empty bucket encountered, not contained
        }

        @Override
        public boolean isSuperSetOf(Collection<?> c) {
            for (int i = 0; i < elements.length; i++) {
                T element = elements[i];

                if (element != null && !c.contains(element)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public boolean add(T e) {
            ensureNotFrozen();

            int bucket = determineBucketIndex(e);  // implicit null check here

            while (elements[bucket] != null) {
                if (elements[bucket].equals(e)) {
                    return false; // already present, set unchanged
                }

                bucket++;  // linear probing for simplicity

                if (bucket >= elements.length) {
                    bucket = 0;  // there is no risk of this becoming an infinite loop as there is always spare capacity
                }
            }

            if (size == requestedCapacity) {  // this check is "late" so that adding the same element to an already "full" set will correctly return "false"
                throw new IllegalStateException("set is full");
            }

            elements[bucket] = e;
            size++;

            return true; // not present, set changed
        }

        @Override
        public int hashCode() {
            int h = 0;

            for (int i = 0; i < elements.length; i++) {
                T element = elements[i];

                h += element == null ? 0 : element.hashCode();
            }

            return h;
        }

        private int determineBucketIndex(Object o) {
            int h = o.hashCode();

            return (h ^ (h >>> 16)) & mask;  // inspired by HashMap
        }
    }
}
