/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.LinkedList;
import java.util.List;

/**
 * A list that has a maximum size. Useful for recording historical data, but not
 * too much of it.
 *
 * <p>Items are added at the front of the list. That is, the item at index 0 is
 * more recent than that added at index 1. In this regard, this class resembles
 * a stack.</p>
 *
 * @param <E> The type of the items added into the list.
 */
public class SizeLimitedList<E> {
    private final int maxSize;
    private final List<E> backingList;

    public SizeLimitedList(int maxSize) {
        this.maxSize = maxSize;
        this.backingList = new LinkedList<>();
    }

    public E get(int index) {
        return backingList.get(index);
    }

    public void add(E item) {
        backingList.add(0, item);

        if (backingList.size() > maxSize) {
            backingList.remove(maxSize);
        }
    }

    public int size() {
        return backingList.size();
    }

    public boolean contains(E item) {
        return backingList.contains(item);
    }
}
