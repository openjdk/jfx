/*
 * Copyright (c) 2015, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.css;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javafx.beans.InvalidationListener;
import javafx.collections.SetChangeListener;
import javafx.css.PseudoClass;

public class BitSetShim<T> {

    public static boolean add(BitSet s, Object t) {
        return s.add(t);
    }

    public static boolean addAll(BitSet s, Collection c) {
        return s.addAll(c);
    }

    public static boolean contains(BitSet s, Object o) {
        return s.contains(o);
    }

    public static boolean containsAll(BitSet s, Collection<?> c) {
        return s.containsAll(c);
    }

    public static boolean equals(BitSet s, Object obj) {
        return s.equals(obj);
    }

    public static long[] getBits(BitSet s) {
        return s.getBits();
    }

    public static boolean isEmpty(BitSet s) {
        return s.isEmpty();
    }

    public static Iterator iterator(BitSet s) {
        return s.iterator();
    }

    public static boolean remove(BitSet s, Object o) {
        return s.remove(o);
    }

    public static boolean retainAll(BitSet s, Collection<?> c) {
        return s.retainAll(c);
    }

    public static int size(BitSet s) {
        return s.size();
    }

    public static BitSetShim<PseudoClass> getPseudoClassInstance() {
        return new BitSetShim<>(new PseudoClassState());
    }

    private final BitSet<T> delegate;

    private BitSetShim(BitSet<T> delegate) {
        this.delegate = delegate;
    }

    // These delegate methods were generated automatically by an IDE.

    public void forEach(Consumer<? super T> action) {
        delegate.forEach(action);
    }

    public int size() {
        return delegate.size();
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public Iterator<T> iterator() {
        return delegate.iterator();
    }

    public boolean add(T t) {
        return delegate.add(t);
    }

    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    public Object[] toArray() {
        return delegate.toArray();
    }

    public boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }

    public <E> E[] toArray(E[] a) {
        return delegate.toArray(a);
    }

    public boolean addAll(Collection<? extends T> c) {
        return delegate.addAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return delegate.retainAll(c);
    }

    public boolean removeAll(Collection<?> c) {
        return delegate.removeAll(c);
    }

    public void clear() {
        delegate.clear();
    }

    public void addListener(SetChangeListener<? super T> setChangeListener) {
        delegate.addListener(setChangeListener);
    }

    public void removeListener(SetChangeListener<? super T> setChangeListener) {
        delegate.removeListener(setChangeListener);
    }

    public void addListener(InvalidationListener invalidationListener) {
        delegate.addListener(invalidationListener);
    }

    public void removeListener(InvalidationListener invalidationListener) {
        delegate.removeListener(invalidationListener);
    }

    public Spliterator<T> spliterator() {
        return delegate.spliterator();
    }

    public <E> E[] toArray(IntFunction<E[]> generator) {
        return delegate.toArray(generator);
    }

    public boolean removeIf(Predicate<? super T> filter) {
        return delegate.removeIf(filter);
    }

    public Stream<T> stream() {
        return delegate.stream();
    }

    public Stream<T> parallelStream() {
        return delegate.parallelStream();
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return delegate.equals(other);
    }
}
