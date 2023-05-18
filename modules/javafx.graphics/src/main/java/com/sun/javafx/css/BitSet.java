/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.collections.SetListenerHelper;

import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

/**
 * Pseudo-class state and style-classes are represented as bits in a long[]
 * which makes matching faster.
 */
abstract class BitSet<T> extends AbstractSet<T> implements ObservableSet<T> {

    /** Create an empty set of T */
    protected BitSet () {
        this.bits = EMPTY_SET;
    }


    /** {@inheritDoc} */
    @Override
    public int size() {

        int size = 0;
        if (bits.length > 0) {
            for (int n = 0; n < bits.length; n++) {
                final long mask = bits[n];
                if (mask != 0) {
                    size += Long.bitCount(mask);
                }
            }
        }
        // index.length is zero or all index[n] values are zero
        return size;

    }

    @Override
    public boolean isEmpty() {

        if (bits.length > 0) {
            for (int n = 0; n < bits.length; n++) {
                final long mask = bits[n];
                if (mask != 0) {
                    return false;
                }
            }
        }
        // index.length is zero or all index[n] values are zero
        return true;

    }

    /**
     * {@inheritDoc} This returned iterator is not fail-fast.
     */
    @Override
    public Iterator<T> iterator() {

        return new Iterator<>() {
            int next = -1;
            int element = 0;
            int index = -1;

            @Override
            public boolean hasNext() {
                if (bits == null || bits.length == 0) {
                    return false;
                }

                boolean found = false;

                do {
                    if (++next >= Long.SIZE) {
                        if (++element < bits.length) {
                            next = 0;
                        } else {
                            return false;
                        }
                    }

                    long bit = 1l << next;
                    found = (bit & bits[element]) == bit;

                } while( !found );

                if (found) {
                    index = Long.SIZE * element + next;
                }
                return found;
            }

            @Override
            public T next() {
                try {
                    return getT(index);
                } catch (IndexOutOfBoundsException e) {
                    throw new NoSuchElementException("["+element+"]["+next+"]");
                }
            }

            @Override
            public void remove() {
                try {
                    T t = getT(index);
                    BitSet.this.remove(t);
                } catch (IndexOutOfBoundsException e) {
                    throw new NoSuchElementException("["+element+"]["+next+"]");
                }
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public boolean add(T t) {

        if (t == null) {
            // this not modified!
            return false;
        }

        final int element = getIndex(t) / Long.SIZE;
        final long bit = 1l << (getIndex(t) % Long.SIZE);

        // need to grow?
        if (element >= bits.length) {
            final long[] temp = new long[element + 1];
            System.arraycopy(bits, 0, temp, 0, bits.length);
            bits = temp;
        }

        final long temp = bits[element];
        bits[element] = temp | bit;

        // if index[element] == temp, then the bit was already set
        final boolean modified = (bits[element] != temp);
        if (modified && SetListenerHelper.hasListeners(listenerHelper)){
            notifyObservers(t, Change.ELEMENT_ADDED);
        }
        return modified;
    }

    /** {@inheritDoc} */
    @Override
    public boolean remove(Object o) {

        if (o == null) {
            // this not modified!
            return false;
        }

        T t = cast(o);

        final int element = getIndex(t) / Long.SIZE;
        final long bit = 1l << (getIndex(t) % Long.SIZE);

        if (element >= bits.length) {
            // not in this Set!
            return false;
        }

        final long temp = bits[element];
        bits[element] = temp & ~bit;

        // if index[element] == temp, then the bit was not there
        final boolean modified = (bits[element] != temp);
        if (modified) {
            if (SetListenerHelper.hasListeners(listenerHelper)) {
                notifyObservers(t, Change.ELEMENT_REMOVED);
            }

            // did removing the bit leave an empty set?
            boolean isEmpty = true;
            for (int n=0; n<bits.length && isEmpty; n++) {
                isEmpty &= bits[n] == 0;
            }
            if (isEmpty) bits = EMPTY_SET;
        }
        return modified;
    }


    /** {@inheritDoc} */
    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }

        final T t = cast(o);

        final int element = getIndex(t) / Long.SIZE;
        final long bit = 1l << (getIndex(t) % Long.SIZE);

        return (element < bits.length) && (bits[element] & bit) == bit;
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsAll(Collection<?> c) {
        if (this.getClass() != c.getClass()) {
            for (Object obj : c) {
                if (!contains(obj)) {
                    return false;
                }
            }

            return true;
        }

        BitSet other = (BitSet)c;

        // this contains all of other if both are empty
        if (bits.length == 0 && other.bits.length == 0) {
            return true;
        }
        // [foo] cannot contain all of [foo bar]
        if (bits.length < other.bits.length) {
            return false;
        }
        // does [foo bar bang] contain all of [foo bar]?
        for (int n = 0, max = other.bits.length; n < max; n++) {
            if ((bits[n] & other.bits[n]) != other.bits[n]) {
                return false;
            }
        }
        return true;
    }


    /** {@inheritDoc} */
    @Override
    public boolean addAll(Collection<? extends T> c) {
        if (this.getClass() != c.getClass()) {
            boolean modified = false;

            for (T obj : c) {
                modified |= add(obj);
            }

            return modified;
        }

        boolean modified = false;

        BitSet other = (BitSet)c;

        final long[] maskOne = this.bits;
        final long[] maskTwo = other.bits;

        final int a = maskOne.length;
        final int b = maskTwo.length;
        // Math.max(maskOne.length, maskTwo.length) is too slow
        final int max = a < b ? b : a;

        final long[] union = max > 0 ? new long[max] : EMPTY_SET;

        for(int n = 0; n < max; n++) {

            if (n < maskOne.length && n < maskTwo.length) {
                union[n] = maskOne[n] | maskTwo[n];
                modified |= (union[n] != maskOne[n]);
            } else if (n < maskOne.length) {
                union[n] = maskOne[n];
                modified |= false;
            } else {
                union[n] = maskTwo[n];
                modified = true;
            }

        }

        if (modified) {

            if (SetListenerHelper.hasListeners(listenerHelper)) {

                for (int n = 0; n < max; n++) {

                    long bitsAdded = 0l;

                    if (n < maskOne.length && n < maskTwo.length) {
                        bitsAdded = ~maskOne[n] & maskTwo[n];
                    } else if (n < maskOne.length) {
                        // union[n] = maskOne[n], so no bits added
                        continue;
                    } else {
                        bitsAdded = maskTwo[n];
                    }

                    for(int bit = 0; bit < Long.SIZE; bit++) {
                        long m = 1l << bit;
                        if ((m & bitsAdded) == m) {
                            T t = getT(n*Long.SIZE + bit);
                            notifyObservers(t, Change.ELEMENT_ADDED);
                        }
                    }
                }
            }

            this.bits = union;
        }

        return modified;

    }

    /** {@inheritDoc} */
    @Override
    public boolean retainAll(Collection<?> c) {
        if (this.getClass() != c.getClass()) {
            boolean modified = false;

            for (Iterator<T> iterator = this.iterator(); iterator.hasNext();) {
                T obj = iterator.next();

                if (!c.contains(obj)) {
                    iterator.remove();
                    modified = true;
                }
            }

            return modified;
        }

        boolean modified = false;

        BitSet other = (BitSet)c;

        final long[] maskOne = this.bits;
        final long[] maskTwo = other.bits;

        final int a = maskOne.length;
        final int b = maskTwo.length;
        // Math.min(maskOne.length, maskTwo.length) is too slow
        final int max = a < b ? a : b;

        final long[] intersection = max > 0 ? new long[max] : EMPTY_SET;

        //
        // Make sure modified is set if maskOne has more bits than maskTwo.
        // If max is zero, then the loop that does the intersection is
        // never entered (since maskTwo is empty). If modified isn't set,
        // then the if (modified) block isn't entered and this.bits isn't
        // set to the intersection.
        //
        modified |= (maskOne.length > max);

        //
        // RT-32872 - If the intersection is empty, then set bits to the EMPTY_SET.
        // This ensures that {a,b,c}.retainAll({}) yields bits = EMPTY_SET as
        // opposed to bits = long[1] and bits[0] == 0.
        //
        // Assume isEmpty is true. If any intersection[n] != 0,
        // isEmpty will be set to false and will remain false.
        //
        //
        boolean isEmpty = true;

        for(int n = 0; n < max; n++) {
            intersection[n] = maskOne[n] & maskTwo[n];
            modified |= intersection[n] != maskOne[n];
            isEmpty &= intersection[n] == 0;
        }

        if (modified) {

            if (SetListenerHelper.hasListeners(listenerHelper)) {

                for (int n = 0; n < maskOne.length; n++) {

                    long bitsRemoved = 0l;

                    if (n < maskTwo.length) {
                        bitsRemoved = maskOne[n] & ~maskTwo[n];
                    } else {
                        // maskTwo was shorter than maskOne,
                        // and remaining bits in maskOne (which is this.bits) were removed
                        bitsRemoved = maskOne[n];
                    }

                    for(int bit = 0; bit < Long.SIZE; bit++) {
                        long m = 1l << bit;
                        if ((m & bitsRemoved) == m) {
                            T t = getT(n*Long.SIZE + bit);
                            notifyObservers(t, Change.ELEMENT_REMOVED);
                        }
                    }
                }
            }

            this.bits = isEmpty == false ? intersection : EMPTY_SET;
        }

        return modified;
    }


    /** {@inheritDoc} */
    @Override
    public boolean removeAll(Collection<?> c) {
        if (this.getClass() != c.getClass()) {
            boolean modified = false;

            for (Object obj : c) {
                modified |= remove(obj);
            }

            return modified;
        }

        boolean modified = false;

        BitSet other = (BitSet)c;

        final long[] maskOne = bits;
        final long[] maskTwo = other.bits;

        final int a = maskOne.length;
        final int b = maskTwo.length;
        // Math.min(maskOne.length, maskTwo.length) is too slow
        final int max = a < b ? a : b;

        final long[] difference = max > 0 ? new long[max] : EMPTY_SET;

        //
        // RT-32872 - If the intersection is empty, then set bits to the EMPTY_SET.
        // This ensures that {a,b,c}.retainAll({}) yields bits = EMPTY_SET as
        // opposed to bits = long[1] and bits[0] == 0.
        //
        // Assume isEmpty is true. If any difference[n] != 0,
        // isEmpty will be set to false and will remain false.
        //
        //
        boolean isEmpty = true;

        for(int n = 0; n < max; n++) {
            difference[n] = maskOne[n] & ~maskTwo[n];
            modified |= difference[n] != maskOne[n];
            isEmpty &= difference[n] == 0;
        }

        if (modified) {

            if (SetListenerHelper.hasListeners(listenerHelper)) {

                for (int n = 0; n < max; n++) {

                    long bitsRemoved = maskOne[n] & maskTwo[n];

                    for(int bit = 0; bit < Long.SIZE; bit++) {
                        long m = 1l << bit;
                        if ((m & bitsRemoved) == m) {
                            T t = getT(n*Long.SIZE + bit);
                            notifyObservers(t, Change.ELEMENT_REMOVED);
                        }
                    }
                }
            }

            this.bits = isEmpty == false ? difference : EMPTY_SET;
        }

        return modified;
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {

        for (int n = 0; n < bits.length; n++) {

            long bitsRemoved = bits[n];

            for(int b = 0; b < Long.SIZE; b++) {
                long m = 1l << b;
                if ((m & bitsRemoved) == m) {
                    T t = getT(n*Long.SIZE + b);
                    notifyObservers(t, Change.ELEMENT_REMOVED);
                }
            }
        }

        bits = EMPTY_SET;
    }

    @Override
    public int hashCode() {
        // Note: overridden because equals is overridden; both equals and hashCode MUST
        // respect the Set contract to interact correctly with sets of other types!
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        // Note: overridden to provide a fast path; both equals and hashCode MUST respect
        // the Set contract to interact correctly with sets of other types!
        if (obj == this) {
            return true;
        }
        if (getClass() == obj.getClass()) {  // fast path if other is exact same type of BitSet
            return equalsBitSet((BitSet<?>) obj);
        }

        return super.equals(obj);
    }

    private boolean equalsBitSet(BitSet<?> other) {
        int a = this.bits != null ? this.bits.length : 0;
        int b = other.bits != null ? other.bits.length : 0;

        if (a != b) return false;

        for(int m=0; m<a; m++) {
            final long m0 = this.bits[m];
            final long m1 = other.bits[m];
            if (m0 != m1) {
                return false;
            }
        }
        return true;
    }

    abstract protected T getT(int index);
    abstract protected int getIndex(T t);

    /*
     * Try to cast the arg to a T.
     * @throws ClassCastException if the class of the argument is
     *         is not a T
     * @throws NullPointerException if the argument is null
     */
    abstract protected T cast(Object o);

    protected long[] getBits() {
        return bits;
    }

    private static final long[] EMPTY_SET = new long[0];

    // the set
    private long[] bits;

    private SetListenerHelper<T> listenerHelper;

    private class Change extends SetChangeListener.Change<T> {

        private static final boolean ELEMENT_ADDED = false;
        private static final boolean ELEMENT_REMOVED = true;

        private final T element;
        private final boolean removed;

        public Change(T element, boolean removed) {
            super(FXCollections.unmodifiableObservableSet(BitSet.this));
            this.element = element;
            this.removed = removed;
        }

        @Override
        public boolean wasAdded() {
            return removed != ELEMENT_REMOVED;
        }

        @Override
        public boolean wasRemoved() {
            return removed;
        }

        @Override
        public T getElementAdded() {
            return removed ? null : element;
        }

        @Override
        public T getElementRemoved() {
            return removed ? element : null;
        }

    }

    @Override
    public void addListener(SetChangeListener<? super T> setChangeListener) {
        if (setChangeListener != null) {
            listenerHelper = SetListenerHelper.addListener(listenerHelper, setChangeListener);
        }
    }

    @Override
    public void removeListener(SetChangeListener<? super T> setChangeListener) {
        if (setChangeListener != null) {
            listenerHelper = SetListenerHelper.removeListener(listenerHelper, setChangeListener);
        }
    }

    @Override
    public void addListener(InvalidationListener invalidationListener) {
        if (invalidationListener != null) {
            listenerHelper = SetListenerHelper.addListener(listenerHelper, invalidationListener);
        }
    }

    @Override
    public void removeListener(InvalidationListener invalidationListener) {
        if (invalidationListener != null) {
            listenerHelper = SetListenerHelper.removeListener(listenerHelper, invalidationListener);
        }
    }

    private void notifyObservers(T element, boolean removed) {
        if (element != null && SetListenerHelper.hasListeners(listenerHelper)) {
            Change change = new Change(element, removed);
            SetListenerHelper.fireValueChangedEvent(listenerHelper, change);
        }
    }
}
