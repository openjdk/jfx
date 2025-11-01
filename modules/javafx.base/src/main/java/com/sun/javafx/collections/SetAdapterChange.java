/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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
import javafx.collections.SetChangeListener.Change;

public final class SetAdapterChange<E> extends SetChangeListener.Change<E> {

    private Change<? extends E> change;

    public SetAdapterChange(ObservableSet<E> set, Change<? extends E> change) {
        super(set);
        this.change = change;
    }

    @Override
    public String toString() {
        return change.toString();
    }

    @Override
    public boolean wasAdded() {
        return change.wasAdded();
    }

    @Override
    public boolean wasRemoved() {
        return change.wasRemoved();
    }

    @Override
    public E getElementAdded() {
        return change.getElementAdded();
    }

    @Override
    public E getElementRemoved() {
        return change.getElementRemoved();
    }

    @Override
    public Change<E> next() {
        Change<? extends E> nextChange = change.next();
        if (nextChange != null) {
            change = nextChange;
            return this;
        }

        return null;
    }
}
