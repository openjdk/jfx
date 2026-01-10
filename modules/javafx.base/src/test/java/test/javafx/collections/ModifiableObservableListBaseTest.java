/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ModifiableObservableListBaseShim;
import javafx.collections.ObservableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static org.junit.jupiter.api.Assertions.*;

public class ModifiableObservableListBaseTest {

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
    class SetAllTest {
        private final ObservableList<String> list = FXCollections.observableArrayList(List.of("a", "b", "c"));
        private final List<String> recordedChanges = new ArrayList<>();

        {
            list.addListener((Change<? extends String> c) -> recordedChanges.add(c.toString()));
        }

        @Test
        public void testNullArgumentThrowsNPE() {
            assertThrows(NullPointerException.class, () -> list.setAll((Collection<? extends String>) null));
            assertThrows(NullPointerException.class, () -> list.replaceRange(0, 0, (Collection<? extends String>) null));
            assertEquals(List.of(), recordedChanges);
        }

        @Test
        void shouldRejectIllegalRange() {
            assertThrows(IndexOutOfBoundsException.class, () -> list.replaceRange(-1, 2, List.of("d", "e", "f")));
            assertThrows(IndexOutOfBoundsException.class, () -> list.replaceRange(0, 5, List.of("d", "e", "f")));
            assertThrows(IndexOutOfBoundsException.class, () -> list.replaceRange(2, 1, List.of("d", "e", "f")));
            assertEquals(List.of(), recordedChanges);
        }

        @Test
        void shouldNotModifyListWhenOperationIsANoOp() {
            assertFalse(list.replaceRange(1, 1, List.of()));
            assertEquals(List.of("a", "b", "c"), list);
            assertEquals(List.of(), recordedChanges);
        }

        @Test
        void shouldInsertWhenRangeIsEmpty() {
            assertTrue(list.replaceRange(1, 1, List.of("A", "B", "C")));
            assertEquals(List.of("a", "A", "B", "C", "b", "c"), list);
            assertEquals(List.of("{ [A, B, C] added at 1 }"), recordedChanges);
        }

        @Test
        void shouldReplaceElementsWithRangeAtEnd() {
            assertTrue(list.replaceRange(1, 3, List.of("B", "C", "D")));
            assertEquals(List.of("a", "B", "C", "D"), list);
            assertEquals(List.of("{ [b, c] replaced by [B, C, D] at 1 }"), recordedChanges);
        }

        @Test
        void shouldReplaceElementsWithRangeAtBeginning() {
            assertTrue(list.replaceRange(0, 2, List.of("A", "B", "C")));
            assertEquals(List.of("A", "B", "C", "c"), list);
            assertEquals(List.of("{ [a, b] replaced by [A, B, C] at 0 }"), recordedChanges);
        }

        @Test
        void shouldReplaceElementsWithRangeInMiddle() {
            assertTrue(list.replaceRange(1, 2, List.of("A", "B", "C")));
            assertEquals(List.of("a", "A", "B", "C", "c"), list);
            assertEquals(List.of("{ [b] replaced by [A, B, C] at 1 }"), recordedChanges);
        }

        @Test
        void shouldReplaceRangeWithFewerElementsThanAreRemovedAtEnd() {
            assertTrue(list.replaceRange(1, 3, List.of("B")));
            assertEquals(List.of("a", "B"), list);
            assertEquals(List.of("{ [b, c] replaced by [B] at 1 }"), recordedChanges);
        }

        @Test
        void shouldReplaceRangeWithFewerElementsThanAreRemovedAtBeginning() {
            assertTrue(list.replaceRange(0, 2, List.of("B")));
            assertEquals(List.of("B", "c"), list);
            assertEquals(List.of("{ [a, b] replaced by [B] at 0 }"), recordedChanges);
        }

        @Test
        void shouldOnlyRemoveRangeWhenCollectionWasEmpty() {
            assertTrue(list.replaceRange(1, 3, List.of()));
            assertEquals(List.of("a"), list);
            assertEquals(List.of("{ [b, c] removed at 1 }"), recordedChanges);
        }
    }

    @Nested
    class AddAllTest {
        @Test
        public void testNullArgumentThrowsNPE() {
            var list = new MockModifiableObservableList(new ArrayList<>(List.of("a", "b", "c")));
            assertThrows(NullPointerException.class, () -> list.addAll((Collection<? extends String>) null));
        }

        @Test
        public void testEmptyCollectionArgumentDoesNotEnumerateCollection() {
            var list = new MockModifiableObservableList(List.of()) {
                @Override
                public Iterator<String> iterator() {
                    throw new AssertionError("iterator() was not elided");
                }

                @Override
                public ListIterator<String> listIterator() {
                    throw new AssertionError("listIterator() was not elided");
                }
            };

            assertDoesNotThrow(() -> list.addAll(List.of()));
            assertDoesNotThrow(() -> list.addAll(0, List.of()));
        }

        @Test
        public void testInvalidIndexThrowsIOOBE() {
            var nonEmptyList = new MockModifiableObservableList(new ArrayList<>(List.of("a", "b", "c")));
            assertThrows(IndexOutOfBoundsException.class, () -> nonEmptyList.addAll(-1, List.of()));
            assertThrows(IndexOutOfBoundsException.class, () -> nonEmptyList.addAll(4, List.of()));
            assertDoesNotThrow(() -> nonEmptyList.addAll(3, List.of("d", "e")));

            var emptyList = new MockModifiableObservableList(new ArrayList<>());
            assertThrows(IndexOutOfBoundsException.class, () -> emptyList.addAll(-1, List.of()));
            assertThrows(IndexOutOfBoundsException.class, () -> emptyList.addAll(1, List.of()));
            assertDoesNotThrow(() -> emptyList.addAll(0, List.of("d", "e")));
        }
    }

    @Nested
    class RemoveAllTest {
        @Test
        public void testNullArgumentThrowsNPE() {
            var list = new MockModifiableObservableList(new ArrayList<>(List.of("a", "b", "c")));
            assertThrows(NullPointerException.class, () -> list.removeAll((Collection<? extends String>) null));
        }

        @Test
        public void testEmptyCollectionArgumentDoesNotEnumerateBackingList() {
            var list = new MockModifiableObservableList(List.of("a", "b", "c")) {
                @Override
                public String get(int index) {
                    throw new AssertionError("get() was not elided");
                }
            };

            assertDoesNotThrow(() -> list.removeAll(List.of()));
        }
    }

    @Nested
    class RetainAllTest {
        @Test
        public void testNullArgumentThrowsNPE() {
            var list = new MockModifiableObservableList(new ArrayList<>(List.of("a", "b", "c")));
            assertThrows(NullPointerException.class, () -> list.retainAll((Collection<? extends String>) null));
        }

        @Test
        public void testEmptyCollectionArgumentDoesNotCallContains() {
            var list = new MockModifiableObservableList(new ArrayList<>(List.of("a", "b", "c")));

            assertDoesNotThrow(() -> list.retainAll(new ArrayList<String>() {
                @Override
                public boolean contains(Object o) {
                    throw new AssertionError("contains() was not elided");
                }
            }));
        }

        @Test
        public void testEmptyCollectionArgumentCallsClear() {
            boolean[] flag = new boolean[1];
            new MockModifiableObservableList(new ArrayList<>(List.of("a", "b", "c"))) {
                @Override
                public void clear() {
                    flag[0] = true;
                    super.clear();
                }
            }.retainAll(List.of());

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

            assertDoesNotThrow(() -> list.retainAll(List.of()));
        }
    }

    @Nested
    class SubList_AddAllTest {
        @Test
        public void testNullArgumentThrowsNPE() {
            var subList = new MockModifiableObservableList(List.of("a", "b", "c"), List.of("a", "b")).subList(0, 2);
            assertThrows(NullPointerException.class, () -> subList.addAll((Collection<? extends String>) null));
        }

        @Test
        public void testEmptyCollectionArgumentDoesNotEnumerateCollection() {
            var backingSubList = new ArrayList<String>(List.of()) {
                @Override
                public Iterator<String> iterator() {
                    throw new AssertionError("iterator() was not elided");
                }

                @Override
                public ListIterator<String> listIterator() {
                    throw new AssertionError("listIterator() was not elided");
                }
            };

            var subList = new MockModifiableObservableList(List.of(), backingSubList).subList(0, 0);
            assertDoesNotThrow(() -> subList.addAll(List.of()));
            assertDoesNotThrow(() -> subList.addAll(0, List.of()));
        }

        @Test
        public void testInvalidIndexThrowsIOOBE() {
            var nonEmptySubList = new MockModifiableObservableList(
                    new ArrayList<>(List.of("a", "b", "c")),
                    new ArrayList<>(List.of("a", "b")))
                .subList(0, 2);
            assertThrows(IndexOutOfBoundsException.class, () -> nonEmptySubList.addAll(-1, List.of()));
            assertThrows(IndexOutOfBoundsException.class, () -> nonEmptySubList.addAll(3, List.of()));
            assertDoesNotThrow(() -> nonEmptySubList.addAll(2, List.of("d", "e")));

            var emptySubList = new MockModifiableObservableList(
                    new ArrayList<>(List.of("a", "b", "c")),
                    new ArrayList<>())
                .subList(0, 0);
            assertThrows(IndexOutOfBoundsException.class, () -> emptySubList.addAll(-1, List.of()));
            assertThrows(IndexOutOfBoundsException.class, () -> emptySubList.addAll(1, List.of()));
            assertDoesNotThrow(() -> emptySubList.addAll(0, List.of("d", "e")));
        }
    }

    @Nested
    class SubList_RemoveAllTest {
        @Test
        public void testNullArgumentThrowsNPE() {
            var subList = new MockModifiableObservableList(List.of("a", "b", "c"), List.of("a", "b")).subList(0, 2);
            assertThrows(NullPointerException.class, () -> subList.removeAll((Collection<? extends String>) null));
        }

        @Test
        public void testEmptyCollectionArgumentDoesNotCallRemoveAll() {
            var backingList = List.of("a", "b", "c");
            var backingSubList = new ArrayList<>(List.of("a", "b")) {
                @Override
                public boolean removeAll(Collection<?> c) {
                    throw new AssertionError("removeAll() was not elided");
                }
            };

            var subList = new MockModifiableObservableList(backingList, backingSubList).subList(0, 2);
            assertDoesNotThrow(() -> subList.removeAll(List.of()));
        }
    }

    @Nested
    class SubList_RetainAllTest {
        @Test
        public void testNullArgumentThrowsNPE() {
            var subList = new MockModifiableObservableList(List.of("a", "b", "c"), List.of("a", "b")).subList(0, 2);
            assertThrows(NullPointerException.class, () -> subList.retainAll((Collection<? extends String>) null));
        }

        @Test
        public void testEmptyCollectionArgumentDoesNotCallContains() {
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
        public void testEmptyCollectionArgumentCallsClear() {
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
            subList.retainAll(List.of());
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
            assertDoesNotThrow(() -> subList.retainAll(List.of()));
        }
    }

    private static class MockModifiableObservableList extends ModifiableObservableListBaseShim<String> {
        private final List<String> backingList;
        private final List<String> subList;

        MockModifiableObservableList(List<String> list) {
            this.backingList = list;
            this.subList = List.of();
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
