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

import com.sun.javafx.collections.ObservableMapWrapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ObservableMapWrapperTest {

    @Nested
    class RemoveAllTest {
        @Test
        public void testEntrySetNullArgumentThrowsNPE() {
            var map1 = new ObservableMapWrapper<>(new HashMap<>());
            assertThrows(NullPointerException.class, () -> map1.entrySet().removeAll((Collection<?>) null));

            var map2 = new ObservableMapWrapper<>(new HashMap<>(Map.of("k0", "v0", "k1", "v1", "k2", "v2")));
            assertThrows(NullPointerException.class, () -> map2.entrySet().removeAll((Collection<?>) null));
        }

        @Test
        public void testKeySetNullArgumentThrowsNPE() {
            var map1 = new ObservableMapWrapper<>(new HashMap<>());
            assertThrows(NullPointerException.class, () -> map1.keySet().removeAll((Collection<?>) null));

            var map2 = new ObservableMapWrapper<>(new HashMap<>(Map.of("k0", "v0", "k1", "v1", "k2", "v2")));
            assertThrows(NullPointerException.class, () -> map2.keySet().removeAll((Collection<?>) null));
        }

        @Test
        public void testValueSetNullArgumentThrowsNPE() {
            var map1 = new ObservableMapWrapper<>(new HashMap<>());
            assertThrows(NullPointerException.class, () -> map1.values().removeAll((Collection<?>) null));

            var map2 = new ObservableMapWrapper<>(new HashMap<>(Map.of("k0", "v0", "k1", "v1", "k2", "v2")));
            assertThrows(NullPointerException.class, () -> map2.values().removeAll((Collection<?>) null));
        }

        @Test
        public void testRemoveAllKeysWithEmptyCollectionArgumentDoesNotEnumerateBackingMap() {
            ObservableMapWrapper<String, String> map = newNonIterableObservableMapWrapper();
            map.keySet().removeAll(Collections.<String>emptySet());
        }

        @Test
        public void testRemoveAllValuesWithEmptyCollectionArgumentDoesNotEnumerateBackingMap() {
            ObservableMapWrapper<String, String> map = newNonIterableObservableMapWrapper();
            map.values().removeAll(Collections.<String>emptySet());
        }

        @Test
        public void testRemoveAllEntriesWithEmptyCollectionArgumentDoesNotEnumerateBackingMap() {
            ObservableMapWrapper<String, String> map = newNonIterableObservableMapWrapper();
            map.entrySet().removeAll(Collections.<Map.Entry<String, String>>emptySet());
        }

        @Test
        public void testRemoveAllEntriesWithEmptyCollectionArgumentWorksCorrectly() {
            var content = Map.of("k0", "v0", "k1", "v1", "k2", "v2");
            var map = new ObservableMapWrapper<>(new HashMap<>(content));
            assertFalse(map.entrySet().removeAll(Set.of()));
            assertEquals(content, map);
        }
    }

    @Nested
    class RetainAllTest {
        @Test
        public void testEntrySetNullArgumentThrowsNPE() {
            var map1 = new ObservableMapWrapper<>(new HashMap<>());
            assertThrows(NullPointerException.class, () -> map1.entrySet().retainAll((Collection<?>) null));

            var map2 = new ObservableMapWrapper<>(new HashMap<>(Map.of("k0", "v0", "k1", "v1", "k2", "v2")));
            assertThrows(NullPointerException.class, () -> map2.entrySet().retainAll((Collection<?>) null));
        }

        @Test
        public void testKeySetNullArgumentThrowsNPE() {
            var map1 = new ObservableMapWrapper<>(new HashMap<>());
            assertThrows(NullPointerException.class, () -> map1.keySet().retainAll((Collection<?>) null));

            var map2 = new ObservableMapWrapper<>(new HashMap<>(Map.of("k0", "v0", "k1", "v1", "k2", "v2")));
            assertThrows(NullPointerException.class, () -> map2.keySet().retainAll((Collection<?>) null));
        }

        @Test
        public void testValueSetNullArgumentThrowsNPE() {
            var map1 = new ObservableMapWrapper<>(new HashMap<>());
            assertThrows(NullPointerException.class, () -> map1.values().retainAll((Collection<?>) null));

            var map2 = new ObservableMapWrapper<>(new HashMap<>(Map.of("k0", "v0", "k1", "v1", "k2", "v2")));
            assertThrows(NullPointerException.class, () -> map2.values().retainAll((Collection<?>) null));
        }

        @Test
        public void testRetainAllKeysWithEmptyCollectionArgumentDoesNotCallContains() {
            var map = new ObservableMapWrapper<>(new HashMap<>(Map.of("k0", "v0", "k1", "v1", "k2", "v2")));
            map.keySet().retainAll(newNoContainsHashSet());
        }

        @Test
        public void testRetainAllValuesWithEmptyCollectionArgumentDoesNotCallContains() {
            var map = new ObservableMapWrapper<>(new HashMap<>(Map.of("k0", "v0", "k1", "v1", "k2", "v2")));
            map.values().retainAll(newNoContainsHashSet());
        }

        @Test
        public void testRetainAllEntriesWithEmptyCollectionArgumentDoesNotCallContains() {
            var map = new ObservableMapWrapper<>(new HashMap<>(Map.of("k0", "v0", "k1", "v1", "k2", "v2")));
            map.entrySet().retainAll(newNoContainsHashSet());
        }

        @Test
        public void testRetainAllWithEmptyCollectionArgumentWorksCorrectly() {
            var content = Map.of("k0", "v0", "k1", "v1", "k2", "v2");
            var map = new ObservableMapWrapper<>(new HashMap<>(content));
            assertTrue(map.entrySet().retainAll(Set.of()));
            assertTrue(map.isEmpty());
        }
    }

    private ObservableMapWrapper<String, String> newNonIterableObservableMapWrapper() {
        return new ObservableMapWrapper<>(
            new HashMap<>(Map.of("k0", "v0", "k1", "v1", "k2", "v2")) {
                Set<Entry<String, String>> entrySet;

                @Override
                public Set<Entry<String, String>> entrySet() {
                    if (entrySet == null) {
                        entrySet = new AbstractSet<>() {
                            @Override
                            public Iterator<Entry<String, String>> iterator() {
                                throw new AssertionError("iterator() was not elided");
                            }

                            @Override
                            public int size() {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }
                    return entrySet;
                }
            });
    }

    @SuppressWarnings("rawtypes")
    private HashSet newNoContainsHashSet() {
        return new HashSet() {
            @Override
            public boolean contains(Object o) {
                throw new AssertionError("contains() was not elided");
            }
        };
    }

}
