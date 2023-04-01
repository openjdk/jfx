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

package test.javafx.collections;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import javafx.collections.ModifiableObservableListBaseShim;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static org.junit.jupiter.api.Assertions.*;

public class ModifiableObservableListBaseTest {

    @Test
    public void testAddAllWithEmptyCollectionArgumentDoesNotEnumerateCollection() {
        var list = new MockModifiableObservableList(Collections.emptyList()) {
            @Override
            public Iterator<String> iterator() {
                throw new AssertionError("iterator() was not elided");
            }

            @Override
            public ListIterator<String> listIterator() {
                throw new AssertionError("listIterator() was not elided");
            }
        };

        assertDoesNotThrow(() -> list.addAll(Collections.emptyList()));
        assertDoesNotThrow(() -> list.addAll(0, Collections.emptyList()));
    }

    @Test
    public void testAddAllWithEmptyCollectionArgumentAndInvalidIndexThrowsIOOBE() {
        var list = new MockModifiableObservableList(new ArrayList<>(List.of("a", "b", "c")));
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(-1, Collections.emptyList()));
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(4, Collections.emptyList()));
        assertDoesNotThrow(() -> list.addAll(3, List.of("d", "e")));
    }

    @Test
    public void testRemoveAllWithEmptyCollectionArgumentDoesNotEnumerateBackingList() {
        var list = new MockModifiableObservableList(List.of("a", "b", "c")) {
            @Override
            public String get(int index) {
                throw new AssertionError("get() was not elided");
            }
        };

        assertDoesNotThrow(() -> list.removeAll(Collections.<String>emptyList()));
    }

    @Test
    public void testRetainAllWithEmptyCollectionArgumentDoesNotCallContains() {
        var list = new MockModifiableObservableList(new ArrayList<>(List.of("a", "b", "c")));

        assertDoesNotThrow(() -> list.retainAll(new ArrayList<String>() {
            @Override
            public boolean contains(Object o) {
                throw new AssertionError("contains() was not elided");
            }
        }));
    }

    @Test
    public void testRetainAllWithEmptyCollectionArgumentCallsClear() {
        boolean[] flag = new boolean[1];
        new MockModifiableObservableList(new ArrayList<>(List.of("a", "b", "c"))) {
            @Override
            public void clear() {
                flag[0] = true;
                super.clear();
            }
        }.retainAll(Collections.<String>emptyList());

        assertTrue(flag[0], "clear() was not called");
    }

    @Test
    public void testRetainAllOnEmptyListDoesNotCallClear() {
        var list = new MockModifiableObservableList(new ArrayList<>()) {
            @Override
            public void clear() {
                throw new AssertionError("clear() was not elided");
            }
        };

        assertDoesNotThrow(() -> list.retainAll(Collections.<String>emptyList()));
    }

    @Test
    public void testRemoveEmptyRangeDoesNotEnumerateList() {
        var list = new MockModifiableObservableList(List.of("a", "b", "c")) {
            @Override
            public Iterator<String> iterator() {
                throw new AssertionError("iterator() was not elided");
            }

            @Override
            public ListIterator<String> listIterator() {
                throw new AssertionError("listIterator() was not elided");
            }
        };

        assertDoesNotThrow(() -> list.removeRange(0, 0));
    }

    @Nested
    class SubListTest {
        @Test
        public void testAddAllWithEmptyCollectionArgumentDoesNotEnumerateCollection() {
            var backingSubList = new ArrayList<>(Collections.<String>emptyList()) {
                @Override
                public Iterator<String> iterator() {
                    throw new AssertionError("iterator() was not elided");
                }

                @Override
                public ListIterator<String> listIterator() {
                    throw new AssertionError("listIterator() was not elided");
                }
            };

            var subList = new MockModifiableObservableList(Collections.emptyList(), backingSubList).subList(0, 0);
            assertDoesNotThrow(() -> subList.addAll(Collections.emptyList()));
            assertDoesNotThrow(() -> subList.addAll(0, Collections.emptyList()));
        }

        @Test
        public void testAddAllWithEmptyCollectionArgumentAndInvalidIndexThrowsIOOBE() {
            var backingList = new MockModifiableObservableList(new ArrayList<>(List.of("a", "b", "c")));
            var backingSubList = new ArrayList<>(new ArrayList<>(List.of("a", "b")));
            var subList = new MockModifiableObservableList(backingList, backingSubList).subList(0, 2);
            assertThrows(IndexOutOfBoundsException.class, () -> subList.addAll(-1, Collections.emptyList()));
            assertThrows(IndexOutOfBoundsException.class, () -> subList.addAll(3, Collections.emptyList()));
            assertDoesNotThrow(() -> subList.addAll(2, List.of("d", "e")));
        }

        @Test
        public void testRemoveAllWithEmptyCollectionArgumentDoesNotCallRemoveAll() {
            var backingList = List.of("a", "b", "c");
            var backingSubList = new ArrayList<>(List.of("a", "b")) {
                @Override
                public boolean removeAll(Collection<?> c) {
                    throw new AssertionError("removeAll() was not elided");
                }
            };

            var subList = new MockModifiableObservableList(backingList, backingSubList).subList(0, 2);
            assertDoesNotThrow(() -> subList.removeAll(Collections.<String>emptyList()));
        }

        @Test
        public void testRetainAllWithEmptyCollectionArgumentDoesNotCallContains() {
            var backingList = List.of("a", "b", "c");
            var backingSubList = new ArrayList<>(List.of("a", "b"));
            var subList = new MockModifiableObservableList(backingList, backingSubList).subList(0, 2);

            assertDoesNotThrow(() -> subList.retainAll(new ArrayList<String>() {
                @Override
                public boolean contains(Object o) {
                    throw new AssertionError("contains() was not elided");
                }
            }));
        }

        @Test
        public void testRetainAllWithEmptyCollectionArgumentCallsClear() {
            boolean[] flag = new boolean[1];
            var backingList = List.of("a", "b", "c");
            var backingSubList = new ArrayList<>(List.of("a", "b")) {
                @Override
                public void clear() {
                    flag[0] = true;
                    super.clear();
                }
            };

            var subList = new MockModifiableObservableList(backingList, backingSubList).subList(0, 2);
            subList.retainAll(Collections.<String>emptyList());
            assertTrue(flag[0], "clear() was not called");
        }

        @Test
        public void testRetainAllOnEmptyListDoesNotCallClear() {
            var backingList = new ArrayList<String>();
            var backingSubList = new ArrayList<String>() {
                @Override
                public void clear() {
                    throw new AssertionError("clear() was not elided");
                }
            };

            var subList = new MockModifiableObservableList(backingList, backingSubList).subList(0, 0);
            assertDoesNotThrow(() -> subList.retainAll(Collections.<String>emptyList()));
        }
    }

    private static class MockModifiableObservableList extends ModifiableObservableListBaseShim<String> {
        private final List<String> backingList;
        private final List<String> subList;

        MockModifiableObservableList(List<String> list) {
            this.backingList = list;
            this.subList = Collections.emptyList();
        }

        MockModifiableObservableList(List<String> list, List<String> subList) {
            this.backingList = list;
            this.subList = subList;
        }

        @Override public String get(int index) { return backingList.get(index); }
        @Override public int size() { return backingList.size(); }
        @Override protected void doAdd(int index, String element) { backingList.add(index, element); }
        @Override protected String doSet(int index, String element) { return backingList.set(index, element); }
        @Override protected String doRemove(int index) { return backingList.remove(index); }
        @Override public void removeRange(int fromIndex, int toIndex) { super.removeRange(fromIndex, toIndex); }
        @Override protected List<String> getTestSubList(int fromIndex, int toIndex) { return subList; }
    }

}
