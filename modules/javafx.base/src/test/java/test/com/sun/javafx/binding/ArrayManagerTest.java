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

package test.com.sun.javafx.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.sun.javafx.binding.ArrayManager;

public class ArrayManagerTest {

    private final ArrayManager<ArrayManagerTest, String> manager = new ArrayManager<>(String.class) {
        protected int compact(ArrayManagerTest instance, String[] array) {
            return compacter.apply(instance, array);
        }

        @Override
        public String[] getArray(ArrayManagerTest instance) {
            return instance.array;
        }

        @Override
        public void setArray(ArrayManagerTest instance, String[] array) {
            instance.array = array;
        }

        @Override
        public int getOccupiedSlots(ArrayManagerTest instance) {
            return instance.size;
        }

        @Override
        public void setOccupiedSlots(ArrayManagerTest instance, int occupiedSlots) {
            instance.size = occupiedSlots;
        }
    };

    private BiFunction<ArrayManagerTest, String[], Integer> compacter = (instance, array) -> 0;
    private String[] array;
    private int size;

    @Test
    void constructorShouldRejectNullArguments() {
        assertThrows(NullPointerException.class, () -> new ArrayManager<>(null) {
            @Override
            protected Object[] getArray(Object instance) {
                return null;
            }

            @Override
            protected void setArray(Object instance, Object[] array) {
            }

            @Override
            protected int getOccupiedSlots(Object instance) {
                return 0;
            }

            @Override
            protected void setOccupiedSlots(Object instance, int occupiedSlots) {
            }
        });
    }

    @Nested
    class WhenEmpty {

        @Test
        void getShouldRejectAccessingIllegalIndices() {
            assertThrows(IndexOutOfBoundsException.class, () -> manager.get(ArrayManagerTest.this, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> manager.get(ArrayManagerTest.this, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> manager.get(ArrayManagerTest.this, -1));
        }

        @Test
        void removeShouldRejectRemovingIllegalIndices() {
            assertThrows(IndexOutOfBoundsException.class, () -> manager.remove(ArrayManagerTest.this, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> manager.remove(ArrayManagerTest.this, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> manager.remove(ArrayManagerTest.this, -1));
        }

        @Test
        void setShouldRejectSettingIllegalIndices() {
            assertThrows(IndexOutOfBoundsException.class, () -> manager.set(ArrayManagerTest.this, 0, "A"));
            assertThrows(IndexOutOfBoundsException.class, () -> manager.set(ArrayManagerTest.this, 1, "A"));
            assertThrows(IndexOutOfBoundsException.class, () -> manager.set(ArrayManagerTest.this, -1, "A"));
        }

        @Test
        void indexOfShouldNotFindAnything() {
            assertEquals(-1, manager.indexOf(ArrayManagerTest.this, "A"));
            assertEquals(-1, manager.indexOf(ArrayManagerTest.this, null));
            assertEquals(-1, manager.indexOf(ArrayManagerTest.this, ""));
        }

        @Test
        void removeIfShouldNotRemoveAnything() {
            assertFalse(manager.removeIf(ArrayManagerTest.this, s -> true));

            assertEquals(0, size);
        }

        @Test
        void addShouldAddElements() {
           assertEquals(0, size);
           manager.add(ArrayManagerTest.this, "A");
           assertEquals(1, size);
           manager.add(ArrayManagerTest.this, null);
           assertEquals(2, size);
           manager.add(ArrayManagerTest.this, "");
           assertEquals(3, size);
        }

        @Test
        void compactShouldBeCalledWhenGrowingANonEmptyArray() {
            AtomicBoolean called = new AtomicBoolean();

            compacter = (instance, array) -> { called.set(true); return 0; };

            manager.add(ArrayManagerTest.this, "A");

            assertNotNull(array);
            assertFalse(called.get());

            manager.add(ArrayManagerTest.this, "B");
            manager.add(ArrayManagerTest.this, "C");
            manager.add(ArrayManagerTest.this, "D");  // triggers grow with default settings

            assertTrue(called.get());
        }

        @Nested
        class AndSomeElementsAreAdded {
            {
                manager.add(ArrayManagerTest.this, "A");
                manager.add(ArrayManagerTest.this, "B");
                manager.add(ArrayManagerTest.this, "C");
                manager.add(ArrayManagerTest.this, "D");
                manager.add(ArrayManagerTest.this, "E");
            }

            @Test
            void shouldContainsThoseElementsInInsertionOrder() {
                assertElements("A", "B", "C", "D", "E");
            }

            @Test
            void removeShouldRemoveElements() {
                assertEquals("C", manager.remove(ArrayManagerTest.this, 2));
                assertElements("A", "B", "D", "E");
                assertEquals(7, array.length);

                assertEquals("D", manager.remove(ArrayManagerTest.this, 2));
                assertElements("A", "B", "E");
                assertEquals(7, array.length);

                assertEquals("A", manager.remove(ArrayManagerTest.this, 0));
                assertElements("B", "E");
                assertEquals(7, array.length);

                assertEquals("E", manager.remove(ArrayManagerTest.this, 1));  // triggers shrink
                assertElements("B");
                assertEquals(3, array.length);

                assertEquals("B", manager.remove(ArrayManagerTest.this, 0));  // triggers empty
                assertElements();
                assertNull(array);
            }

            @Test
            void removeShouldRemoveElements_2() {  // for coverage of remove from end, and shrink case with elements that must be moved
                assertEquals("C", manager.remove(ArrayManagerTest.this, 2));
                assertElements("A", "B", "D", "E");
                assertEquals(7, array.length);

                assertEquals("E", manager.remove(ArrayManagerTest.this, 3));
                assertElements("A", "B", "D");
                assertEquals(7, array.length);

                assertEquals("A", manager.remove(ArrayManagerTest.this, 0));
                assertElements("B", "D");
                assertEquals(7, array.length);

                assertEquals("B", manager.remove(ArrayManagerTest.this, 0));  // triggers shrink
                assertElements("D");
                assertEquals(3, array.length);

                assertEquals("D", manager.remove(ArrayManagerTest.this, 0));  // triggers empty
                assertElements();
                assertNull(array);
            }

            @Test
            void removeIfShouldNotRemoveAnythingWhenNothingMatched() {
                assertFalse(manager.removeIf(ArrayManagerTest.this, s -> s.compareTo("F") > 0));
            }

            @Test
            void removeIfShouldRemoveElementsFromEnd() {
                assertTrue(manager.removeIf(ArrayManagerTest.this, s -> s.compareTo("C") > 0));

                assertEquals(3, size);
                assertEquals(manager.get(ArrayManagerTest.this, 0), "A");
                assertEquals(manager.get(ArrayManagerTest.this, 1), "B");
                assertEquals(manager.get(ArrayManagerTest.this, 2), "C");
            }

            @Test
            void removeIfShouldRemoveElementsFromBeginning() {
                assertTrue(manager.removeIf(ArrayManagerTest.this, s -> s.compareTo("C") < 0));

                assertEquals(3, size);
                assertEquals(manager.get(ArrayManagerTest.this, 0), "C");
                assertEquals(manager.get(ArrayManagerTest.this, 1), "D");
                assertEquals(manager.get(ArrayManagerTest.this, 2), "E");
            }

            @Test
            void removeIfShouldAllocateNewArrayIfNeeded() {
                assertEquals(7, array.length);

                assertTrue(manager.removeIf(ArrayManagerTest.this, s -> s.compareTo("E") < 0));

                assertEquals(1, size);
                assertEquals(manager.get(ArrayManagerTest.this, 0), "E");
                assertEquals(3, array.length);
            }

            @Test
            void removeIfShouldReturnEmptyArrayWhenAllRemoved() {
                assertEquals(7, array.length);

                assertTrue(manager.removeIf(ArrayManagerTest.this, s -> s.compareTo("F") < 0));

                assertEquals(0, size);
                assertNull(array);
            }

            @Test
            void indexOfShouldBeAbleToFindElements() {
                assertEquals(2, manager.indexOf(ArrayManagerTest.this, "C"));
                assertEquals(-1, manager.indexOf(ArrayManagerTest.this, null));

                manager.add(ArrayManagerTest.this, null);

                assertEquals(5, manager.indexOf(ArrayManagerTest.this, null));
            }

            @Test
            void setShouldBeAbleToReplaceElements() {
                manager.set(ArrayManagerTest.this, 2, "c");
                manager.set(ArrayManagerTest.this, 4, null);

                assertElements("A", "B", "c", "D", null);
            }

            @Test
            void shouldShrinkArrayWhenCompactionIsVeryEffective() {
                AtomicBoolean called = new AtomicBoolean();

                compacter = (instance, array) -> { called.set(true); return 7; };  // 7 out of 7 compacted

                assertEquals(7, array.length);

                manager.add(ArrayManagerTest.this, "F");
                manager.add(ArrayManagerTest.this, "G");
                manager.add(ArrayManagerTest.this, "H");  // triggers grow with default settings

                assertTrue(called.get());
                assertEquals(3, array.length);
                assertElements("H");
            }

            @Test
            void shouldKeepArrayWhenCompactionReclaimsSpace() {
                AtomicBoolean called = new AtomicBoolean();

                compacter = (instance, array) -> { called.set(true); return 1; };

                assertEquals(7, array.length);

                manager.add(ArrayManagerTest.this, "F");
                manager.add(ArrayManagerTest.this, "G");
                manager.add(ArrayManagerTest.this, "H");  // triggers grow with default settings

                assertTrue(called.get());
                assertEquals(7, array.length);
                assertElements("A", "B", "C", "D", "E", "F", "H");    // G was dropped by compacter
            }
        }
    }

    void assertElements(String... expecteds) {
        for (int i = 0; i < expecteds.length; i++) {
            String expected = expecteds[i];

            assertEquals(expected, manager.get(ArrayManagerTest.this, i));
        }

        assertEquals(expecteds.length, size);
    }
}
