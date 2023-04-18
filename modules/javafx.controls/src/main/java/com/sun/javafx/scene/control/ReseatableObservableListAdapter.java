/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.collections.SourceAdapterChange;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import javafx.collections.WeakListChangeListener;
import java.util.Objects;

/**
 * Thin unmodifiable wrapper around an {@link ObservableList}.
 * <p>
 * The wrapped {@code ObservableList} can be changed at any time, allowing an instance of this
 * class to be used as a stand-in for different backing lists. Change notifications of the wrapped
 * backing list are forwarded through this list, and swapping out the backing list fires an
 * appropriate change notification on this list.
 */
public final class ReseatableObservableListAdapter<T> extends ObservableListBase<T> implements ListChangeListener<T> {

    private final WeakListChangeListener<T> weakListChangeListener = new WeakListChangeListener<>(this);
    private ObservableList<T> list;

    public void changeList(ObservableList<T> newList) {
        ObservableList<T> oldList = this.list;
        this.list = newList;

        beginChange();

        if (oldList != null) {
            oldList.removeListener(weakListChangeListener);
            nextRemove(0, oldList);
        }

        if (newList != null) {
            newList.addListener(weakListChangeListener);
            nextAdd(0, newList.size());
        }

        endChange();
    }

    @Override
    public void onChanged(Change<? extends T> c) {
        fireChange(new SourceAdapterChange<>(this, c));
    }

    @Override
    public T get(int index) {
        if (list == null) {
            throw new IndexOutOfBoundsException(index);
        }

        return list.get(index);
    }

    @Override
    public int size() {
        return list != null ? list.size() : 0;
    }

}
