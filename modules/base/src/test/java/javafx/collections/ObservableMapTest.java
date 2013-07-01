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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static javafx.collections.MockMapObserver.Call.call;
import static javafx.collections.MockMapObserver.Tuple.tup;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ObservableMapTest {

    final Callable<ObservableMap<String, String>> mapFactory;
    private ObservableMap<String, String> observableMap;
    private MockMapObserver<String, String> observer;


    public ObservableMapTest(final Callable<ObservableMap<String, String>> mapFactory) {
        this.mapFactory = mapFactory;
    }

    @Parameterized.Parameters
    public static Collection createParameters() {
        Object[][] data = new Object[][] {
            { TestedObservableMaps.HASH_MAP },
            { TestedObservableMaps.TREE_MAP },
            { TestedObservableMaps.LINKED_HASH_MAP },
            { TestedObservableMaps.CONCURRENT_HASH_MAP },
            { TestedObservableMaps.CHECKED_OBSERVABLE_HASH_MAP },
            { TestedObservableMaps.SYNCHRONIZED_OBSERVABLE_HASH_MAP },
            { TestedObservableMaps.OBSERVABLE_MAP_PROPERTY }
         };
        return Arrays.asList(data);
    }

    @Before
    public void setUp() throws Exception {
        observableMap = mapFactory.call();
        observer = new MockMapObserver<String, String>();
        observableMap.addListener(observer);

        useMapData();
    }

    /**
     * Modifies the map in the fixture to use the strings passed in instead of
     * the default strings, and re-creates the observable map and the observer.
     * If no strings are passed in, the result is an empty map.
     *
     * @param strings the strings to use for the list in the fixture
     */
    void useMapData(String... strings) {
        observableMap.clear();
        observableMap.put("one", "1");
        observableMap.put("two", "2");
        observableMap.put("foo", "bar");
        observer.clear();
    }

    @Test
    public void testPutRemove() {
        observableMap.put("observedFoo", "barVal");
        observableMap.put("foo", "barfoo");
        assertEquals("barVal", observableMap.get("observedFoo"));

        observableMap.remove("observedFoo");
        observableMap.remove("foo");
        observableMap.remove("bar");
        observableMap.put("one", "1");

        assertFalse(observableMap.containsKey("foo"));

        observer.assertAdded(0, tup("observedFoo", "barVal"));
        observer.assertAdded(1, tup("foo", "barfoo"));
        observer.assertRemoved(1, tup("foo", "bar"));
        observer.assertRemoved(2, tup("observedFoo", "barVal"));
        observer.assertRemoved(3, tup("foo", "barfoo"));

        assertEquals(observer.getCallsNumber(), 4);
    }

    @Test
    public void testPutRemove_Null() {
        if (mapFactory instanceof TestedObservableMaps.CallableConcurrentHashMapImpl) {
            return; // Do not perform on ConcurrentHashMap, as it doesn't accept nulls
        }
        observableMap.clear();
        observer.clear();

        observableMap.put("bar", null);
        observableMap.put("foo", "x");
        observableMap.put("bar", "x");
        observableMap.put("foo", null);

        assertEquals(2, observableMap.size());
        
        observableMap.remove("bar");
        observableMap.remove("foo");

        assertEquals(0, observableMap.size());

        observer.assertAdded(0, tup("bar", (String)null));
        observer.assertAdded(1, tup("foo", "x"));
        observer.assertAdded(2, tup("bar", "x"));
        observer.assertRemoved(2, tup("bar", (String)null));
        observer.assertAdded(3, tup("foo", (String)null));
        observer.assertRemoved(3, tup("foo", "x"));
        observer.assertRemoved(4, tup("bar", "x"));
        observer.assertRemoved(5, tup("foo", (String)null));

        assertEquals(observer.getCallsNumber(), 6);
    }
    
    @Test
    public void testPutRemove_NullKey() {
        if (mapFactory instanceof TestedObservableMaps.CallableConcurrentHashMapImpl ||
                mapFactory instanceof TestedObservableMaps.CallableTreeMapImpl) {
            return; // Do not perform on ConcurrentHashMap and TreeMap, as they doesn't accept null keys
        }

        observableMap.put(null, "abc");

        assertEquals(4, observableMap.size());
        
        observableMap.remove(null);

        assertEquals(3, observableMap.size());

        observer.assertAdded(0, tup((String)null, "abc"));
        observer.assertRemoved(1, tup((String)null, "abc"));

        assertEquals(observer.getCallsNumber(), 2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPutAll() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("oFoo", "OFoo");
        map.put("pFoo", "PFoo");
        map.put("foo", "foofoo");
        map.put("one", "1");
        observableMap.putAll(map);

        assertTrue(observableMap.containsKey("oFoo"));
        observer.assertMultipleCalls(call("oFoo", null, "OFoo"), call("pFoo", null, "PFoo"), call("foo", "bar", "foofoo"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testClear() {
        observableMap.clear();

        assertTrue(observableMap.isEmpty());
        observer.assertMultipleRemoved(tup("one", "1"), tup("two", "2"), tup("foo", "bar"));

    }

    @Test
    public void testOther() {
        assertEquals(3, observableMap.size());
        assertFalse(observableMap.isEmpty());

        assertTrue(observableMap.containsKey("foo"));
        assertFalse(observableMap.containsKey("bar"));

        assertFalse(observableMap.containsValue("foo"));
        assertTrue(observableMap.containsValue("bar"));
    }

    @Test
    public void testKeySet_Remove() {
        observableMap.keySet().remove("one");
        observableMap.keySet().remove("two");
        observableMap.keySet().remove("three");

        observer.assertRemoved(0, tup("one", "1"));
        observer.assertRemoved(1, tup("two", "2"));
        assertTrue(observer.getCallsNumber() == 2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testKeySet_RemoveAll() {
        observableMap.keySet().removeAll(Arrays.asList("one", "two", "three"));

        observer.assertMultipleRemoved(tup("one", "1"), tup("two", "2"));
        assertTrue(observableMap.size() == 1);
    }

    @Test
    public void testKeySet_RetainAll() {
        observableMap.keySet().retainAll(Arrays.asList("one", "two", "three"));

        observer.assertRemoved(tup("foo", "bar"));
        assertTrue(observableMap.size() == 2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testKeySet_Clear() {
        observableMap.keySet().clear();
        assertTrue(observableMap.keySet().isEmpty());
        observer.assertMultipleRemoved(tup("one", "1"), tup("two", "2"), tup("foo", "bar"));
    }

    @Test
    public void testKeySet_Iterator() {
        Iterator<String> iterator = observableMap.keySet().iterator();
        assertTrue(iterator.hasNext());

        String toBeRemoved = iterator.next();
        String toBeRemovedVal = observableMap.get(toBeRemoved);
        iterator.remove();

        assertTrue(observableMap.size() == 2);
        observer.assertRemoved(tup(toBeRemoved, toBeRemovedVal));
    }

    @Test
    public void testKeySet_Other() {
        assertEquals(3, observableMap.keySet().size());
        assertTrue(observableMap.keySet().contains("foo"));
        assertFalse(observableMap.keySet().contains("bar"));

        assertTrue(observableMap.keySet().containsAll(Arrays.asList("one", "two")));
        assertFalse(observableMap.keySet().containsAll(Arrays.asList("one", "three")));

        assertTrue(observableMap.keySet().toArray(new String[0]).length == 3);
        assertTrue(observableMap.keySet().toArray().length == 3);
    }

    @Test
    public void testValues_Remove() {
        observableMap.values().remove("1");
        observableMap.values().remove("2");
        observableMap.values().remove("3");

        observer.assertRemoved(0, tup("one", "1"));
        observer.assertRemoved(1, tup("two", "2"));
        assertTrue(observer.getCallsNumber() == 2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testValues_RemoveAll() {
        observableMap.values().removeAll(Arrays.asList("1", "2", "3"));

        observer.assertMultipleRemoved(tup("one", "1"), tup("two", "2"));
        assertTrue(observableMap.size() == 1);
    }

    @Test
    public void testValues_RetainAll() {
        observableMap.values().retainAll(Arrays.asList("1", "2", "3"));

        observer.assertRemoved(tup("foo", "bar"));
        assertTrue(observableMap.size() == 2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testValues_Clear() {
        observableMap.values().clear();
        assertTrue(observableMap.values().isEmpty());
        observer.assertMultipleRemoved(tup("one", "1"), tup("two", "2"), tup("foo", "bar"));
    }

    @Test
    public void testValues_Iterator() {
        Iterator<String> iterator = observableMap.values().iterator();
        assertTrue(iterator.hasNext());

        String toBeRemovedVal = iterator.next();
        iterator.remove();

        assertTrue(observableMap.size() == 2);
        observer.assertRemoved(tup(toBeRemovedVal.equals("1") ? "one"
                : toBeRemovedVal.equals("2") ? "two"
                : toBeRemovedVal.equals("bar") ? "foo" : null, toBeRemovedVal));
    }

    @Test
    public void testValues_Other() {
        assertEquals(3, observableMap.values().size());
        assertFalse(observableMap.values().contains("foo"));
        assertTrue(observableMap.values().contains("bar"));

        assertTrue(observableMap.values().containsAll(Arrays.asList("1", "2")));
        assertFalse(observableMap.values().containsAll(Arrays.asList("1", "3")));

        assertTrue(observableMap.values().toArray(new String[0]).length == 3);
        assertTrue(observableMap.values().toArray().length == 3);
    }

    @Test
    public void testEntrySet_Remove() {
        observableMap.entrySet().remove(entry("one","1"));
        observableMap.entrySet().remove(entry("two","2"));
        observableMap.entrySet().remove(entry("three","3"));

        observer.assertRemoved(0, tup("one", "1"));
        observer.assertRemoved(1, tup("two", "2"));
        assertTrue(observer.getCallsNumber() == 2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEntrySet_RemoveAll() {
        observableMap.entrySet().removeAll(Arrays.asList(entry("one","1"), entry("two","2"), entry("three","3")));

        observer.assertMultipleRemoved(tup("one", "1"), tup("two", "2"));
        assertTrue(observableMap.size() == 1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEntrySet_RetainAll() {
        observableMap.entrySet().retainAll(Arrays.asList(entry("one","1"), entry("two","2"), entry("three","3")));

        observer.assertRemoved(tup("foo", "bar"));
        assertTrue(observableMap.size() == 2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEntrySet_Clear() {
        observableMap.entrySet().clear();
        assertTrue(observableMap.entrySet().isEmpty());
        observer.assertMultipleRemoved(tup("one", "1"), tup("two", "2"), tup("foo", "bar"));
    }

    @Test
    public void testEntrySet_Iterator() {
        Iterator<Map.Entry<String, String>> iterator = observableMap.entrySet().iterator();
        assertTrue(iterator.hasNext());

        Map.Entry<String, String> toBeRemoved = iterator.next();
        String toBeRemovedKey = toBeRemoved.getKey();
        String toBeRemovedVal = toBeRemoved.getValue();

        iterator.remove();

        assertTrue(observableMap.size() == 2);
        observer.assertRemoved(tup(toBeRemovedKey, toBeRemovedVal));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEntrySet_Other() {
        assertEquals(3, observableMap.entrySet().size());
        assertTrue(observableMap.entrySet().contains(entry("foo", "bar")));
        assertFalse(observableMap.entrySet().contains(entry("bar", "foo")));

        assertTrue(observableMap.entrySet().containsAll(Arrays.asList(entry("one","1"), entry("two","2"))));
        assertFalse(observableMap.entrySet().containsAll(Arrays.asList(entry("one","1"), entry("three","3"))));

        assertTrue(observableMap.entrySet().toArray(new Map.Entry[0]).length == 3);
        assertTrue(observableMap.entrySet().toArray().length == 3);
    }
    
    @Test
    public void testEqualsAndHashCode() {
        final Map<String, String> other = new HashMap<>(observableMap);
        assertTrue(observableMap.equals(other));
        assertEquals(observableMap.hashCode(), other.hashCode());
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
            
            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof Map.Entry)) {
                    return false;
                }
                Map.Entry entry = (Map.Entry)obj;
                return (getKey()==null ?
                    entry.getKey()==null : getKey().equals(entry.getKey()))  &&
                    (getValue()==null ?
                    entry.getValue()==null : getValue().equals(entry.getValue()));
            }
            
            @Override
            public int hashCode() {
                return (getKey()==null   ? 0 : getKey().hashCode()) ^
                    (getValue()==null ? 0 : getValue().hashCode());
            }

        };
    }
}
