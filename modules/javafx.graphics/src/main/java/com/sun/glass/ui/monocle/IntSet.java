/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.monocle;

import java.util.Arrays;

/**
 * Mutable sorted set of int values, optimized for a small number of values. Not
 * thread-safe.
 */
class IntSet {
    private int[] elements = new int[4];
    private int size = 0;

    void addInt(int value) {
        int i = getIndex(value);
        if (i < 0) {
            int insertionPoint = -1 - i;
            if (size == elements.length) {
                elements = Arrays.copyOf(elements, size * 2);
            }
            if (insertionPoint != size) {
                System.arraycopy(elements, insertionPoint,
                                 elements, insertionPoint + 1,
                                 size - insertionPoint);
            }
            elements[insertionPoint] = value;
            size ++;
        }
    }

    void removeInt(int value) {
        int i = getIndex(value);
        if (i >= 0) {
            if (i < size - 1) {
                System.arraycopy(elements, i + 1, elements, i, size - i - 1);
            }
            size --;
        }
    }

    boolean containsInt(int value) {
        return getIndex(value) >= 0;
    }

    private int getIndex(int value) {
        int i;
        for (i = 0; i < size; i++) {
            if (elements[i] == value) {
                return i;
            } else if (elements[i] > value) {
                return -i - 1;
            }
        }
        return -i - 1;
    }

    int get(int index) {
        return elements[index];
    }

    /** Adds to the set "dest" values that in this set but are not in the set
     * "compared". */
    void difference(IntSet dest, IntSet compared) {
        int i = 0;
        int j = 0;
        while (i < size && j < compared.size) {
            int a = elements[i];
            int b = compared.elements[j];
            if (a < b) {
                // our set has a value that is not in "compared"
                dest.addInt(a);
                i ++;
            } else if (a > b) {
                // "compared" has a value that is not in our set.
                j ++;
            } else {
                // the sets match at this index.
                i ++;
                j ++;
            }
        }
        // anything left in our set is part of the delta and belongs in "dest".
        while (i < size) {
            dest.addInt(elements[i]);
            i ++;
        }
    }

    void clear() {
        size = 0;
    }

    int size() {
        return size;
    }

    boolean isEmpty() {
        return size == 0;
    }

    /** Copies the contents of this set to the target set. */
    void copyTo(IntSet target) {
        if (target.elements.length < size) {
            target.elements = Arrays.copyOf(elements, elements.length);
        } else {
            System.arraycopy(elements, 0, target.elements, 0, size);
        }
        target.size = size;
    }

    public boolean equals(IntSet set) {
        if (set.size == size) {
            for (int i = 0; i < size; i++) {
                if (set.elements[i] != elements[i]) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof IntSet) {
            return equals((IntSet) o);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int h = 1;
        for (int i = 0; i < size; i++) {
            h = 31 * h + elements[i];
        }
        return h;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("IntSet[");
        for (int i = 0; i < size; i++) {
            sb.append(elements[i]);
            if (i < size - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

}
