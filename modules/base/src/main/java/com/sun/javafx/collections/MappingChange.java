/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.AbstractList;
import java.util.List;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

public final class MappingChange<E, F> extends Change<F>{
    private final Map<E, F> map;
    private final Change<? extends E> original;
    private List<F> removed;

    public static final Map NOOP_MAP = new Map() {

        @Override
        public Object map(Object original) {
            return original;
        }
    };

    public static interface Map<E, F> {
        F map(E original);
    }

    public MappingChange(Change<? extends E> original, Map<E, F> map, ObservableList<F> list) {
        super(list);
        this.original = original;
        this.map = map;
    }

    @Override
    public boolean next() {
        return original.next();
    }

    @Override
    public void reset() {
        original.reset();
    }

    @Override
    public int getFrom() {
        return original.getFrom();
    }

    @Override
    public int getTo() {
        return original.getTo();
    }

    @Override
    public List<F> getRemoved() {
        if (removed == null) {
            removed = new AbstractList<F>() {

                @Override
                public F get(int index) {
                    return map.map(original.getRemoved().get(index));
                }

                @Override
                public int size() {
                    return original.getRemovedSize();
                }
            };
        }
        return removed;
    }

    @Override
    protected int[] getPermutation() {
        return new int[0];
    }

    @Override
    public boolean wasPermutated() {
        return original.wasPermutated();
    }

    @Override
    public boolean wasUpdated() {
        return original.wasUpdated();
    }

    @Override
    public int getPermutation(int i) {
        return original.getPermutation(i);
    }

    @Override
    public String toString() {
        // Get the current position. We don't want to store the current position explicitely,
        // just for the toString(), so we need to iterate the changes twice. This shouldn't
        // be an issue, given the average number of change sub-parts
        int posToEnd = 0;
        while (next()) {
            posToEnd++;
        }

        int size = 0;
        reset();
        while (next()) {
            size++;
        }
        reset();
        StringBuilder b = new StringBuilder();
        b.append("{ ");
        int pos = 0;
        while (next()) {
            if (wasPermutated()) {
                b.append(ChangeHelper.permChangeToString(getPermutation()));
            } else if (wasUpdated()) {
                b.append(ChangeHelper.updateChangeToString(getFrom(), getTo()));
            } else {
                b.append(ChangeHelper.addRemoveChangeToString(getFrom(), getTo(), getList(), getRemoved()));
            }
            if (pos != size) {
                b.append(", ");
            }
        }
        b.append(" }");

        reset();
        pos = size - posToEnd;
        while (pos-- > 0) {
            next();
        }

        return b.toString();
    }

}
