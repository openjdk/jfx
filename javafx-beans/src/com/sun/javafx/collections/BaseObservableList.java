/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public abstract class BaseObservableList<E> extends AbstractList<E>  implements ObservableList<E> {

    private ListListenerHelper<E> listenerHelper;
    private final ListChangeBuilder<E> changeBuilder = new ListChangeBuilder<E>(this);

    protected final void nextUpdate(int pos) {
        changeBuilder.nextUpdate(pos);
    }

    protected final void nextSet(int idx, E old) {
        changeBuilder.nextSet(idx, old);
    }

    protected final void nextReplace(int from, int to, ArrayList removed) {
        changeBuilder.nextReplace(from, to, removed);
    }

    protected final void nextRemove(int idx, List removed) {
        changeBuilder.nextRemove(idx, removed);
    }

    protected final void nextRemove(int idx, E removed) {
        changeBuilder.nextRemove(idx, removed);
    }

    protected final void nextPermutation(int from, int to, int[] perm) {
        changeBuilder.nextPermutation(from, to, perm);
    }

    protected final void nextAdd(int from, int to) {
        changeBuilder.nextAdd(from, to);
    }

    protected final void endChange() {
        changeBuilder.endChange();
    }

    protected final void beginChange() {
        changeBuilder.beginChange();
    }

    @Override
    public final void addListener(InvalidationListener listener) {
        listenerHelper = ListListenerHelper.addListener(listenerHelper, listener);
    }

    @Override
    public final void removeListener(InvalidationListener listener) {
        listenerHelper = ListListenerHelper.removeListener(listenerHelper, listener);
    }

    @Override
    public final void addListener(ListChangeListener<? super E> listener) {
        listenerHelper = ListListenerHelper.addListener(listenerHelper, listener);
    }

    @Override
    public final void removeListener(ListChangeListener<? super E> listener) {
        listenerHelper = ListListenerHelper.removeListener(listenerHelper, listener);
    }

    protected final void callObservers(ListChangeListener.Change<? extends E> change) {
        ListListenerHelper.fireValueChangedEvent(listenerHelper, change);
    }

    protected final boolean hasListChangeListener() {
        return ListListenerHelper.hasListeners(listenerHelper);
    }

    @Override
    public boolean addAll(E... elements) {
        return addAll(Arrays.asList(elements));
    }

    @Override
    public boolean setAll(E... elements) {
        return setAll(Arrays.asList(elements));
    }

    @Override
    public boolean setAll(Collection<? extends E> col) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(E... elements) {
        return removeAll(Arrays.asList(elements));
    }

    @Override
    public boolean retainAll(E... elements) {
        return retainAll(Arrays.asList(elements));
    }

    @Override
    public void remove(int from, int to) {
        removeRange(from, to);
    }
}
