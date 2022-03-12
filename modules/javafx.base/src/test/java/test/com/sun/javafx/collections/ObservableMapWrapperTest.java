/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
import org.junit.jupiter.api.Test;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ObservableMapWrapperTest {

    @Test
    public void testRemoveAllKeysWithEmptyArgumentDoesNotEnumerateBackingMap() {
        ObservableMapWrapper<String, String> set = newNonIterableObservableMapWrapper();
        set.keySet().removeAll(Collections.<String>emptySet());
    }

    @Test
    public void testRemoveAllValuesWithEmptyArgumentDoesNotEnumerateBackingMap() {
        ObservableMapWrapper<String, String> set = newNonIterableObservableMapWrapper();
        set.values().removeAll(Collections.<String>emptySet());
    }

    @Test
    public void testRemoveAllEntriesWithEmptyArgumentDoesNotEnumerateBackingMap() {
        ObservableMapWrapper<String, String> set = newNonIterableObservableMapWrapper();
        set.entrySet().removeAll(Collections.<Map.Entry<String, String>>emptySet());
    }

    private ObservableMapWrapper<String, String> newNonIterableObservableMapWrapper() {
        return new ObservableMapWrapper<>(
            new HashMap<>(Map.of("k0", "v0", "k1", "v1", "k2", "v2")) {
                Set<Entry<String, String>> entrySet;

                @Override
                public Set<Entry<String, String>> entrySet() {
                    if (entrySet == null) {
                        entrySet = new AbstractSet<Entry<String, String>>() {
                            @Override public Iterator<Entry<String, String>> iterator() {
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

    @Test
    public void testRetainAllKeysWithEmptyArgumentDoesNotCallContains() {
        ObservableMapWrapper<String, String> set = new ObservableMapWrapper<>(
            new HashMap<>(Map.of("k0", "v0", "k1", "v1", "k2", "v2")));

        set.keySet().retainAll(newNoContainsHashSet());
    }

    @Test
    public void testRetainAllValuesWithEmptyArgumentDoesNotCallContains() {
        ObservableMapWrapper<String, String> set = new ObservableMapWrapper<>(
                new HashMap<>(Map.of("k0", "v0", "k1", "v1", "k2", "v2")));

        set.values().retainAll(newNoContainsHashSet());
    }

    @Test
    public void testRetainAllEntriesWithEmptyArgumentDoesNotCallContains() {
        ObservableMapWrapper<String, String> set = new ObservableMapWrapper<>(
                new HashMap<>(Map.of("k0", "v0", "k1", "v1", "k2", "v2")));

        set.entrySet().retainAll(newNoContainsHashSet());
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
