/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static javafx.collections.MockMapObserver.Tuple.tup;
import static org.junit.Assert.*;

public abstract class UnmodifiableObservableMapTestBase {

    private final Map<String, String> baseMap;
    private ObservableMap<String, String> observableMap;
    private ObservableMap<String, String> unmodifiableMap;
    private MockMapObserver<String, String> observer;


    public UnmodifiableObservableMapTestBase(final Map<String, String> map) {
        baseMap = map;
    }

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        observableMap = FXCollections.observableMap(baseMap);
        unmodifiableMap = FXCollections.unmodifiableObservableMap(observableMap);
        observer = new MockMapObserver<String, String>();
        unmodifiableMap.addListener(observer);
        baseMap.put("one", "1");
        baseMap.put("two", "2");
        baseMap.put("foo", "bar");
    }


    @Test
    public void testObservability() {
        /*
         * Since unmodifiable ObservableMap wraps an ObservableMap, we need to test
         * Observability of changes to the underlying Map, we won't test the full
         * ObservableMap API since that is already done.
         */
        observableMap.put("observedFoo", "barVal");
        observableMap.put("foo", "barfoo");
        assertEquals("barVal", unmodifiableMap.get("observedFoo"));

        observableMap.remove("observedFoo");
        observableMap.remove("foo");
        observableMap.remove("bar");
        observableMap.put("one", "1"); // dup, no change
        assertFalse(unmodifiableMap.containsKey("foo"));

        observer.assertAdded(0, tup("observedFoo", "barVal"));
        observer.assertAdded(1, tup("foo", "barfoo"));
        observer.assertRemoved(1, tup("foo", "bar"));
        observer.assertRemoved(2, tup("observedFoo", "barVal"));
        observer.assertRemoved(3, tup("foo", "barfoo"));
        assertEquals(observer.getCallsNumber(), 4);
    }

    @Test
    public void testPutAll() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("oFoo", "OFoo");
        map.put("pFoo", "PFoo");
        map.put("foo", "foofoo");
        map.put("one", "1");
        try {
            unmodifiableMap.putAll(map);
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {}
    }

    @Test
    public void testClear() {
        try {
            unmodifiableMap.clear();
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {}
    }

    @Test
    public void testKeySet() {
        try {
            unmodifiableMap.keySet().remove("one");
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {}
        try {
            unmodifiableMap.keySet().removeAll(Arrays.asList("one", "two", "three"));
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {}
        try {
            unmodifiableMap.keySet().retainAll(Arrays.asList("one", "two", "three"));
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {}
        try {
            unmodifiableMap.keySet().clear();
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {}
    }

    @Test
    public void testKeySet_Iterator() {
        // iterators should not be removable
        Iterator<String> iterator = unmodifiableMap.keySet().iterator();
        assertTrue("Test error, underlying Map should not be empty!", iterator.hasNext());
        try {
            iterator.remove();
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {}
    }

    @Test
    public void testValues() {
        try {
            unmodifiableMap.values().remove("1");
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {}
        try {
            unmodifiableMap.values().removeAll(Arrays.asList("1", "2", "3"));
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {}
        try {
            unmodifiableMap.values().retainAll(Arrays.asList("1", "2", "3"));
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {}
        try {
            unmodifiableMap.values().clear();
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {}
    }

    @Test
    public void testValues_Iterator() {
        Iterator<String> iterator = unmodifiableMap.values().iterator();
        assertTrue("Test error, underlying Map should not be empty!", iterator.hasNext());
        try {
            iterator.remove();
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {}
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEntrySet() {
        try {
            unmodifiableMap.entrySet().remove(entry("one","1"));
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {}
        try {
            unmodifiableMap.entrySet().removeAll(Arrays.asList(entry("one","1"), entry("two","2"), entry("three","3")));
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {}
        try {
            unmodifiableMap.entrySet().retainAll(Arrays.asList(entry("one","1"), entry("two","2"), entry("three","3")));
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {}
        try {
            unmodifiableMap.entrySet().clear();
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {}
    }

    @Test
    public void testEntrySet_Iterator() {
        Iterator<Map.Entry<String, String>> iterator = unmodifiableMap.entrySet().iterator();
        assertTrue("Test error, underlying Map should not be empty!", iterator.hasNext());
        try {
            iterator.remove();
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {}
    }

    @Test
    public void testEntrySet_Entry() {
        Map.Entry<String, String> unmodifiable = unmodifiableMap.entrySet().iterator().next();
        Map.Entry<String, String> regular = baseMap.entrySet().iterator().next();

        // Entries should NOT be the same as in the underlying Map object!
        assertNotSame(unmodifiable, regular);
        assertNotSame(regular, unmodifiable);
        assertNotSame(unmodifiable.hashCode(), regular.hashCode());
        try {
            unmodifiable.setValue("newval");
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {}
    }

    private<K, V> Map.Entry<K, V> entry(final K key, final V value) {
        return new Map.Entry<K, V>() {

            @Override
            public K getKey() {
                return key;
            }

            @Override
            public V getValue() {
                return value;
            }

            @Override
            public V setValue(V value) {
                throw new UnsupportedOperationException("Not supported.");
            }

        };
    }
}
