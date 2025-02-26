/*
 * Copyright (c) 2010, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public abstract class VetoableListDecorator<E> implements ObservableList<E> {

    private final ObservableList<E> list;
    private int modCount;
    private ListListenerHelper<E> helper;

    private static interface ModCountAccessor {
        public int get();
        public int incrementAndGet();
        public int decrementAndGet();
    }

    /**
     * The type of the change can be observed from the combination of arguments.
     * <ul>
     * <li> If something is going to be added <code>toBeAdded</code> is non-empty
     *      and <code>indexes</code> contain two indexes that are pointing to the position, e.g. {2, 2}
     * <li> If something is going to be removed, the <code>indexes</code> are paired by two:
     *      from(inclusive)-to(exclusive) and are pointing to the current list.
     *      <br> E.g. if we remove 2,3,5 from list {0,1,2,3,4,5}, the indexes
     *      will be {2, 4, 5, 6}. If there's more than one pair of indexes, <code>toBeAdded</code> is always empty.
     * <li> for set <code>toBeAdded</code> contains 1 element and <code>indexes</code> are like with removal: {index, index + 1}
     * <li> for setAll, <code>toBeAdded</code> contains all new elements and <code>indexes</code> looks like this: {0, size()}
     * </ul>
     *
     * Note that it's always safe to iterate over toBeAdded and use indexes as pairs of
     * from-to, as there's always at least one pair.
     *
     * @param toBeAdded the list to be added
     * @throws IllegalArgumentException when the change is vetoed
     */
    protected abstract void onProposedChange(List<E> toBeAdded, int... indexes);

    public VetoableListDecorator(ObservableList<E> decorated) {
        this.list = decorated;
        this.list.addListener((ListChangeListener.Change<? extends E> c) -> {
            ListListenerHelper.fireValueChangedEvent(helper,
                    new SourceAdapterChange<>(VetoableListDecorator.this, c));
        });
    }

    @Override
    public void addListener(ListChangeListener<? super E> listener) {
        helper = ListListenerHelper.addListener(helper, listener);
    }

    @Override
    public void removeListener(ListChangeListener<? super E> listener) {
        helper = ListListenerHelper.removeListener(helper, listener);
    }

    @Override
    public void addListener(InvalidationListener listener) {
        helper = ListListenerHelper.addListener(helper, listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        helper = ListListenerHelper.removeListener(helper, listener);
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
        List<E> elements = unmodifiableList(col);
        onProposedChange(elements, 0, size());
        try {
            modCount++;
            return list.setAll(elements);
        } catch(Exception e) {
            modCount--;
            throw e;
        }
    }

    private void removeFromList(List<E> backingList, int offset, Collection<?> col, boolean complement) {
        int[] toBeRemoved = new int[2];
        int pointer = -1;
        for (int i = 0; i < backingList.size(); ++i) {
            final E el = backingList.get(i);
            if (col.contains(el) ^ complement) {
                if (pointer == -1) {
                    toBeRemoved[pointer + 1] = offset + i;
                    toBeRemoved[pointer + 2] = offset + i + 1;
                    pointer += 2;
                } else {
                    if (toBeRemoved[pointer] == offset + i) {
                        toBeRemoved[pointer] = offset + i + 1;
                    } else {
                        int[] tmp = new int[toBeRemoved.length + 2];
                        System.arraycopy(toBeRemoved, 0, tmp, 0, toBeRemoved.length);
                        toBeRemoved = tmp;
                        toBeRemoved[pointer + 1] = offset + i;
                        toBeRemoved[pointer + 2] = offset + i + 1;
                        pointer += 2;
                    }
                }
            }
        }
        if (pointer != -1) {
            onProposedChange(Collections.<E>emptyList(), toBeRemoved);
        }
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
        Objects.checkFromToIndex(from, to, size());
        onProposedChange(Collections.<E>emptyList(), from, to);
        try {
            modCount++;
            list.remove(from, to);
        } catch (Exception e) {
            modCount--;
        }
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return new VetoableIteratorDecorator(this, new ModCountAccessorImpl(), list.iterator(), 0);
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return list.toArray(a);
    }

    @Override
    public boolean add(E e) {
        onProposedChange(Collections.singletonList(e), size(), size());
        try {
            modCount++;
            list.add(e);
            return true;
        } catch (Exception ex) {
            modCount--;
            throw ex;
        }
    }

    @Override
    public boolean remove(Object o) {
        int i = list.indexOf(o);
        if (i != - 1) {
            remove(i);
            return true;
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        List<E> elements = unmodifiableList(c);
        onProposedChange(elements, size(), size());
        try {
            modCount++;
            boolean ret = list.addAll(elements);
            if (!ret)
                modCount--;
            return ret;
        } catch (Exception e) {
            modCount--;
            throw e;
        }
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        Objects.checkIndex(index, size() + 1);
        List<E> elements = unmodifiableList(c);
        onProposedChange(elements, index, index);
        try {
            modCount++;
            boolean ret = list.addAll(index, elements);
            if (!ret)
                modCount--;
            return ret;
        } catch (Exception e) {
            modCount--;
            throw e;
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        Collection<?> elements = safeCollection(c);
        removeFromList(this, 0, elements, false);
        try {
            modCount++;
            boolean ret = list.removeAll(elements);
            if (!ret)
                modCount--;
            return ret;
        } catch (Exception e) {
            modCount--;
            throw e;
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        Collection<?> elements = safeCollection(c);
        removeFromList(this, 0, elements, true);
        try {
            modCount++;
            boolean ret = list.retainAll(elements);
            if (!ret)
                modCount--;
            return ret;
        } catch (Exception e) {
            modCount--;
            throw e;
        }
    }

    @Override
    public void clear() {
        onProposedChange(Collections.<E>emptyList(), 0, size());
        try {
            modCount++;
            list.clear();
        } catch (Exception e) {
            modCount--;
            throw e;
        }
    }

    @Override
    public E get(int index) {
        return list.get(index);
    }

    @Override
    public E set(int index, E element) {
        onProposedChange(Collections.singletonList(element), index, index + 1);
        return list.set(index, element);
    }

    @Override
    public void add(int index, E element) {
        Objects.checkIndex(index, size() + 1);
        onProposedChange(Collections.singletonList(element), index, index);
        try {
            modCount++;
            list.add(index, element);
        } catch (Exception e) {
            modCount--;
            throw e;
        }
    }

    @Override
    public E remove(int index) {
        Objects.checkIndex(index, size());
        onProposedChange(Collections.<E>emptyList(), index, index + 1);
        try {
            modCount++;
            E ret = list.remove(index);
            return ret;
        } catch (Exception e) {
            modCount--;
            throw e;
        }
    }

    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator() {
        return new VetoableListIteratorDecorator(this, new ModCountAccessorImpl(), list.listIterator(), 0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return new VetoableListIteratorDecorator(this, new ModCountAccessorImpl(), list.listIterator(index), index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return new VetoableSubListDecorator(this, new ModCountAccessorImpl(), list.subList(fromIndex, toIndex), fromIndex);
    }

    @Override
    public String toString() {
        return list.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return list.equals(obj);
    }

    @Override
    public int hashCode() {
        return list.hashCode();
    }

    /**
     * Returns the specified collection as an unmodifiable list that can safely be used in all bulk
     * operations without triggering {@link ConcurrentModificationException}.
     */
    private <T> List<T> unmodifiableList(Collection<? extends T> c) {
        Objects.requireNonNull(c);
        return !(c instanceof List<?>) || (c instanceof VetoableSubListDecorator<?> d && d.parent == this)
            ? Collections.unmodifiableList(new ArrayList<>(c))
            : Collections.unmodifiableList((List<T>)c);
    }

    /**
     * Returns a collection that can safely be used in the {@link #removeAll(Collection)} and
     * {@link #retainAll(Collection)} operations without triggering {@link ConcurrentModificationException}.
     */
    private <T> Collection<T> safeCollection(Collection<T> c) {
        Objects.requireNonNull(c);
        return c instanceof VetoableSubListDecorator<?> d && d.parent == this
            ? (List<T>)Arrays.asList(c.toArray())
            : c;
    }

    private static class VetoableSubListDecorator<E> implements List<E> {
        private final VetoableListDecorator parent;
        private final List<E> subList;
        private final int offset;
        private final ModCountAccessor modCountAccessor;
        private int modCount;

        public VetoableSubListDecorator(VetoableListDecorator<E> parent, ModCountAccessor modCountAccessor, List<E> subList, int offset) {
            this.parent = parent;
            this.modCountAccessor = modCountAccessor;
            this.modCount = modCountAccessor.get();
            this.subList = subList;
            this.offset = offset;
        }


        @Override
        public int size() {
            checkForComodification();
            return subList.size();
        }

        @Override
        public boolean isEmpty() {
            checkForComodification();
            return subList.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            checkForComodification();
            return subList.contains(o);
        }

        @Override
        public Iterator<E> iterator() {
            checkForComodification();
            return new VetoableIteratorDecorator(parent, new ModCountAccessorImplSub(), subList.iterator(), offset);
        }

        @Override
        public Object[] toArray() {
            checkForComodification();
            return subList.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            checkForComodification();
            return subList.toArray(a);
        }

        @Override
        public boolean add(E e) {
            checkForComodification();
            parent.onProposedChange(Collections.<E>singletonList(e), offset + size(), offset + size());
            try {
                incrementModCount();
                subList.add(e);
            } catch (Exception ex) {
                decrementModCount();
                throw ex;
            }
            return true;
        }

        @Override
        public boolean remove(Object o) {
            checkForComodification();
            int i = indexOf(o);
            if (i != -1) {
                remove(i);
                return true;
            }
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            checkForComodification();
            return subList.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            checkForComodification();
            List<E> elements = parent.unmodifiableList(c);
            parent.onProposedChange(elements, offset + size(), offset + size());
            try {
                incrementModCount();
                boolean res =  subList.addAll(elements);
                if (!res)
                    decrementModCount();
                return res;
            } catch (Exception e) {
                decrementModCount();
                throw e;
            }
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> c) {
            Objects.checkIndex(index, size() + 1);
            checkForComodification();
            List<E> elements = parent.unmodifiableList(c);
            parent.onProposedChange(elements, offset + index, offset + index);
            try {
                incrementModCount();
                boolean res = subList.addAll(index, elements);
                if (!res)
                    decrementModCount();
                return res;
            } catch (Exception e) {
                decrementModCount();
                throw e;
            }
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            checkForComodification();
            Collection<?> elements = parent.safeCollection(c);
            parent.removeFromList(this, offset, elements, false);
            try {
                incrementModCount();
                boolean res = subList.removeAll(elements);
                if (!res)
                    decrementModCount();
                return res;
            } catch (Exception e) {
                decrementModCount();
                throw e;
            }
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            checkForComodification();
            Collection<?> elements = parent.safeCollection(c);
            parent.removeFromList(this, offset, elements, true);
            try {
                incrementModCount();
                boolean res = subList.retainAll(elements);
                if (!res)
                    decrementModCount();
                return res;
            } catch (Exception e) {
                decrementModCount();
                throw e;
            }
        }

        @Override
        public void clear() {
            checkForComodification();
            parent.onProposedChange(Collections.<E>emptyList(), offset, offset + size());
            try {
                incrementModCount();
                subList.clear();
            } catch (Exception e) {
                decrementModCount();
                throw e;
            }
        }

        @Override
        public E get(int index) {
            checkForComodification();
            return subList.get(index);
        }

        @Override
        public E set(int index, E element) {
            checkForComodification();
            parent.onProposedChange(Collections.singletonList(element), offset + index, offset + index + 1);
            return subList.set(index, element);
        }

        @Override
        public void add(int index, E element) {
            Objects.checkIndex(index, size() + 1);
            checkForComodification();
            parent.onProposedChange(Collections.singletonList(element), offset + index, offset + index);
            try {
                incrementModCount();
                subList.add(index, element);
            } catch (Exception e) {
                decrementModCount();
                throw e;
            }
        }

        @Override
        public E remove(int index) {
            Objects.checkIndex(index, size());
            checkForComodification();
            parent.onProposedChange(Collections.<E>emptyList(), offset + index, offset + index + 1);
            try {
                incrementModCount();
                E res =  subList.remove(index);
                return res;
            } catch (Exception e) {
                decrementModCount();
                throw e;
            }

        }

        @Override
        public int indexOf(Object o) {
            checkForComodification();
            return subList.indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o) {
            checkForComodification();
            return subList.lastIndexOf(o);
        }

        @Override
        public ListIterator<E> listIterator() {
            checkForComodification();
            return new VetoableListIteratorDecorator(parent, new ModCountAccessorImplSub(),
                    subList.listIterator(), offset);
        }

        @Override
        public ListIterator<E> listIterator(int index) {
            checkForComodification();
            return new VetoableListIteratorDecorator(parent, new ModCountAccessorImplSub(),
                    subList.listIterator(index), offset + index);
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            checkForComodification();
            return new VetoableSubListDecorator(parent, new ModCountAccessorImplSub(),
                    subList.subList(fromIndex, toIndex), offset + fromIndex);
        }

        @Override
        public String toString() {
            checkForComodification();
            return subList.toString();
        }

        @Override
        public boolean equals(Object obj) {
            checkForComodification();
            return subList.equals(obj);
        }

        @Override
        public int hashCode() {
            checkForComodification();
            return subList.hashCode();
        }

        private void checkForComodification() {
            if (modCount != modCountAccessor.get()) {
                throw new ConcurrentModificationException();
            }
        }

        private void incrementModCount() {
            modCount = modCountAccessor.incrementAndGet();
        }

        private void decrementModCount() {
            modCount = modCountAccessor.decrementAndGet();
        }

        private class ModCountAccessorImplSub implements ModCountAccessor{

            @Override
            public int get() {
                return modCount;
            }

            @Override
            public int incrementAndGet() {
                return modCount = modCountAccessor.incrementAndGet();
            }

            @Override
            public int decrementAndGet() {
                return modCount = modCountAccessor.decrementAndGet();
            }
        }
    }

    private static class VetoableIteratorDecorator<E> implements Iterator<E> {
        final VetoableListDecorator<E> parent;
        private final Iterator<E> it;
        private final ModCountAccessor modCountAccessor;
        private int modCount;
        protected final int offset;
        protected int cursor;
        protected int lastReturned;

        public VetoableIteratorDecorator(VetoableListDecorator<E> parent, ModCountAccessor modCountAccessor, Iterator<E> it, int offset) {
            this.parent = parent;
            this.modCountAccessor = modCountAccessor;
            this.modCount = modCountAccessor.get();
            this.it = it;
            this.offset = offset;
        }

        @Override
        public boolean hasNext() {
            checkForComodification();
            return it.hasNext();
        }

        @Override
        public E next() {
            checkForComodification();
            E e = it.next();
            lastReturned = cursor++;
            return e;
        }

        @Override
        public void remove() {
            checkForComodification();
            if (lastReturned == -1) {
                throw new IllegalStateException();
            }
            parent.onProposedChange(Collections.<E>emptyList(), offset + lastReturned, offset + lastReturned + 1);
            try {
                incrementModCount();
                it.remove();
            } catch (Exception e) {
                decrementModCount();
                throw e;
            }
            lastReturned = -1;
            --cursor;
        }

        protected void checkForComodification() {
            if (modCount != modCountAccessor.get()) {
                throw new ConcurrentModificationException();
            }
        }

        protected void incrementModCount() {
            modCount = modCountAccessor.incrementAndGet();
        }

        protected void decrementModCount() {
            modCount = modCountAccessor.decrementAndGet();
        }
    }

    private static class VetoableListIteratorDecorator<E> extends VetoableIteratorDecorator<E> implements ListIterator<E> {

        private final ListIterator<E> lit;

        public VetoableListIteratorDecorator(VetoableListDecorator<E> parent, ModCountAccessor modCountAccessor, ListIterator<E> it, int offset) {
            super(parent, modCountAccessor, it, offset);
            this.lit = it;
        }

        @Override
        public boolean hasPrevious() {
            checkForComodification();
            return lit.hasPrevious();
        }

        @Override
        public E previous() {
            checkForComodification();
            E e = lit.previous();
            lastReturned = --cursor;
            return e;
        }

        @Override
        public int nextIndex() {
            checkForComodification();
            return lit.nextIndex();
        }

        @Override
        public int previousIndex() {
            checkForComodification();
            return lit.previousIndex();
        }

        @Override
        public void set(E e) {
            checkForComodification();
            if (lastReturned == -1) {
                throw new IllegalStateException();
            }
            parent.onProposedChange(Collections.singletonList(e), offset + lastReturned, offset + lastReturned + 1);
            lit.set(e);
        }

        @Override
        public void add(E e) {
            checkForComodification();
            parent.onProposedChange(Collections.singletonList(e), offset + cursor, offset + cursor);
            try {
                incrementModCount();
                lit.add(e);
            } catch (Exception ex) {
                decrementModCount();
                throw ex;
            }
            ++cursor;
        }
    }

    private class ModCountAccessorImpl implements ModCountAccessor {

        public ModCountAccessorImpl() {
        }

        @Override
        public int get() {
            return modCount;
        }

        @Override
        public int incrementAndGet() {
            return ++modCount;
        }

        @Override
        public int decrementAndGet() {
            return --modCount;
        }
    }
}
