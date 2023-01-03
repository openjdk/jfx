/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import javafx.collections.ObservableListBase;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.util.Callback;

public final class ElementObservableListDecorator<E> extends ObservableListBase<E> {

    private final ObservableList<E> decoratedList;
    private final ListChangeListener<E> listener;
    private ElementObserver<E> observer;


    public ElementObservableListDecorator(ObservableList<E> decorated,
            Callback<E, Observable[]> extractor) {
        this.observer = new ElementObserver<>(extractor, new Callback<E, InvalidationListener>() {

            @Override
            public InvalidationListener call(final E e) {
                return new InvalidationListener() {

                    @Override
                    public void invalidated(Observable observable) {
                        beginChange();
                        int i = 0;
                        if (decoratedList instanceof RandomAccess) {
                            final int size = size();
                            for (; i < size; ++i) {
                                if (get(i) == e) {
                                    nextUpdate(i);
                                }
                            }
                        } else {
                            for (Iterator<?> it = iterator(); it.hasNext();) {
                                if (it.next() == e) {
                                    nextUpdate(i);
                                }
                                ++i;
                            }
                        }
                        endChange();
                    }
                };
            }
        }, this);
        this.decoratedList = decorated;
        final int sz = decoratedList.size();
        for (int i = 0; i < sz; ++i) {
            observer.attachListener(decoratedList.get(i));
        }
        listener = new ListChangeListener<>() {

            @Override
            public void onChanged(Change<? extends E> c) {
                while (c.next()) {
                    if (c.wasAdded() || c.wasRemoved()) {
                        final int removedSize = c.getRemovedSize();
                        final List<? extends E> removed = c.getRemoved();
                        for (int i = 0; i < removedSize; ++i) {
                            observer.detachListener(removed.get(i));
                        }
                        if (decoratedList instanceof RandomAccess) {
                            final int to = c.getTo();
                            for (int i = c.getFrom(); i < to; ++i) {
                                observer.attachListener(decoratedList.get(i));
                            }
                        } else {
                            for (E e : c.getAddedSubList()) {
                                observer.attachListener(e);
                            }
                        }
                    }
                }
                c.reset();
                fireChange(c);
            }
        };
        this.decoratedList.addListener(new WeakListChangeListener<> (listener));
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return decoratedList.toArray(a);
    }

    @Override
    public Object[] toArray() {
        return decoratedList.toArray();
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return decoratedList.subList(fromIndex, toIndex);
    }

    @Override
    public int size() {
        return decoratedList.size();
    }

    @Override
    public E set(int index, E element) {
        return decoratedList.set(index, element);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return decoratedList.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return decoratedList.removeAll(c);
    }

    @Override
    public E remove(int index) {
        return decoratedList.remove(index);
    }

    @Override
    public boolean remove(Object o) {
        return decoratedList.remove(o);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return decoratedList.listIterator(index);
    }

    @Override
    public ListIterator<E> listIterator() {
        return decoratedList.listIterator();
    }

    @Override
    public int lastIndexOf(Object o) {
        return decoratedList.lastIndexOf(o);
    }

    @Override
    public Iterator<E> iterator() {
        return decoratedList.iterator();
    }

    @Override
    public boolean isEmpty() {
        return decoratedList.isEmpty();
    }

    @Override
    public int indexOf(Object o) {
        return decoratedList.indexOf(o);
    }

    @Override
    public E get(int index) {
        return decoratedList.get(index);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return decoratedList.containsAll(c);
    }

    @Override
    public boolean contains(Object o) {
        return decoratedList.contains(o);
    }

    @Override
    public void clear() {
        decoratedList.clear();
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        return decoratedList.addAll(index, c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return decoratedList.addAll(c);
    }

    @Override
    public void add(int index, E element) {
        decoratedList.add(index, element);
    }

    @Override
    public boolean add(E e) {
        return decoratedList.add(e);
    }

    @Override
    public boolean setAll(Collection<? extends E> col) {
        return decoratedList.setAll(col);
    }

    @Override
    public boolean setAll(E... elements) {
        return decoratedList.setAll(elements);
    }

    @Override
    public boolean retainAll(E... elements) {
        return decoratedList.retainAll(elements);
    }

    @Override
    public boolean removeAll(E... elements) {
        return decoratedList.removeAll(elements);
    }

    @Override
    public void remove(int from, int to) {
        decoratedList.remove(from, to);
    }

    @Override
    public boolean addAll(E... elements) {
        return decoratedList.addAll(elements);
    }

}
