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

package test.com.sun.javafx.collections;

import com.sun.javafx.collections.ObservableSetWrapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ObservableSetWrapperTest {

    @Nested
    class RemoveAllTest {
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

}
