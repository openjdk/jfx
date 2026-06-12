/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package jfx.incubator.scene.control.richtext.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import javafx.scene.text.TabStop;

/**
 * Encapsulates the tab stop positions within a paragraph.
 *
 * @since 27
 */
public class TabStops implements List<TabStop> {
    // JDK should have a public ImmutableList class.
    private final List<TabStop> stops;

    /**
     * Constructor.
     * @param stops the tab stops
     */
    public TabStops(List<TabStop> stops) {
        this.stops = List.copyOf(stops);
    }

    /**
     * Creates a {@code TabStops} instance from tab stop positions.
     * @param positions the tab stop positions
     * @return the new instance
     */
    public static TabStops of(double ... positions) {
        int sz = positions.length;
        ArrayList<TabStop> ts = new ArrayList<>(sz);
        for (int i = 0; i < sz; i++) {
            ts.add(new TabStop(positions[i]));
        }
        return new TabStops(ts);
    }

    @Override
    public Iterator<TabStop> iterator() {
        return stops.iterator();
    }

    @Override
    public int hashCode() {
        return stops.hashCode();
    }

    @Override
    public boolean equals(Object x) {
        if (x == this) {
            return true;
        }
        return stops.equals(x);
    }

    @Override
    public int size() {
        return stops.size();
    }

    @Override
    public boolean isEmpty() {
        return stops.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return stops.contains(o);
    }

    @Override
    public Object[] toArray() {
        return stops.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return stops.toArray(a);
    }

    @Override
    public boolean add(TabStop e) {
        throw err();
    }

    @Override
    public boolean remove(Object o) {
        throw err();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return stops.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends TabStop> c) {
        throw err();
    }

    @Override
    public boolean addAll(int index, Collection<? extends TabStop> c) {
        throw err();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw err();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw err();
    }

    @Override
    public void clear() {
        throw err();
    }

    @Override
    public TabStop get(int index) {
        return stops.get(index);
    }

    @Override
    public TabStop set(int index, TabStop element) {
        throw err();
    }

    @Override
    public void add(int index, TabStop element) {
        throw err();
    }

    @Override
    public TabStop remove(int index) {
        throw err();
    }

    @Override
    public int indexOf(Object o) {
        return stops.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return stops.lastIndexOf(o);
    }

    @Override
    public ListIterator<TabStop> listIterator() {
        return stops.listIterator();
    }

    @Override
    public ListIterator<TabStop> listIterator(int index) {
        return stops.listIterator(index);
    }

    @Override
    public List<TabStop> subList(int fromIndex, int toIndex) {
        return stops.subList(fromIndex, toIndex);
    }

    private static UnsupportedOperationException err() {
        return new UnsupportedOperationException();
    }
}
