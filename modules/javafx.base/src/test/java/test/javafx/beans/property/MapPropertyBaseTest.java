/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.beans.property;

import test.javafx.beans.InvalidationListenerMock;
import test.javafx.beans.value.ChangeListenerMock;
import javafx.beans.value.ObservableObjectValueStub;
import javafx.collections.FXCollections;
import test.javafx.collections.MockMapObserver;
import javafx.collections.ObservableMap;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import javafx.beans.property.MapProperty;
import javafx.beans.property.MapPropertyBase;
import javafx.beans.property.SimpleMapProperty;

import static test.javafx.collections.MockMapObserver.Call;
import static org.junit.Assert.*;

public class MapPropertyBaseTest {

    private static final Object NO_BEAN = null;
    private static final String NO_NAME_1 = null;
    private static final String NO_NAME_2 = "";
    private static final Object KEY_1b = new Object();
    private static final Object DATA_1b = new Object();
    private static final Object KEY_2a_0 = new Object();
    private static final Object DATA_2a_0 = new Object();
    private static final Object KEY_2a_1 = new Object();
    private static final Object DATA_2a_1 = new Object();
    private static final Object KEY_2b_0 = new Object();
    private static final Object DATA_2b_0 = new Object();
    private static final Object KEY_2b_1 = new Object();
    private static final Object DATA_2b_1 = new Object();
    private static final Object KEY_2b_2 = new Object();
    private static final Object DATA_2b_2 = new Object();
    private static final ObservableMap<Object, Object> UNDEFINED = FXCollections.observableMap(Collections.emptyMap());
    private static final ObservableMap<Object, Object> VALUE_1a = FXCollections.observableMap(Collections.emptyMap());
    private static final ObservableMap<Object, Object> VALUE_1b = FXCollections.observableMap(Collections.singletonMap(KEY_1b, DATA_1b));
    private static final ObservableMap<Object, Object> VALUE_2a = FXCollections.observableMap(new HashMap<Object, Object>());

    static {
        VALUE_2a.put(KEY_2a_0, DATA_2a_0);
        VALUE_2a.put(KEY_2a_1, DATA_2a_1);
    }
    private static final ObservableMap<Object, Object> VALUE_2b = FXCollections.observableMap(new HashMap<Object, Object>());

    static {
        VALUE_2b.put(KEY_2b_0, DATA_2b_0);
        VALUE_2b.put(KEY_2b_1, DATA_2b_1);
        VALUE_2b.put(KEY_2b_2, DATA_2b_2);
    }
    private MapPropertyMock property;
    private InvalidationListenerMock invalidationListener;
    private ChangeListenerMock<ObservableMap<Object, Object>> changeListener;
    private MockMapObserver<Object, Object> mapChangeListener;

    @Before
    public void setUp() throws Exception {
        property = new MapPropertyMock();
        invalidationListener = new InvalidationListenerMock();
        changeListener = new ChangeListenerMock<ObservableMap<Object, Object>>(UNDEFINED);
        mapChangeListener = new MockMapObserver<Object, Object>();
    }

    private void attachInvalidationListener() {
        property.addListener(invalidationListener);
        property.get();
        invalidationListener.reset();
    }

    private void attachChangeListener() {
        property.addListener(changeListener);
        property.get();
        changeListener.reset();
    }

    private void attachMapChangeListener() {
        property.addListener(mapChangeListener);
        property.get();
        mapChangeListener.clear();
    }

    @Test
    public void testConstructor() {
        final MapProperty<Object, Object> p1 = new SimpleMapProperty<Object, Object>();
        assertEquals(null, p1.get());
        assertEquals(null, p1.getValue());
        assertFalse(property.isBound());

        final MapProperty<Object, Object> p2 = new SimpleMapProperty<Object, Object>(VALUE_1b);
        assertEquals(VALUE_1b, p2.get());
        assertEquals(VALUE_1b, p2.getValue());
        assertFalse(property.isBound());
    }

    @Test
    public void testEmptyProperty() {
        assertEquals("empty", property.emptyProperty().getName());
        assertEquals(property, property.emptyProperty().getBean());
        assertTrue(property.emptyProperty().get());

        property.set(VALUE_2a);
        assertFalse(property.emptyProperty().get());
        property.set(VALUE_1a);
        assertTrue(property.emptyProperty().get());
    }

    @Test
    public void testSizeProperty() {
        assertEquals("size", property.sizeProperty().getName());
        assertEquals(property, property.sizeProperty().getBean());
        assertEquals(0, property.sizeProperty().get());

        property.set(VALUE_2a);
        assertEquals(2, property.sizeProperty().get());
        property.set(VALUE_1a);
        assertEquals(0, property.sizeProperty().get());
    }

    @Test
    public void testInvalidationListener() {
        attachInvalidationListener();
        property.set(VALUE_2a);
        invalidationListener.check(property, 1);
        property.removeListener(invalidationListener);
        invalidationListener.reset();
        property.set(VALUE_1a);
        invalidationListener.check(null, 0);
    }

    @Test
    public void testChangeListener() {
        attachChangeListener();
        property.set(VALUE_2a);
        changeListener.check(property, null, VALUE_2a, 1);
        property.removeListener(changeListener);
        changeListener.reset();
        property.set(VALUE_1a);
        changeListener.check(null, UNDEFINED, UNDEFINED, 0);
    }

    @Test
    public void testMapChangeListener() {
        attachMapChangeListener();
        property.set(VALUE_2a);
        mapChangeListener.assertMultipleCalls(new Call[]{new Call(KEY_2a_0, null, DATA_2a_0), new Call(KEY_2a_1, null, DATA_2a_1)});
        property.removeListener(mapChangeListener);
        mapChangeListener.clear();
        property.set(VALUE_1a);
        assertEquals(0, mapChangeListener.getCallsNumber());
    }

    @Test
    public void testSourceMap_Invalidation() {
        final ObservableMap<Object, Object> source1 = FXCollections.observableMap(new HashMap<Object, Object>());
        final ObservableMap<Object, Object> source2 = FXCollections.observableMap(new HashMap<Object, Object>());
        final Object key = new Object();
        final Object value1 = new Object();
        final Object value2 = new Object();

        // constructor
        property = new MapPropertyMock(source1);
        property.reset();
        attachInvalidationListener();

        // add element
        source1.put(key, value1);
        assertEquals(value1, property.get(key));
        property.check(1);
        invalidationListener.check(property, 1);

        // replace element
        source1.put(key, value2);
        assertEquals(value2, property.get(key));
        property.check(1);
        invalidationListener.check(property, 1);

        // remove element
        source1.remove(key);
        assertNull(property.get(key));
        property.check(1);
        invalidationListener.check(property, 1);

        // set
        property.set(source2);
        property.get();
        property.reset();
        invalidationListener.reset();

        // add element
        source2.put(key, value1);
        assertEquals(value1, property.get(key));
        property.check(1);
        invalidationListener.check(property, 1);

        // replace element
        source2.put(key, value2);
        assertEquals(value2, property.get(key));
        property.check(1);
        invalidationListener.check(property, 1);

        // remove element
        source2.remove(key);
        assertNull(property.get(key));
        property.check(1);
        invalidationListener.check(property, 1);
    }

    @Test
    public void testSourceMap_Change() {
        final ObservableMap<Object, Object> source1 = FXCollections.observableMap(new HashMap<Object, Object>());
        final ObservableMap<Object, Object> source2 = FXCollections.observableMap(new HashMap<Object, Object>());
        final Object key = new Object();
        final Object value1 = new Object();
        final Object value2 = new Object();

        // constructor
        property = new MapPropertyMock(source1);
        property.reset();
        attachChangeListener();

        // add element
        source1.put(key, value1);
        assertEquals(value1, property.get(key));
        property.check(1);
        changeListener.check(property, source1, source1, 1);

        // replace element
        source1.put(key, value2);
        assertEquals(value2, property.get(key));
        property.check(1);
        changeListener.check(property, source1, source1, 1);

        // remove element
        source1.remove(key);
        assertNull(property.get(key));
        property.check(1);
        changeListener.check(property, source1, source1, 1);

        // set
        property.set(source2);
        property.get();
        property.reset();
        changeListener.reset();

        // add element
        source2.put(key, value1);
        assertEquals(value1, property.get(key));
        property.check(1);
        changeListener.check(property, source2, source2, 1);

        // replace element
        source2.put(key, value2);
        assertEquals(value2, property.get(key));
        property.check(1);
        changeListener.check(property, source2, source2, 1);

        // remove element
        source2.remove(key);
        assertNull(property.get(key));
        property.check(1);
        changeListener.check(property, source2, source2, 1);
    }

    @Test
    public void testSourceMap_MapChange() {
        final ObservableMap<Object, Object> source1 = FXCollections.observableMap(new HashMap<Object, Object>());
        final ObservableMap<Object, Object> source2 = FXCollections.observableMap(new HashMap<Object, Object>());
        final Object key = new Object();
        final Object value1 = new Object();
        final Object value2 = new Object();

        // constructor
        property = new MapPropertyMock(source1);
        property.reset();
        attachMapChangeListener();

        // add element
        source1.put(key, value1);
        assertEquals(value1, property.get(key));
        property.check(1);
        mapChangeListener.assertAdded(MockMapObserver.Tuple.tup(key, value1));
        mapChangeListener.clear();

        // replace element
        source1.put(key, value2);
        assertEquals(value2, property.get(key));
        property.check(1);
        mapChangeListener.assertMultipleCalls(new Call<Object, Object>(key, value1, value2));
        mapChangeListener.clear();

        // remove element
        source1.remove(key);
        assertNull(property.get(key));
        property.check(1);
        mapChangeListener.assertRemoved(MockMapObserver.Tuple.tup(key, value2));
        mapChangeListener.clear();

        // set
        property.set(source2);
        property.get();
        property.reset();
        mapChangeListener.clear();

        // add element
        source2.put(key, value1);
        assertEquals(value1, property.get(key));
        property.check(1);
        mapChangeListener.assertAdded(MockMapObserver.Tuple.tup(key, value1));
        mapChangeListener.clear();

        // replace element
        source2.put(key, value2);
        assertEquals(value2, property.get(key));
        property.check(1);
        mapChangeListener.assertMultipleCalls(new Call<Object, Object>(key, value1, value2));
        mapChangeListener.clear();

        // remove element
        source2.remove(key);
        assertNull(property.get(key));
        property.check(1);
        mapChangeListener.assertRemoved(MockMapObserver.Tuple.tup(key, value2));
        mapChangeListener.clear();
    }

    @Test
    public void testMap_Invalidation() {
        attachInvalidationListener();

        // set value once
        property.set(VALUE_2a);
        assertEquals(VALUE_2a, property.get());
        property.check(1);
        invalidationListener.check(property, 1);

        // set same value again
        property.set(VALUE_2a);
        assertEquals(VALUE_2a, property.get());
        property.check(0);
        invalidationListener.check(null, 0);

        // set value twice without reading
        property.set(VALUE_1a);
        property.set(VALUE_2b);
        assertEquals(VALUE_2b, property.get());
        property.check(1);
        invalidationListener.check(property, 1);
    }

    @Test
    public void testMap_Change() {
        attachChangeListener();

        // set value once
        property.set(VALUE_2a);
        assertEquals(VALUE_2a, property.get());
        property.check(1);
        changeListener.check(property, null, VALUE_2a, 1);

        // set same value again
        property.set(VALUE_2a);
        assertEquals(VALUE_2a, property.get());
        property.check(0);
        changeListener.check(null, UNDEFINED, UNDEFINED, 0);

        // set value twice without reading
        property.set(VALUE_1a);
        property.set(VALUE_1b);
        assertEquals(VALUE_1b, property.get());
        property.check(2);
        changeListener.check(property, VALUE_1a, VALUE_1b, 2);
    }

    @Test
    public void testMap_MapChange() {
        attachMapChangeListener();

        // set value once
        property.set(VALUE_2a);
        assertEquals(VALUE_2a, property.get());
        property.check(1);
        mapChangeListener.assertMultipleCalls(new Call[]{new Call(KEY_2a_0, null, DATA_2a_0), new Call(KEY_2a_1, null, DATA_2a_1)});

        // set same value again
        mapChangeListener.clear();
        property.set(VALUE_2a);
        assertEquals(VALUE_2a, property.get());
        property.check(0);
        assertEquals(0, mapChangeListener.getCallsNumber());

        // set value twice without reading
        property.set(VALUE_1a);
        mapChangeListener.clear();
        property.set(VALUE_1b);
        assertEquals(VALUE_1b, property.get());
        property.check(2);
        mapChangeListener.assertAdded(MockMapObserver.Tuple.tup(KEY_1b, DATA_1b));
    }

    @Test
    public void testMapValue_Invalidation() {
        attachInvalidationListener();

        // set value once
        property.setValue(VALUE_2a);
        assertEquals(VALUE_2a, property.get());
        property.check(1);
        invalidationListener.check(property, 1);

        // set same value again
        property.setValue(VALUE_2a);
        assertEquals(VALUE_2a, property.get());
        property.check(0);
        invalidationListener.check(null, 0);

        // set value twice without reading
        property.setValue(VALUE_1a);
        property.setValue(VALUE_1b);
        assertEquals(VALUE_1b, property.get());
        property.check(1);
        invalidationListener.check(property, 1);
    }

    @Test
    public void testMapValue_Change() {
        attachChangeListener();

        // set value once
        property.setValue(VALUE_2a);
        assertEquals(VALUE_2a, property.get());
        property.check(1);
        changeListener.check(property, null, VALUE_2a, 1);

        // set same value again
        property.setValue(VALUE_2a);
        assertEquals(VALUE_2a, property.get());
        property.check(0);
        changeListener.check(null, UNDEFINED, UNDEFINED, 0);

        // set value twice without reading
        property.setValue(VALUE_1a);
        property.setValue(VALUE_1b);
        assertEquals(VALUE_1b, property.get());
        property.check(2);
        changeListener.check(property, VALUE_1a, VALUE_1b, 2);
    }

    @Test
    public void testMapValue_MapChange() {
        attachMapChangeListener();

        // set value once
        property.setValue(VALUE_2a);
        assertEquals(VALUE_2a, property.get());
        property.check(1);
        mapChangeListener.assertMultipleCalls(new Call[]{new Call(KEY_2a_0, null, DATA_2a_0), new Call(KEY_2a_1, null, DATA_2a_1)});

        // set same value again
        mapChangeListener.clear();
        property.setValue(VALUE_2a);
        assertEquals(VALUE_2a, property.get());
        property.check(0);
        assertEquals(0, mapChangeListener.getCallsNumber());

        // set value twice without reading
        property.setValue(VALUE_1a);
        mapChangeListener.clear();
        property.setValue(VALUE_1b);
        assertEquals(VALUE_1b, property.get());
        property.check(2);
        mapChangeListener.assertAdded(MockMapObserver.Tuple.tup(KEY_1b, DATA_1b));
    }

    @Test(expected = RuntimeException.class)
    public void testMapBoundValue() {
        final MapProperty<Object, Object> v = new SimpleMapProperty<Object, Object>(VALUE_1a);
        property.bind(v);
        property.set(VALUE_1a);
    }

    @Test
    public void testBind_Invalidation() {
        attachInvalidationListener();
        final ObservableObjectValueStub<ObservableMap<Object, Object>> v = new ObservableObjectValueStub<ObservableMap<Object, Object>>(FXCollections.observableMap(VALUE_1a));

        property.bind(v);
        assertEquals(VALUE_1a, property.get());
        assertTrue(property.isBound());
        property.check(1);
        invalidationListener.check(property, 1);

        // change binding once
        v.set(VALUE_2a);
        assertEquals(VALUE_2a, property.get());
        property.check(1);
        invalidationListener.check(property, 1);

        // change binding twice without reading
        v.set(VALUE_1a);
        v.set(VALUE_1b);
        assertEquals(VALUE_1b, property.get());
        property.check(1);
        invalidationListener.check(property, 1);

        // change binding twice to same value
        v.set(VALUE_1a);
        v.set(VALUE_1a);
        assertEquals(VALUE_1a, property.get());
        property.check(1);
        invalidationListener.check(property, 1);
    }

    @Test
    public void testBind_Change() {
        attachChangeListener();
        final ObservableObjectValueStub<ObservableMap<Object, Object>> v = new ObservableObjectValueStub<ObservableMap<Object, Object>>(FXCollections.observableMap(VALUE_1a));

        property.bind(v);
        assertEquals(VALUE_1a, property.get());
        assertTrue(property.isBound());
        property.check(1);
        changeListener.check(property, null, VALUE_1a, 1);

        // change binding once
        v.set(VALUE_2a);
        assertEquals(VALUE_2a, property.get());
        property.check(1);
        changeListener.check(property, VALUE_1a, VALUE_2a, 1);

        // change binding twice without reading
        v.set(VALUE_1a);
        v.set(VALUE_1b);
        assertEquals(VALUE_1b, property.get());
        property.check(2);
        changeListener.check(property, VALUE_1a, VALUE_1b, 2);

        // change binding twice to same value
        v.set(VALUE_1a);
        v.set(VALUE_1a);
        assertEquals(VALUE_1a, property.get());
        property.check(2);
        changeListener.check(property, VALUE_1b, VALUE_1a, 1);
    }

    @Test
    public void testBind_MapChange() {
        attachMapChangeListener();
        final ObservableObjectValueStub<ObservableMap<Object, Object>> v = new ObservableObjectValueStub<ObservableMap<Object, Object>>(FXCollections.observableMap(VALUE_1a));

        property.bind(v);
        assertEquals(VALUE_1a, property.get());
        assertTrue(property.isBound());
        property.check(1);
        assertEquals(0, mapChangeListener.getCallsNumber());

        // change binding once
        mapChangeListener.clear();
        v.set(VALUE_2a);
        assertEquals(VALUE_2a, property.get());
        property.check(1);
        mapChangeListener.assertMultipleCalls(new Call[]{new Call(KEY_2a_0, null, DATA_2a_0), new Call(KEY_2a_1, null, DATA_2a_1)});

        // change binding twice without reading
        v.set(VALUE_1a);
        mapChangeListener.clear();
        v.set(VALUE_1b);
        assertEquals(VALUE_1b, property.get());
        property.check(2);
        mapChangeListener.assertAdded(MockMapObserver.Tuple.tup(KEY_1b, DATA_1b));

        // change binding twice to same value
        v.set(VALUE_1a);
        mapChangeListener.clear();
        v.set(VALUE_1a);
        assertEquals(VALUE_1a, property.get());
        property.check(2);
        assertEquals(0, mapChangeListener.getCallsNumber());
    }

    @Test(expected = NullPointerException.class)
    public void testBindToNull() {
        property.bind(null);
    }

    @Test
    public void testRebind() {
        attachInvalidationListener();
        final MapProperty<Object, Object> v1 = new SimpleMapProperty<Object, Object>(VALUE_1a);
        final MapProperty<Object, Object> v2 = new SimpleMapProperty<Object, Object>(VALUE_2a);
        property.bind(v1);
        property.get();
        property.reset();
        invalidationListener.reset();

        // rebind causes invalidation event
        property.bind(v2);
        assertEquals(VALUE_2a, property.get());
        assertTrue(property.isBound());
        assertEquals(1, property.counter);
        invalidationListener.check(property, 1);
        property.reset();

        // change old binding
        v1.set(VALUE_1b);
        assertEquals(VALUE_2a, property.get());
        assertEquals(0, property.counter);
        invalidationListener.check(null, 0);
        property.reset();

        // change new binding
        v2.set(VALUE_2b);
        assertEquals(VALUE_2b, property.get());
        assertEquals(1, property.counter);
        invalidationListener.check(property, 1);
        property.reset();

        // rebind to same observable should have no effect
        property.bind(v2);
        assertEquals(VALUE_2b, property.get());
        assertTrue(property.isBound());
        assertEquals(0, property.counter);
        invalidationListener.check(null, 0);
    }

    @Test
    public void testRebind_Identity() {
        final MapProperty<Object, Object> v1 = new SimpleMapProperty<>(FXCollections.observableHashMap());
        final MapProperty<Object, Object> v2 = new SimpleMapProperty<>(FXCollections.observableHashMap());
        attachMapChangeListener();

        // bind
        property.bind(v1);
        property.check(1);
        mapChangeListener.clear();

        // rebind to same
        property.bind(v1);
        property.check(0);
        mapChangeListener.check0();

        // rebind to other, without explicitly unbinding
        property.bind(v2);
        property.check(1);
        mapChangeListener.clear();

        v2.put("One", "1");
        mapChangeListener.assertAdded(MockMapObserver.Tuple.tup("One", "1"));
        mapChangeListener.clear();

        v2.put("Two", "2");
        mapChangeListener.assertAdded(MockMapObserver.Tuple.tup("Two", "2"));
        mapChangeListener.clear();

        property.check(4);
        assertTrue(property.isBound());
        assertEquals(2, property.size());
        assertEquals("MapProperty [bound, value: {One=1, Two=2}]", property.toString());
    }

    @Test
    public void testUnbind() {
        attachInvalidationListener();
        final MapProperty<Object, Object> v = new SimpleMapProperty<Object, Object>(VALUE_1a);
        property.bind(v);
        property.unbind();
        assertEquals(VALUE_1a, property.get());
        assertFalse(property.isBound());
        property.reset();
        invalidationListener.reset();

        // change binding
        v.set(VALUE_2a);
        assertEquals(VALUE_1a, property.get());
        assertEquals(0, property.counter);
        invalidationListener.check(null, 0);
        property.reset();

        // set value
        property.set(VALUE_1b);
        assertEquals(VALUE_1b, property.get());
        assertEquals(1, property.counter);
        invalidationListener.check(property, 1);
    }

    @Test
    public void testAddingListenerWillAlwaysReceiveInvalidationEvent() {
        final MapProperty<Object, Object> v = new SimpleMapProperty<Object, Object>(VALUE_1a);
        final InvalidationListenerMock listener2 = new InvalidationListenerMock();
        final InvalidationListenerMock listener3 = new InvalidationListenerMock();

        // setting the property
        property.set(VALUE_1a);
        property.addListener(listener2);
        listener2.reset();
        property.set(VALUE_1b);
        listener2.check(property, 1);

        // binding the property
        property.bind(v);
        v.set(VALUE_2a);
        property.addListener(listener3);
        v.get();
        listener3.reset();
        v.set(VALUE_2b);
        listener3.check(property, 1);
    }

    @Test
    public void testToString() {
        final ObservableMap<Object, Object> value0 = null;
        final ObservableMap<Object, Object> value1 = FXCollections.observableMap(new HashMap<Object, Object>());
        value1.put(new Object(), new Object());
        value1.put(new Object(), new Object());
        final ObservableMap<Object, Object> value2 = FXCollections.observableMap(new HashMap<Object, Object>());
        final MapProperty<Object, Object> v = new SimpleMapProperty<Object, Object>(value2);

        property.set(value1);
        assertEquals("MapProperty [value: " + value1 + "]", property.toString());

        property.bind(v);
        assertEquals("MapProperty [bound, invalid]", property.toString());
        property.get();
        assertEquals("MapProperty [bound, value: " + value2 + "]", property.toString());
        v.set(value1);
        assertEquals("MapProperty [bound, invalid]", property.toString());
        property.get();
        assertEquals("MapProperty [bound, value: " + value1 + "]", property.toString());

        final Object bean = new Object();
        final String name = "My name";
        final MapProperty<Object, Object> v1 = new MapPropertyMock(bean, name);
        assertEquals("MapProperty [bean: " + bean.toString() + ", name: My name, value: " + null + "]", v1.toString());
        v1.set(value1);
        assertEquals("MapProperty [bean: " + bean.toString() + ", name: My name, value: " + value1 + "]", v1.toString());
        v1.set(value0);
        assertEquals("MapProperty [bean: " + bean.toString() + ", name: My name, value: " + value0 + "]", v1.toString());

        final MapProperty<Object, Object> v2 = new MapPropertyMock(bean, NO_NAME_1);
        assertEquals("MapProperty [bean: " + bean.toString() + ", value: " + null + "]", v2.toString());
        v2.set(value1);
        assertEquals("MapProperty [bean: " + bean.toString() + ", value: " + value1 + "]", v2.toString());
        v1.set(value0);
        assertEquals("MapProperty [bean: " + bean.toString() + ", name: My name, value: " + value0 + "]", v1.toString());

        final MapProperty<Object, Object> v3 = new MapPropertyMock(bean, NO_NAME_2);
        assertEquals("MapProperty [bean: " + bean.toString() + ", value: " + null + "]", v3.toString());
        v3.set(value1);
        assertEquals("MapProperty [bean: " + bean.toString() + ", value: " + value1 + "]", v3.toString());
        v1.set(value0);
        assertEquals("MapProperty [bean: " + bean.toString() + ", name: My name, value: " + value0 + "]", v1.toString());

        final MapProperty<Object, Object> v4 = new MapPropertyMock(NO_BEAN, name);
        assertEquals("MapProperty [name: My name, value: " + null + "]", v4.toString());
        v4.set(value1);
        v1.set(value0);
        assertEquals("MapProperty [bean: " + bean.toString() + ", name: My name, value: " + value0 + "]", v1.toString());
        assertEquals("MapProperty [name: My name, value: " + value1 + "]", v4.toString());
    }

    private static class MapPropertyMock extends MapPropertyBase<Object, Object> {

        private final Object bean;
        private final String name;
        private int counter;

        private MapPropertyMock() {
            this.bean = NO_BEAN;
            this.name = NO_NAME_1;
        }

        private MapPropertyMock(Object bean, String name) {
            this.bean = bean;
            this.name = name;
        }

        private MapPropertyMock(ObservableMap<Object, Object> initialValue) {
            super(initialValue);
            this.bean = NO_BEAN;
            this.name = NO_NAME_1;
        }

        @Override
        protected void invalidated() {
            counter++;
        }

        private void check(int expected) {
            assertEquals(expected, counter);
            reset();
        }

        private void reset() {
            counter = 0;
        }

        @Override
        public Object getBean() {
            return bean;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
