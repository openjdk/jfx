/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

public class BitSetShim {

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

}
