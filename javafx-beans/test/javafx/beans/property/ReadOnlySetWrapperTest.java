/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package javafx.beans.property;


import javafx.beans.InvalidationListenerMock;
import javafx.beans.value.ChangeListenerMock;
import javafx.beans.value.ObservableObjectValueStub;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReadOnlySetWrapperTest {

    private static final Object UNDEFINED = null;
    private static final ObservableSet<Object> DEFAULT = null;
    private static final ObservableSet<Object> VALUE_1 = FXCollections.observableSet();
    private static final ObservableSet<Object> VALUE_2 = FXCollections.observableSet(new Object());
    
    private ReadOnlySetWrapperMock property;
    private ReadOnlySetProperty<Object> readOnlyProperty;
    private InvalidationListenerMock internalInvalidationListener;
    private InvalidationListenerMock publicInvalidationListener;
    private ChangeListenerMock<Object> internalChangeListener;
    private ChangeListenerMock<Object> publicChangeListener;
    
    @Before
    public void setUp() throws Exception {
        property = new ReadOnlySetWrapperMock();
        readOnlyProperty = property.getReadOnlyProperty();
        internalInvalidationListener = new InvalidationListenerMock();
        publicInvalidationListener = new InvalidationListenerMock();
        internalChangeListener = new ChangeListenerMock<Object>(UNDEFINED);
        publicChangeListener = new ChangeListenerMock<Object>(UNDEFINED);
    }
    
    private void attachInvalidationListeners() {
        property.addListener(internalInvalidationListener);
        readOnlyProperty.addListener(publicInvalidationListener);
        property.get();
        readOnlyProperty.get();
        internalInvalidationListener.reset();
        publicInvalidationListener.reset();
    }

    private void attachInternalChangeListener() {
        property.addListener(internalChangeListener);
        property.get();
        internalChangeListener.reset();
    }

    private void attachPublicChangeListener() {
        readOnlyProperty.addListener(publicChangeListener);
        readOnlyProperty.get();
        publicChangeListener.reset();
    }

    @Test
    public void testConstructor_NoArguments() {
        final ReadOnlySetWrapper<Object> p1 = new ReadOnlySetWrapper<Object>();
        assertEquals(DEFAULT, p1.get());
        assertEquals((Object)DEFAULT, p1.getValue());
        assertFalse(property.isBound());
        assertEquals(null, p1.getBean());
        assertEquals("", p1.getName());
        final ReadOnlySetProperty<Object> r1 = p1.getReadOnlyProperty();
        assertEquals(DEFAULT, r1.get());
        assertEquals((Object)DEFAULT, r1.getValue());
        assertEquals(null, r1.getBean());
        assertEquals("", r1.getName());
    }
    
    @Test
    public void testConstructor_InitialValue() {
        final ReadOnlySetWrapper<Object> p1 = new ReadOnlySetWrapper<Object>(VALUE_1);
        assertEquals(VALUE_1, p1.get());
        assertEquals((Object)VALUE_1, p1.getValue());
        assertFalse(property.isBound());
        assertEquals(null, p1.getBean());
        assertEquals("", p1.getName());
        final ReadOnlySetProperty<Object> r1 = p1.getReadOnlyProperty();
        assertEquals(VALUE_1, r1.get());
        assertEquals((Object)VALUE_1, r1.getValue());
        assertEquals(null, r1.getBean());
        assertEquals("", r1.getName());
    }
    
    @Test
    public void testConstructor_Bean_Name() {
        final Object bean = new Object();
        final String name = "My name";
        final ReadOnlySetWrapper<Object> p1 = new ReadOnlySetWrapper<Object>(bean, name);
        assertEquals(DEFAULT, p1.get());
        assertEquals((Object)DEFAULT, p1.getValue());
        assertFalse(property.isBound());
        assertEquals(bean, p1.getBean());
        assertEquals(name, p1.getName());
        final ReadOnlySetProperty<Object> r1 = p1.getReadOnlyProperty();
        assertEquals(DEFAULT, r1.get());
        assertEquals((Object)DEFAULT, r1.getValue());
        assertEquals(bean, r1.getBean());
        assertEquals(name, r1.getName());
    }
    
    @Test
    public void testConstructor_Bean_Name_InitialValue() {
        final Object bean = new Object();
        final String name = "My name";
        final ReadOnlySetWrapper<Object> p1 = new ReadOnlySetWrapper<Object>(bean, name, VALUE_1);
        assertEquals(VALUE_1, p1.get());
        assertEquals((Object)VALUE_1, p1.getValue());
        assertFalse(property.isBound());
        assertEquals(bean, p1.getBean());
        assertEquals(name, p1.getName());
        final ReadOnlySetProperty<Object> r1 = p1.getReadOnlyProperty();
        assertEquals(VALUE_1, r1.get());
        assertEquals((Object)VALUE_1, r1.getValue());
        assertEquals(bean, r1.getBean());
        assertEquals(name, r1.getName());
    }

    @Test
    public void testLazySet() {
        attachInvalidationListeners();
        
        // set value once
        property.set(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(1);
        internalInvalidationListener.check(readOnlyProperty, 1);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicInvalidationListener.check(readOnlyProperty, 1);
        
        // set same value again
        property.set(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(0);
        internalInvalidationListener.check(null, 0);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicInvalidationListener.check(null, 0);
        
        // set value twice without reading
        property.set(VALUE_2);
        property.set(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(1);
        internalInvalidationListener.check(readOnlyProperty, 1);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicInvalidationListener.check(readOnlyProperty, 1);
    }
    
    @Test
    public void testInternalEagerSet() {
        attachInternalChangeListener();
        
        // set value once
        property.set(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(1);
        internalChangeListener.check(readOnlyProperty, DEFAULT, VALUE_1, 1);
        assertEquals(VALUE_1, readOnlyProperty.get());
        
        // set same value again
        property.set(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(0);
        internalChangeListener.check(null, UNDEFINED, UNDEFINED, 0);
        assertEquals(VALUE_1, readOnlyProperty.get());
        
        // set value twice without reading
        property.set(VALUE_2);
        property.set(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(2);
        internalChangeListener.check(readOnlyProperty, VALUE_2, VALUE_1, 2);
        assertEquals(VALUE_1, readOnlyProperty.get());
    }
    
    @Test
    public void testPublicEagerSet() {
        attachPublicChangeListener();
        
        // set value once
        property.set(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(1);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicChangeListener.check(readOnlyProperty, DEFAULT, VALUE_1, 1);
        
        // set same value again
        property.set(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(0);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicChangeListener.check(null, UNDEFINED, UNDEFINED, 0);
        
        // set value twice without reading
        property.set(VALUE_2);
        property.set(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(2);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicChangeListener.check(readOnlyProperty, VALUE_2, VALUE_1, 2);
    }

    @Test
    public void testLazySetValue() {
        attachInvalidationListeners();
        
        // set value once
        property.setValue(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(1);
        internalInvalidationListener.check(readOnlyProperty, 1);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicInvalidationListener.check(readOnlyProperty, 1);
        
        // set same value again
        property.setValue(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(0);
        internalInvalidationListener.check(null, 0);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicInvalidationListener.check(null, 0);
        
        // set value twice without reading
        property.setValue(VALUE_2);
        property.setValue(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(1);
        internalInvalidationListener.check(readOnlyProperty, 1);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicInvalidationListener.check(readOnlyProperty, 1);
    }
    
    @Test
    public void testInternalEagerSetValue() {
        attachInternalChangeListener();
        
        // set value once
        property.setValue(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(1);
        internalChangeListener.check(readOnlyProperty, DEFAULT, VALUE_1, 1);
        assertEquals(VALUE_1, readOnlyProperty.get());
        
        // set same value again
        property.setValue(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(0);
        internalChangeListener.check(null, UNDEFINED, UNDEFINED, 0);
        assertEquals(VALUE_1, readOnlyProperty.get());
        
        // set value twice without reading
        property.setValue(VALUE_2);
        property.setValue(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(2);
        internalChangeListener.check(readOnlyProperty, VALUE_2, VALUE_1, 2);
        assertEquals(VALUE_1, readOnlyProperty.get());
    }
    
    @Test
    public void testPublicEagerSetValue() {
        attachPublicChangeListener();
        
        // set value once
        property.setValue(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(1);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicChangeListener.check(readOnlyProperty, DEFAULT, VALUE_1, 1);
        
        // set same value again
        property.setValue(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(0);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicChangeListener.check(null, UNDEFINED, UNDEFINED, 0);
        
        // set value twice without reading
        property.setValue(VALUE_2);
        property.setValue(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(2);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicChangeListener.check(readOnlyProperty, VALUE_2, VALUE_1, 2);
    }
    
    @Test(expected=RuntimeException.class)
    public void testSetBoundValue() {
        final SetProperty<Object> v = new SimpleSetProperty<Object>(VALUE_1);
        property.bind(v);
        property.set(VALUE_1);
    }

    @Test
    public void testLazyBind_primitive() {
        attachInvalidationListeners();
        final ObservableObjectValueStub<ObservableSet<Object>> v = new ObservableObjectValueStub<ObservableSet<Object>>(VALUE_1);

        property.bind(v);
        assertEquals(VALUE_1, property.get());
        assertTrue(property.isBound());
        property.check(1);
        internalInvalidationListener.check(readOnlyProperty, 1);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicInvalidationListener.check(readOnlyProperty, 1);

        // change binding once
        v.set(VALUE_2);
        assertEquals(VALUE_2, property.get());
        property.check(1);
        internalInvalidationListener.check(readOnlyProperty, 1);
        assertEquals(VALUE_2, readOnlyProperty.get());
        publicInvalidationListener.check(readOnlyProperty, 1);

        // change binding twice without reading
        v.set(VALUE_1);
        v.set(VALUE_2);
        assertEquals(VALUE_2, property.get());
        property.check(1);
        internalInvalidationListener.check(readOnlyProperty, 1);
        assertEquals(VALUE_2, readOnlyProperty.get());
        publicInvalidationListener.check(readOnlyProperty, 1);

        // change binding twice to same value
        v.set(VALUE_1);
        v.set(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(1);
        internalInvalidationListener.check(readOnlyProperty, 1);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicInvalidationListener.check(readOnlyProperty, 1);
    }

    @Test
    public void testInternalEagerBind_primitive() {
        attachInternalChangeListener();
        final ObservableObjectValueStub<ObservableSet<Object>> v = new ObservableObjectValueStub<ObservableSet<Object>>(VALUE_1);

        property.bind(v);
        assertEquals(VALUE_1, property.get());
        assertTrue(property.isBound());
        property.check(1);
        internalChangeListener.check(readOnlyProperty, DEFAULT, VALUE_1, 1);
        assertEquals(VALUE_1, readOnlyProperty.get());

        // change binding once
        v.set(VALUE_2);
        assertEquals(VALUE_2, property.get());
        property.check(1);
        internalChangeListener.check(readOnlyProperty, VALUE_1, VALUE_2, 1);
        assertEquals(VALUE_2, readOnlyProperty.get());

        // change binding twice without reading
        v.set(VALUE_1);
        v.set(VALUE_2);
        assertEquals(VALUE_2, property.get());
        property.check(2);
        internalChangeListener.check(readOnlyProperty, VALUE_1, VALUE_2, 2);
        assertEquals(VALUE_2, readOnlyProperty.get());

        // change binding twice to same value
        v.set(VALUE_1);
        v.set(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(2);
        internalChangeListener.check(readOnlyProperty, VALUE_2, VALUE_1, 1);
        assertEquals(VALUE_1, readOnlyProperty.get());
    }
    
    @Test
    public void testPublicEagerBind_primitive() {
        attachPublicChangeListener();
        final ObservableObjectValueStub<ObservableSet<Object>> v = new ObservableObjectValueStub<ObservableSet<Object>>(VALUE_1);

        property.bind(v);
        assertEquals(VALUE_1, property.get());
        assertTrue(property.isBound());
        property.check(1);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicChangeListener.check(readOnlyProperty, DEFAULT, VALUE_1, 1);

        // change binding once
        v.set(VALUE_2);
        assertEquals(VALUE_2, property.get());
        property.check(1);
        assertEquals(VALUE_2, readOnlyProperty.get());
        publicChangeListener.check(readOnlyProperty, VALUE_1, VALUE_2, 1);

        // change binding twice without reading
        v.set(VALUE_1);
        v.set(VALUE_2);
        assertEquals(VALUE_2, property.get());
        property.check(2);
        assertEquals(VALUE_2, readOnlyProperty.get());
        publicChangeListener.check(readOnlyProperty, VALUE_1, VALUE_2, 2);

        // change binding twice to same value
        v.set(VALUE_1);
        v.set(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(2);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicChangeListener.check(readOnlyProperty, VALUE_2, VALUE_1, 1);
    }
    
    @Test
    public void testLazyBind_generic() {
        attachInvalidationListeners();
        final ObservableObjectValueStub<ObservableSet<Object>> v = new ObservableObjectValueStub<ObservableSet<Object>>(VALUE_1);

        property.bind(v);
        assertEquals(VALUE_1, property.get());
        assertTrue(property.isBound());
        property.check(1);
        internalInvalidationListener.check(readOnlyProperty, 1);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicInvalidationListener.check(readOnlyProperty, 1);

        // change binding once
        v.set(VALUE_2);
        assertEquals(VALUE_2, property.get());
        property.check(1);
        internalInvalidationListener.check(readOnlyProperty, 1);
        assertEquals(VALUE_2, readOnlyProperty.get());
        publicInvalidationListener.check(readOnlyProperty, 1);

        // change binding twice without reading
        v.set(VALUE_1);
        v.set(VALUE_2);
        assertEquals(VALUE_2, property.get());
        property.check(1);
        internalInvalidationListener.check(readOnlyProperty, 1);
        assertEquals(VALUE_2, readOnlyProperty.get());
        publicInvalidationListener.check(readOnlyProperty, 1);

        // change binding twice to same value
        v.set(VALUE_1);
        v.set(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(1);
        internalInvalidationListener.check(readOnlyProperty, 1);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicInvalidationListener.check(readOnlyProperty, 1);
    }

    @Test
    public void testInternalEagerBind_generic() {
        attachInternalChangeListener();
        final ObservableObjectValueStub<ObservableSet<Object>> v = new ObservableObjectValueStub<ObservableSet<Object>>(VALUE_1);

        property.bind(v);
        assertEquals(VALUE_1, property.get());
        assertTrue(property.isBound());
        property.check(1);
        internalChangeListener.check(readOnlyProperty, DEFAULT, VALUE_1, 1);
        assertEquals(VALUE_1, readOnlyProperty.get());

        // change binding once
        v.set(VALUE_2);
        assertEquals(VALUE_2, property.get());
        property.check(1);
        internalChangeListener.check(readOnlyProperty, VALUE_1, VALUE_2, 1);
        assertEquals(VALUE_2, readOnlyProperty.get());

        // change binding twice without reading
        v.set(VALUE_1);
        v.set(VALUE_2);
        assertEquals(VALUE_2, property.get());
        property.check(2);
        internalChangeListener.check(readOnlyProperty, VALUE_1, VALUE_2, 2);
        assertEquals(VALUE_2, readOnlyProperty.get());

        // change binding twice to same value
        v.set(VALUE_1);
        v.set(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(2);
        internalChangeListener.check(readOnlyProperty, VALUE_2, VALUE_1, 1);
        assertEquals(VALUE_1, readOnlyProperty.get());
    }

    @Test
    public void testPublicEagerBind_generic() {
        attachPublicChangeListener();
        final ObservableObjectValueStub<ObservableSet<Object>> v = new ObservableObjectValueStub<ObservableSet<Object>>(VALUE_1);

        property.bind(v);
        assertEquals(VALUE_1, property.get());
        assertTrue(property.isBound());
        property.check(1);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicChangeListener.check(readOnlyProperty, DEFAULT, VALUE_1, 1);

        // change binding once
        v.set(VALUE_2);
        assertEquals(VALUE_2, property.get());
        property.check(1);
        assertEquals(VALUE_2, readOnlyProperty.get());
        publicChangeListener.check(readOnlyProperty, VALUE_1, VALUE_2, 1);

        // change binding twice without reading
        v.set(VALUE_1);
        v.set(VALUE_2);
        assertEquals(VALUE_2, property.get());
        property.check(2);
        assertEquals(VALUE_2, readOnlyProperty.get());
        publicChangeListener.check(readOnlyProperty, VALUE_1, VALUE_2, 2);

        // change binding twice to same value
        v.set(VALUE_1);
        v.set(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(2);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicChangeListener.check(readOnlyProperty, VALUE_2, VALUE_1, 1);
    }
    
    @Test(expected=NullPointerException.class)
    public void testBindToNull() {
        property.bind(null);
    }

    @Test
    public void testRebind() {
        attachInvalidationListeners();
        final SetProperty<Object> v1 = new SimpleSetProperty<Object>(VALUE_1);
        final SetProperty<Object> v2 = new SimpleSetProperty<Object>(VALUE_2);
        property.bind(v1);
        property.get();
        readOnlyProperty.get();
        property.reset();
        internalInvalidationListener.reset();
        publicInvalidationListener.reset();
        
        // rebind causes invalidation event
        property.bind(v2);
        assertEquals(VALUE_2, property.get());
        assertTrue(property.isBound());
        property.check(1);
        internalInvalidationListener.check(readOnlyProperty, 1);
        assertEquals(VALUE_2, readOnlyProperty.get());
        publicInvalidationListener.check(readOnlyProperty, 1);
        
        // change new binding
        v2.set(VALUE_1);
        assertEquals(VALUE_1, property.get());
        property.check(1);
        internalInvalidationListener.check(readOnlyProperty, 1);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicInvalidationListener.check(readOnlyProperty, 1);
        
        // change old binding
        v1.set(VALUE_2);
        assertEquals(VALUE_1, property.get());
        property.check(0);
        internalInvalidationListener.check(null, 0);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicInvalidationListener.check(null, 0);
        
        // rebind to same observable should have no effect
        property.bind(v2);
        assertEquals(VALUE_1, property.get());
        assertTrue(property.isBound());
        property.check(0);
        internalInvalidationListener.check(null, 0);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicInvalidationListener.check(null, 0);
    }

    @Test
    public void testUnbind() {
        attachInvalidationListeners();
        final SetProperty<Object> v = new SimpleSetProperty<Object>(VALUE_1);
        property.bind(v);
        property.unbind();
        assertEquals(VALUE_1, property.get());
        assertFalse(property.isBound());
        assertEquals(VALUE_1, readOnlyProperty.get());
        property.reset();
        internalInvalidationListener.reset();
        publicInvalidationListener.reset();
        
        // change binding
        v.set(VALUE_2);
        assertEquals(VALUE_1, property.get());
        property.check(0);
        internalInvalidationListener.check(null, 0);
        assertEquals(VALUE_1, readOnlyProperty.get());
        publicInvalidationListener.check(null, 0);
        
        // set value
        property.set(VALUE_2);
        assertEquals(VALUE_2, property.get());
        property.check(1);
        internalInvalidationListener.check(readOnlyProperty, 1);
        assertEquals(VALUE_2, readOnlyProperty.get());
        publicInvalidationListener.check(readOnlyProperty, 1);
    }
    
    @Test
    public void testAddingListenerWillAlwaysReceiveInvalidationEvent() {
        final SetProperty<Object> v = new SimpleSetProperty<Object>(VALUE_1);
        final InvalidationListenerMock internalListener2 = new InvalidationListenerMock();
        final InvalidationListenerMock internalListener3 = new InvalidationListenerMock();
        final InvalidationListenerMock publicListener2 = new InvalidationListenerMock();
        final InvalidationListenerMock publicListener3 = new InvalidationListenerMock();

        // setting the property,checking internal
        property.set(VALUE_1);
        property.addListener(internalListener2);
        internalListener2.reset();
        property.set(VALUE_2);
        internalListener2.check(readOnlyProperty, 1);
        
        // setting the property, checking public
        property.set(VALUE_1);
        readOnlyProperty.addListener(publicListener2);
        publicListener2.reset();
        property.set(VALUE_2);
        publicListener2.check(readOnlyProperty, 1);
        
        // binding the property, checking internal
        property.bind(v);
        v.set(VALUE_2);
        property.addListener(internalListener3);
        v.get();
        internalListener3.reset();
        v.set(VALUE_1);
        internalListener3.check(readOnlyProperty, 1);
        
        // binding the property, checking public
        property.bind(v);
        v.set(VALUE_2);
        readOnlyProperty.addListener(publicListener3);
        v.get();
        publicListener3.reset();
        v.set(VALUE_1);
        publicListener3.check(readOnlyProperty, 1);
    }
    
    @Test
    public void testRemoveListeners() {
        attachInvalidationListeners();
        attachInternalChangeListener();
        property.removeListener(internalInvalidationListener);
        property.removeListener(internalChangeListener);
        property.get();
        internalInvalidationListener.reset();
        internalChangeListener.reset();
        
        property.set(VALUE_1);
        internalInvalidationListener.check(null, 0);
        internalChangeListener.check(null, UNDEFINED, UNDEFINED, 0);
        
        // no read only property created => no-op
        final ReadOnlySetWrapper<Object> v1 = new ReadOnlySetWrapper<Object>();
        v1.removeListener(internalInvalidationListener);
        v1.removeListener(internalChangeListener);
    }
    
    @Test
    public void testNoReadOnlyPropertyCreated() {
        final SetProperty<Object> v1 = new SimpleSetProperty<Object>(VALUE_1);
        final ReadOnlySetWrapper<Object> p1 = new ReadOnlySetWrapper<Object>();
        
        p1.set(VALUE_1);
        p1.bind(v1);
        assertEquals(VALUE_1, p1.get());
        v1.set(VALUE_2);
        assertEquals(VALUE_2, p1.get());
    }
    
    @Test
    public void testToString() {
        final SetProperty<Object> v1 = new SimpleSetProperty<Object>(VALUE_1);
        
        property.set(VALUE_1);
        assertEquals("SetProperty [value: " + VALUE_1 + "]", property.toString());
        assertEquals("ReadOnlySetProperty [value: " + VALUE_1 + "]", readOnlyProperty.toString());
        
        property.bind(v1);
        assertEquals("SetProperty [bound, invalid]", property.toString());
        assertEquals("ReadOnlySetProperty [value: " + VALUE_1 + "]", readOnlyProperty.toString());
        property.get();
        assertEquals("SetProperty [bound, value: " + VALUE_1 + "]", property.toString());
        assertEquals("ReadOnlySetProperty [value: " + VALUE_1 + "]", readOnlyProperty.toString());
        v1.set(VALUE_2);
        assertEquals("SetProperty [bound, invalid]", property.toString());
        assertEquals("ReadOnlySetProperty [value: " + VALUE_2 + "]", readOnlyProperty.toString());
        property.get();
        assertEquals("SetProperty [bound, value: " + VALUE_2 + "]", property.toString());
        assertEquals("ReadOnlySetProperty [value: " + VALUE_2 + "]", readOnlyProperty.toString());
        
        final Object bean = new Object();
        final String name = "My name";
        final ReadOnlySetWrapper<Object> v2 = new ReadOnlySetWrapper<Object>(bean, name);
        assertEquals("SetProperty [bean: " + bean.toString() + ", name: My name, value: " + DEFAULT + "]", v2.toString());
        assertEquals("ReadOnlySetProperty [bean: " + bean.toString() + ", name: My name, value: " + DEFAULT + "]", v2.getReadOnlyProperty().toString());
        
        final ReadOnlySetWrapper<Object> v3 = new ReadOnlySetWrapper<Object>(bean, "");
        assertEquals("SetProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v3.toString());
        assertEquals("ReadOnlySetProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v3.getReadOnlyProperty().toString());

        final ReadOnlySetWrapper<Object> v4 = new ReadOnlySetWrapper<Object>(null, name);
        assertEquals("SetProperty [name: My name, value: " + DEFAULT + "]", v4.toString());
        assertEquals("ReadOnlySetProperty [name: My name, value: " + DEFAULT + "]", v4.getReadOnlyProperty().toString());
    }
    
    private static class ReadOnlySetWrapperMock extends ReadOnlySetWrapper<Object> {
        
        private int counter;
        
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
    }
}
