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

package test.com.sun.javafx.collections;

import com.sun.javafx.collections.ObservableSetWrapper;
import javafx.collections.SetChangeListener;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ObservableSetWrapperTest {

    @Test
    public void partialChangeIterationCausesSubsequentListenerInvocation() {
        var trace = new ArrayList<String>();
        var invocations = new int[1];
        var set = new ObservableSetWrapper<String>(new HashSet<>());

        // This listener only processes 2 changes in each invocation.
        set.addListener((SetChangeListener<String>) change -> {
            invocations[0]++;
            trace.add(change.toString());

            change = change.next();
            trace.add(change.toString());
        });

        set.addAll(List.of("a", "b", "c", "d", "e", "f"));
        assertEquals(3, invocations[0]);
        assertEquals(List.of("added a", "added b", "added c", "added d", "added e", "added f"), trace);
    }

    @Nested
    class AddAllTest {
        @Test
        public void duplicateElement() {
            var set = new TestObservableSetWrapper(Set.of("a", "b", "c"));
            set.addAll(Set.of("b"));
            set.assertTraceEquals();
        }

        @Test
        public void singleElement() {
            var set = new TestObservableSetWrapper(Set.of("a", "b", "c"));
            set.addAll(Set.of("d"));
            set.assertTraceEquals("added d");
        }

        @Test
        public void multipleElements() {
            var set = new TestObservableSetWrapper(Set.of("a", "b", "c"));
            set.addAll(Set.of("b", "c", "d", "e"));
            set.assertTraceEquals("added d", "added e");
        }
    }

    @Nested
    class RemoveAllTest {
        @Test
        public void singleElement() {
            var set = new TestObservableSetWrapper(Set.of("a", "b", "c"));
            set.removeAll(Set.of("b"));
            set.assertTraceEquals("removed b");
        }

        @Test
        public void multipleElements() {
            var set = new TestObservableSetWrapper(Set.of("a", "b", "c"));
            set.removeAll(Set.of("a", "b", "c"));
            set.assertTraceEquals("removed a", "removed b", "removed c");
        }

        @Test
        public void testNullArgumentThrowsNPE() {
            var set = new ObservableSetWrapper<>(Set.of("a", "b", "c"));
            assertThrows(NullPointerException.class, () -> set.removeAll(null));
        }

        @Test
        public void testEmptyCollectionArgumentDoesNotEnumerateBackingSet() {
            ObservableSetWrapper<String> set = new ObservableSetWrapper<>(new HashSet<>(Set.of("a", "b", "c")) {
                @Override
                public Iterator<String> iterator() {
                    throw new AssertionError("iterator() was not elided");
                }
            });

            set.removeAll(Set.of());
        }

        @Test
        public void testEmptyCollectionArgumentWorksCorrectly() {
            var set = new ObservableSetWrapper<>(new HashSet<>(Set.of("a", "b", "c")));
            assertFalse(set.removeAll(Set.of()));
            assertEquals(Set.of("a", "b", "c"), set);
        }
    }

    @Nested
    class RetainAllTest {
        @Test
        public void singleElement() {
            var set = new TestObservableSetWrapper(Set.of("a", "b", "c"));
            set.retainAll(Set.of("b"));
            set.assertTraceEquals("removed a", "removed c");
        }

        @Test
        public void multipleElements() {
            var set = new TestObservableSetWrapper(Set.of("a", "b", "c"));
            set.retainAll(Set.of("a", "c"));
            set.assertTraceEquals("removed b");
        }

        @Test
        public void testNullArgumentThrowsNPE() {
            var set = new ObservableSetWrapper<>(Set.of("a", "b", "c"));
            assertThrows(NullPointerException.class, () -> set.retainAll(null));
        }

        @Test
        public void testEmptyCollectionArgumentDoesNotCallContains() {
            ObservableSetWrapper<String> set = new ObservableSetWrapper<>(new HashSet<>(Set.of("a", "b", "c")));
            set.removeAll(new HashSet<String>() {
                @Override
                public boolean contains(Object o) {
                    throw new AssertionError("contains() was not elided");
                }
            });
        }

        @Test
        public void testEmptyCollectionArgumentWorksCorrectly() {
            var set = new ObservableSetWrapper<>(new HashSet<>(Set.of("a", "b", "c")));
            assertTrue(set.retainAll(Set.of()));
            assertTrue(set.isEmpty());
        }
    }

    private static class TestObservableSetWrapper extends ObservableSetWrapper<String> {
        final Set<String> bulkChangeTrace = new HashSet<>();
        final Set<String> singleChangeTrace = new HashSet<>();

        public TestObservableSetWrapper(Set<String> set) {
            super(new HashSet<>(set));

            addListener((SetChangeListener<String>) change -> {
                do {
                    bulkChangeTrace.add(change.toString());
                } while ((change = change.next()) != null);
            });

            addListener((SetChangeListener<String>) change -> {
                singleChangeTrace.add(change.toString());
            });
        }

        void assertTraceEquals(String... expected) {
            var expectedSet = Set.of(expected);
            assertEquals(expectedSet, bulkChangeTrace);
            assertEquals(expectedSet, singleChangeTrace);
        }
    }
}
