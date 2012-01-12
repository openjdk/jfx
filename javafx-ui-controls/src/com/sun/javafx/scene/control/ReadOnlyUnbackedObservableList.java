/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

import com.sun.javafx.collections.ListInvalidationListenerWrapper;
import com.sun.javafx.collections.ListenerList;
import java.util.Collections;

/**
 * A read-only and unbacked ObservableList - the data is retrieved on demand by
 * subclasses via the get method. A combination of ObservableList, ObservableListWrapper
 * and GenericObservableList.
 *
 */
public abstract class ReadOnlyUnbackedObservableList<T> implements ObservableList<T> {

    private ListenerList<ListChangeListener<? super T>> observers;


    @Override public abstract T get(int i);

    @Override public abstract int size();


    @Override public void addListener(InvalidationListener listener) {
        addListener(new ListInvalidationListenerWrapper(this, listener));
    }

    @Override public void removeListener(InvalidationListener listener) {
        removeListener(new ListInvalidationListenerWrapper(this, listener));
    }

    @Override public void addListener(ListChangeListener<? super T> obs) {
        if (observers == null) {
            observers = new ListenerList<ListChangeListener<? super T>>();
            observers.add(obs);
        } else if (!observers.contains(obs)) {
            observers = observers.safeAdd(obs);
        }
    }

    @Override public void removeListener(ListChangeListener<? super T> obs) {
        if (observers != null) {
            observers = observers.safeRemove(obs);
            if (observers.isEmpty()) {
                observers = null;
            }
        }
    }

    public void callObservers(Change<T> c) {
        if (observers != null) {
            ListenerList<ListChangeListener<? super T>> obs = observers;
            obs.lock();
            try {
                for (int i = 0; i < obs.size(); ++i) {
                    c.reset();
                    obs.get(i).onChanged(c);
                }
            } finally {
                obs.unlock();
            }
        }
    }

    @Override public int indexOf(Object o) {
        if (o == null) return -1;

        for (int i = 0; i < size(); i++) {
            Object obj = get(i);
            if (o.equals(obj)) return i;
        }

        return -1;
    }

    @Override public int lastIndexOf(Object o) {
        if (o == null) return -1;

        for (int i = size() - 1; i >= 0; i--) {
            Object obj = get(i);
            if (o.equals(obj)) return i;
        }

        return -1;
    }

    @Override public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    @Override public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (! contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override public boolean isEmpty() {
        return size() == 0;
    }

    @Override public ListIterator<T> listIterator() {
        return new SelectionListIterator(this);
    }

    @Override public ListIterator<T> listIterator(int index) {
        return new SelectionListIterator(this, index);
    }

    @Override
    public Iterator<T> iterator() {
        return new SelectionListIterator(this);
    }

    /**
     * NOTE: This method does not fulfill the subList contract from Collections,
     * it simply returns a list containing the values in the given range.
     */
    @Override public List<T> subList(final int fromIndex, final int toIndex) {
        if (fromIndex >= toIndex) return Collections.emptyList();
        final List<T> outer = this;
        return new ReadOnlyUnbackedObservableList<T>() {
            @Override public T get(int i) {
                return outer.get(i + fromIndex);
            }

            @Override public int size() {
                return toIndex - fromIndex;
            }
        };
    }

    @Override
    public Object[] toArray() {
        Object[] arr = new Object[size()];
        for (int i = 0; i < size(); i++) {
            arr[i] = get(i);
        }
        return arr;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        T[] arr = a;
        if (arr.length < size()) {
            arr = (T[]) new Object[size()];
        }
        
        for (int i = 0; i < size(); i++) {
            arr[i] = (T) get(i);
        }
        
        return arr;
    }

    @Override
    public String toString() {
        // copied from AbstractCollection
        Iterator<T> i = iterator();
        if (! i.hasNext())
            return "[]";

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (;;) {
            T e = i.next();
            sb.append(e == this ? "(this Collection)" : e);
            if (! i.hasNext())
                return sb.append(']').toString();
            sb.append(", ");
        }
    }

    @Override public boolean add(T e) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override public void add(int index, T element) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override public boolean addAll(int index, Collection<? extends T> c) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override public boolean addAll(T... elements) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override public T set(int index, T element) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override public boolean setAll(Collection<? extends T> col) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override public boolean setAll(T... elements) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override public void clear() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override public T remove(int index) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override public boolean remove(Object o) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override public void remove(int from, int to) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override public boolean removeAll(T... elements) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override public boolean retainAll(T... elements) {
        throw new UnsupportedOperationException("Not supported.");
    }

    private static class SelectionListIterator<T> implements ListIterator<T> {
        private int pos;
        private final ReadOnlyUnbackedObservableList<T> list;

        public SelectionListIterator(ReadOnlyUnbackedObservableList<T> list) {
            this(list, 0);
        }

        public SelectionListIterator(ReadOnlyUnbackedObservableList<T> list, int pos) {
            this.list = list;
            this.pos = pos;
        }

        @Override public boolean hasNext() {
            return pos < list.size();
        }

        @Override public T next() {
            return list.get(pos++);
        }

        @Override public boolean hasPrevious() {
            return pos > 0;
        }

        @Override public T previous() {
            return list.get(pos--);
        }

        @Override public int nextIndex() {
            return pos + 1;
        }

        @Override public int previousIndex() {
            return pos - 1;
        }

        @Override public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override public void set(T e) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override public void add(T e) {
            throw new UnsupportedOperationException("Not supported.");
        }
    }
}
