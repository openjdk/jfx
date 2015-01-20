/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MockSetObserver<E> implements SetChangeListener<E> {

    private List<Call> calls = new ArrayList<Call>();

    @Override
    public void onChanged(Change<? extends E> c) {
        calls.add(new Call<E>(c.getElementRemoved(), c.getElementAdded()));
    }

    public int getCallsNumber() {
        return calls.size();
    }

    public void clear() {
        calls.clear();
    }

    public void check0() {
        assertEquals(0, calls.size());
    }

    public void assertAdded(Tuple<E> tuple) {
        assertAdded(0, tuple);
    }

    public void assertAdded(int call, Tuple<E> tuple) {
        assertTrue("Missing call to the observer # " + call, call < calls.size());
        assertEquals(calls.get(call).added, tuple.val);
    }

    public void assertMultipleCalls(Call<E>... calls) {
        assertEquals(this.calls.size(), calls.length);
        for (Call<E> c : calls) {
            assertTrue(Arrays.toString(calls) + " doesn't contain "  + c, this.calls.contains(c));
        }
    }

    public void assertMultipleRemove(Tuple<E>... tuples) {
        assertEquals(this.calls.size(), tuples.length);
        for (Tuple<E> t : tuples) {
            assertTrue(calls + " doesn't contain "  + t, this.calls.contains(new Call<E>(t.val, null)));
        }
    }

    public void assertRemoved(Tuple<E> tuple) {
        assertRemoved(0, tuple);
    }

    public void assertRemoved(int call, Tuple<E> tuple) {
        assertTrue("Missing call to the observer # " + call, call < calls.size());
        assertEquals(calls.get(call).removed, tuple.val);
    }

    public void assertMultipleRemoved(Tuple<E>... tuples) {
        for (Tuple<E> t : tuples) {
            boolean found = false;
            for (Call c : calls) {
                if (c.removed.equals(t.val)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
    }

    public static class Call<E> {
        private E removed;
        private E added;

        public Call(E removed, E added) {
            this.removed = removed;
            this.added = added;
        }

        public static<E> Call<E> call(E o, E n) {
            return new Call<E>(o, n);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Call<E> other = (Call<E>) obj;
            if (this.removed != other.removed && (this.removed == null || !this.removed.equals(other.removed))) {
                return false;
            }
            if (this.added != other.added && (this.added == null || !this.added.equals(other.added))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 47 * hash + (this.removed != null ? this.removed.hashCode() : 0);
            hash = 47 * hash + (this.added != null ? this.added.hashCode() : 0);
            return hash;
        }

        @Override
        public String toString() {
            return "[ " + added + " (" + removed + ") ]";
        }

    }

    public static class Tuple<E> {
        public E val;

        private Tuple(E val) {
            this.val = val;
        }

        public static<E> Tuple<E> tup(E v) {
            return new Tuple<E>(v);
        }
    }

}
