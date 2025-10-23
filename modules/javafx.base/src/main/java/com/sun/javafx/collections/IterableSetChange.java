/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import java.util.List;

/**
 * Base class for set changes that support bulk change iteration.
 *
 * @param <E> the element type
 */
public sealed abstract class IterableSetChange<E> extends SetChangeListener.Change<E> {

    private IterableSetChange(ObservableSet<E> set) {
        super(set);
    }

    /**
     * Returns {@code this} object instance if there is another change to report, or {@code null} if there
     * are no more changes. If this method returns another change, the implementation must configure this
     * object instance to represent the next change.
     * <p>
     * Note that this narrows down the {@link SetChangeListener.Change#next()} specification, which does
     * not mandate that the same object instance is returned on each call.
     *
     * @return this instance, representing the next change, or {@code null} if there are no more changes
     */
    @Override
    public abstract SetChangeListener.Change<E> next();

    /**
     * Resets this {@code IterableSetChange} instance to the first change.
     */
    public abstract void reset();

    public static final class Add<E> extends IterableSetChange<E> {

        private final List<E> elements;
        private int index;

        public Add(ObservableSet<E> set, List<E> elements) {
            super(set);
            this.elements = elements;
        }

        @Override
        public SetChangeListener.Change<E> next() {
            if (index < elements.size() - 1) {
                ++index;
                return this;
            }

            return null;
        }

        @Override
        public void reset() {
            index = 0;
        }

        @Override
        public boolean wasAdded() {
            return true;
        }

        @Override
        public boolean wasRemoved() {
            return false;
        }

        @Override
        public E getElementAdded() {
            return elements.get(index);
        }

        @Override
        public E getElementRemoved() {
            return null;
        }

        @Override
        public String toString() {
            return "added " + elements.get(index);
        }
    }

    public static final class Remove<E> extends IterableSetChange<E> {

        private final List<E> elements;
        private int index;

        public Remove(ObservableSet<E> set, List<E> elements) {
            super(set);
            this.elements = elements;
        }

        @Override
        public SetChangeListener.Change<E> next() {
            if (index < elements.size() - 1) {
                ++index;
                return this;
            }

            return null;
        }

        @Override
        public void reset() {
            index = 0;
        }

        @Override
        public boolean wasAdded() {
            return false;
        }

        @Override
        public boolean wasRemoved() {
            return true;
        }

        @Override
        public E getElementAdded() {
            return null;
        }

        @Override
        public E getElementRemoved() {
            return elements.get(index);
        }

        @Override
        public String toString() {
            return "removed " + elements.get(index);
        }
    }
}
