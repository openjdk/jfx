/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.Collections;
import java.util.List;

/**
 * Often times there are multiple additions that are applied to a list, but they are not contiguous. This class
 * abstracts away the details and creates the right number of next() iterations to correctly report all changes.
 *
 * @param <E> The element contained in the underlying list data structure
 */
public class MultipleAdditionAndRemovedChange<E> extends ListChangeListener.Change<E> {

    private static final int[] EMPTY_PERM = new int[0];

    private boolean invalid = true;

    private final List<E> addedElements;
    private final List<E> removedElements;

    private boolean iteratingThroughAdded = true;
    private boolean returnedRemovedElements = false;
    private int addedIndex = 0;

    private int from;
    private int to;

    public MultipleAdditionAndRemovedChange(List<E> addedElements, List<E> removedElements, ObservableList<E> list) {
        super(list);
        this.addedElements = addedElements;
        this.removedElements = removedElements;
    }

    @Override public boolean next() {
        if (invalid) {
            invalid = false;
        }

        // we firstly work through all additions, finding contiguous results and grouping them together
        if (addedIndex < addedElements.size()) {
            from = getList().indexOf(addedElements.get(addedIndex));
            to = from + 1;

            addedIndex++;
            for (int i = 0; (i + addedIndex) < addedElements.size(); i++) {
                // check to see if the next element in the added list is also the next element in the actual list,
                // stop when this is no longer true.
                E nextElement = addedElements.get(i + addedIndex);
                if (nextElement != getList().get(from + i)) {
                    to = from + 1 + i;
                    addedIndex = addedIndex + i;
                    break;
                }
            }

            return true;
        } else if (!returnedRemovedElements) {
            returnedRemovedElements = true;
            iteratingThroughAdded = false;
            from = 0;
            to = 0;

            return !removedElements.isEmpty();
        }

        return false;
    }

    @Override public void reset() {
        invalid = true;
        from = 0;
        to = 0;
        addedIndex = 0;
        iteratingThroughAdded = true;
        returnedRemovedElements = false;
    }

    @Override public int getFrom() {
        checkState();
        return from;
    }

    @Override public int getTo() {
        checkState();
        return to;
    }

    @Override public List<E> getRemoved() {
        return iteratingThroughAdded ? Collections.<E>emptyList() : removedElements;
    }

    @Override protected int[] getPermutation() {
        return EMPTY_PERM;
    }

    @Override public boolean wasAdded() {
        return iteratingThroughAdded && !addedElements.isEmpty();
    }

    @Override public boolean wasRemoved() {
        return !iteratingThroughAdded && !removedElements.isEmpty();
    }

    private void checkState() {
        if (invalid) {
            throw new IllegalStateException("Invalid Change state: next() must be called before inspecting the Change.");
        }
    }
}
