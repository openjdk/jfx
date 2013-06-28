/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.collections;

import static org.junit.Assert.*;

/**
 * A mock observer that tracks calls to its onChanged() method,
 * combined with utility methods to make assertions on the calls made.
 *
 */
public class MockArrayObserver<T extends ObservableArray<T>> implements ArrayChangeListener<T> {
    private boolean tooManyCalls;

    static class Call<T> {
        T array;
        boolean sizeChanged;
        int from;
        int to;

        @Override
        public String toString() {
            return  "sizeChanged: " + sizeChanged + ", from: " + from + ", to: " + to;
        }
    }

    Call call;

    @Override
    public void onChanged(T observableArray, boolean sizeChanged, int from, int to) {
        if (call == null) {
            call = new Call();
            call.array = observableArray;
            call.sizeChanged = sizeChanged;
            call.from = from;
            call.to = to;

            // Check generic change assertions
            assertFalse("Negative from index", from < 0);
            assertFalse("Negative to index", to < 0);
            assertFalse("from index is greater then to index", from > to);
            assertFalse("No change in both elements and size", from == to && sizeChanged == false);
            assertFalse("from index is greater than array size", from < to && from >= observableArray.size());
            assertFalse("to index is greater than array size", from < to && to > observableArray.size());
        } else {
            tooManyCalls = true;
        }
    }

    public void check0() {
        assertNull(call);
    }
    
    public void checkOnlySizeChanged(T array) {
        assertFalse("Too many array change events", tooManyCalls);
        assertSame(array, call.array);
        assertEquals(true, call.sizeChanged);
    }

    public void checkOnlyElementsChanged(T array,
                       int from,
                       int to) {
        assertFalse("Too many array change events", tooManyCalls);
        assertSame(array, call.array);
        assertEquals(false, call.sizeChanged);
        assertEquals(from, call.from);
        assertEquals(to, call.to);
    }

    public void check(T array,
                       boolean sizeChanged,
                       int from,
                       int to) {
        assertFalse("Too many array change events", tooManyCalls);
        assertSame(array, call.array);
        assertEquals(sizeChanged, call.sizeChanged);
        assertEquals(from, call.from);
        assertEquals(to, call.to);
    }

    public void check1() {
        assertFalse("Too many array change events", tooManyCalls);
        assertNotNull(call);
    }

    public void reset() {
        call = null;
        tooManyCalls = false;
    }
}
