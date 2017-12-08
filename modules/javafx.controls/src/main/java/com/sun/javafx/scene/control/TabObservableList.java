/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.collections.NonIterableChange;
import com.sun.javafx.collections.ObservableListWrapper;
import javafx.scene.control.Tab;
import java.util.List;
import java.util.ListIterator;

/*
 * TabObservableList class extends ObservableListWrapper and
 * adds a method for reordering the list.
 */

public class TabObservableList<E> extends ObservableListWrapper<E> {
    private final List<E> tabList;

    public TabObservableList(List<E> list) {
        super(list);
        tabList = list;
    }

    public void reorder(Tab fromTab, Tab toTab) {
        if (!tabList.contains(fromTab) || !tabList.contains(toTab) || fromTab == toTab) {
            return;
        }
        // Perform reorder with the array of tabs.
        Object[] a = tabList.toArray();
        int fromIndex = tabList.indexOf(fromTab);
        int toIndex = tabList.indexOf(toTab);
        if (fromIndex == -1 || toIndex == -1) {
            return;
        }
        int direction = (toIndex - fromIndex) / Math.abs(toIndex - fromIndex);

        for (int j = fromIndex; j != toIndex; j += direction) {
            a[j] = a[j + direction];
        }
        a[toIndex] = fromTab;

        // Update the list with reordered array.
        ListIterator iter = tabList.listIterator();
        for (int j = 0; j < tabList.size(); j++) {
            iter.next();
            iter.set(a[j]);
        }

        // Update selected tab & index.
        fromTab.getTabPane().getSelectionModel().select(fromTab);

        // Fire permutation change event.
        int permSize = Math.abs(toIndex - fromIndex) + 1;
        int[] perm = new int[permSize];
        int from = direction > 0 ? fromIndex : toIndex;
        int to = direction < 0 ? fromIndex : toIndex;
        for (int i = 0; i < permSize; ++i) {
            perm[i] = i + from;
        }
        fireChange(new NonIterableChange.SimplePermutationChange<E>(from, to + 1, perm, this));
    }
}
