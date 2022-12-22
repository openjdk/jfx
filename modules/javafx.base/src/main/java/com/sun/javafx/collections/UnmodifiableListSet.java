/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.collections;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;

/**
 * A special unmodifiable implementation of Set which wraps a List.
 * <strong>It does not check for uniqueness!</strong> There are
 * several places in our implementation (Node.lookupAll and
 * ObservableSetWrapper are two such places) where we want to use
 * a List for speed of insertion and will be in a position to ensure
 * that the List is unique without having the overhead of hashing,
 * but want to present an unmodifiable Set in the public API.
 */
public final class UnmodifiableListSet<E> extends AbstractSet<E> {
    private List<E> backingList;

    public UnmodifiableListSet(List<E> backingList) {
        if (backingList == null) throw new NullPointerException();
        this.backingList = backingList;
    }

    /**
     * Required implementation that returns an iterator. Note that I
     * don't just return backingList.iterator() because doing so would
     * open up a whole through which developers could remove items from
     * this supposedly unmodifiable set. So the iterator is wrapped
     * such that it throws an exception on remove.
     */
    @Override public Iterator<E> iterator() {
        final Iterator<E> itr = backingList.iterator();
        return new Iterator<>() {
            @Override public boolean hasNext() {
                return itr.hasNext();
            }

            @Override public E next() {
                return itr.next();
            }

            @Override public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override public int size() {
        return backingList.size();
    }
}
