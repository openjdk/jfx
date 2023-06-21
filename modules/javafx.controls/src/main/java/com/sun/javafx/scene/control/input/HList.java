/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control.input;

import java.util.ArrayList;
import java.util.Iterator;
import javafx.event.EventHandler;

/**
 * List of event handlers, which can be added to its head or tail.
 * the order of handlers in the list is guaranteed to be:
 * (first added to the head), (second added to the head), ..., (second added to the tail), (first added to the tail).
 */
public class HList implements Iterable<EventHandler<?>> {
    private final ArrayList<EventHandler<?>> handlers = new ArrayList<>(4);
    private int insertIndex;

    public HList() {
    }

    public static HList from(Object x) {
        if (x instanceof HList h) {
            return h;
        }
        return new HList();
    }

    public void add(EventHandler<?> h, boolean tail) {
        if (insertIndex == handlers.size()) {
            handlers.add(h);
        } else {
            handlers.add(insertIndex, h);
        }

        if (!tail) {
            insertIndex++;
        }
    }

    @Override
    public Iterator<EventHandler<?>> iterator() {
        return handlers.iterator();
    }
}
