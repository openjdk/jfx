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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 *
 * @author martin
 */
public abstract class BaseModifiableObservableList<E> extends BaseObservableList<E> {

    @Override
    public boolean setAll(Collection<? extends E> col) {
        beginChange();
        try {
            clear();
            addAll(col);
        } finally {
            endChange();
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        beginChange();
        try {
            boolean res = super.addAll(c);
            return res;
        } finally {
            endChange();
        }
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        beginChange();
        try {
            boolean res = super.addAll(index, c);
            return res;
        } finally {
            endChange();
        }
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        beginChange();
        try {
            super.removeRange(fromIndex, toIndex);
        } finally {
            endChange();
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        beginChange();
        try {
            boolean res = super.removeAll(c);
            return res;
        } finally {
            endChange();
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        beginChange();
        try {
            boolean res = super.retainAll(c);
            return res;
        } finally {
            endChange();
        }
    }

    @Override
    public void add(int index, E element) {
        doAdd(index, element);
        beginChange();
        nextAdd(index, index + 1);
        ++modCount;
        endChange();
    }

    @Override
    public E set(int index, E element) {
        E old = doSet(index, element);
        beginChange();
        nextSet(index, old);
        endChange();
        return old;
    }

    @Override
    public boolean remove(Object o) {
        int i = indexOf(o);
        if (i != - 1) {
            remove(i);
            return true;
        }
        return false;
    }

    @Override
    public E remove(int index) {
        E old = doRemove(index);
        beginChange();
        nextRemove(index, old);
        ++modCount;
        endChange();
        return old;
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return new SubObservableList(super.subList(fromIndex, toIndex));
    }

    @Override
    public abstract E get(int index);

    @Override
    public abstract int size();

    protected abstract void doAdd(int index, E element);

    protected abstract E doSet(int index, E element);

    protected abstract E doRemove(int index);

    private class SubObservableList implements List<E> {

        public SubObservableList(List<E> sublist) {
            this.sublist = sublist;
        }
        private List<E> sublist;

        @Override
        public int size() {
            return sublist.size();
        }

        @Override
        public boolean isEmpty() {
            return sublist.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return sublist.contains(o);
        }

        @Override
        public Iterator<E> iterator() {
            return sublist.iterator();
        }

        @Override
        public Object[] toArray() {
            return sublist.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return sublist.toArray(a);
        }

        @Override
        public boolean add(E e) {
            return sublist.add(e);
        }

        @Override
        public boolean remove(Object o) {
            return sublist.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return sublist.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            beginChange();
            try {
                boolean res = sublist.addAll(c);
                return res;
            } finally {
                endChange();
            }
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> c) {
            beginChange();
            try {
                boolean res = sublist.addAll(index, c);
                return res;
            } finally {
                endChange();
            }
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            beginChange();
            try {
                boolean res = sublist.removeAll(c);
                return res;
            } finally {
                endChange();
            }
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            beginChange();
            try {
                boolean res = sublist.retainAll(c);
                return res;
            } finally {
                endChange();
            }
        }

        @Override
        public void clear() {
            beginChange();
            try {
                sublist.clear();
            } finally {
                endChange();
            }
        }

        @Override
        public E get(int index) {
            return sublist.get(index);
        }

        @Override
        public E set(int index, E element) {
            return sublist.set(index, element);
        }

        @Override
        public void add(int index, E element) {
            sublist.add(index, element);
        }

        @Override
        public E remove(int index) {
            return sublist.remove(index);
        }

        @Override
        public int indexOf(Object o) {
            return sublist.indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o) {
            return sublist.lastIndexOf(o);
        }

        @Override
        public ListIterator<E> listIterator() {
            return sublist.listIterator();
        }

        @Override
        public ListIterator<E> listIterator(int index) {
            return sublist.listIterator(index);
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            return new SubObservableList(sublist.subList(fromIndex, toIndex));
        }

        @Override
        public boolean equals(Object obj) {
            return sublist.equals(obj);
        }

        @Override
        public int hashCode() {
            return sublist.hashCode();
        }

        @Override
        public String toString() {
            return sublist.toString();
        }
    }
}
