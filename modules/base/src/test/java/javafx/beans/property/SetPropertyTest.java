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

package javafx.beans.property;


import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SetPropertyTest {

    private static final Object NO_BEAN = null;
    private static final String NO_NAME_1 = null;
    private static final String NO_NAME_2 = "";
    private static final ObservableSet<Object> VALUE_1 = FXCollections.observableSet();
    private static final ObservableSet<Object> VALUE_2 = FXCollections.observableSet(new Object());
    private static final Object DEFAULT = null;

    @Test
    public void testBindBidirectional() {
        final SetProperty<Object> p1 = new SimpleSetProperty<Object>(VALUE_2);
        final SetProperty<Object> p2 = new SimpleSetProperty<Object>(VALUE_1);
        
        p1.bindBidirectional(p2);
        assertEquals(VALUE_1, p1.get());
        assertEquals(VALUE_1, p2.get());
        
        p1.set(VALUE_2);
        assertEquals(VALUE_2, p1.get());
        assertEquals(VALUE_2, p2.get());
        
        p2.set(VALUE_1);
        assertEquals(VALUE_1, p1.get());
        assertEquals(VALUE_1, p2.get());
        
        p1.unbindBidirectional(p2);
        p1.set(VALUE_2);
        assertEquals(VALUE_2, p1.get());
        assertEquals(VALUE_1, p2.get());
        
        p1.set(VALUE_1);
        p2.set(VALUE_2);
        assertEquals(VALUE_1, p1.get());
        assertEquals(VALUE_2, p2.get());
    }
    
    @Test
    public void testToString() {
        final SetProperty<Object> v0 = new SetPropertyStub(NO_BEAN, NO_NAME_1);
        assertEquals("SetProperty [value: " + DEFAULT + "]", v0.toString());
        
        final SetProperty<Object> v1 = new SetPropertyStub(NO_BEAN, NO_NAME_2);
        assertEquals("SetProperty [value: " + DEFAULT + "]", v1.toString());
        
        final Object bean = new Object();
        final String name = "My name";
        final SetProperty<Object> v2 = new SetPropertyStub(bean, name);
        assertEquals("SetProperty [bean: " + bean.toString() + ", name: My name, value: " + DEFAULT + "]", v2.toString());
        v2.set(VALUE_1);
        assertEquals("SetProperty [bean: " + bean.toString() + ", name: My name, value: " + VALUE_1 + "]", v2.toString());
        
        final SetProperty<Object> v3 = new SetPropertyStub(bean, NO_NAME_1);
        assertEquals("SetProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v3.toString());
        v3.set(VALUE_1);
        assertEquals("SetProperty [bean: " + bean.toString() + ", value: " + VALUE_1 + "]", v3.toString());

        final SetProperty<Object> v4 = new SetPropertyStub(bean, NO_NAME_2);
        assertEquals("SetProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v4.toString());
        v4.set(VALUE_1);
        assertEquals("SetProperty [bean: " + bean.toString() + ", value: " + VALUE_1 + "]", v4.toString());

        final SetProperty<Object> v5 = new SetPropertyStub(NO_BEAN, name);
        assertEquals("SetProperty [name: My name, value: " + DEFAULT + "]", v5.toString());
        v5.set(VALUE_1);
        assertEquals("SetProperty [name: My name, value: " + VALUE_1 + "]", v5.toString());
    }
    
    private class SetPropertyStub extends SetProperty<Object> {
        
        private final Object bean;
        private final String name;
        private ObservableSet<Object> value;
        
        private SetPropertyStub(Object bean, String name) {
            this.bean = bean;
            this.name = name;
        }

        @Override
        public Object getBean() {
            return bean;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public ObservableSet<Object> get() {
            return value;
        }

        @Override
        public void set(ObservableSet<Object> value) {
            this.value = value;
        }

        @Override
        public void bind(ObservableValue<? extends ObservableSet<Object>> observable) {
            fail("Not in use");
        }

        @Override
        public void unbind() {
            fail("Not in use");
        }

        @Override
        public boolean isBound() {
            fail("Not in use");
            return false;
        }

        @Override
        public void addListener(ChangeListener<? super ObservableSet<Object>> listener) {
            fail("Not in use");
        }

        @Override
        public void removeListener(ChangeListener<? super ObservableSet<Object>> listener) {
            fail("Not in use");
        }

        @Override
        public void addListener(InvalidationListener listener) {
            fail("Not in use");
        }

        @Override
        public void removeListener(InvalidationListener listener) {
            fail("Not in use");
        }

        @Override
        public void addListener(SetChangeListener<? super Object> listChangeListener) {
            fail("Not in use");
        }

        @Override
        public void removeListener(SetChangeListener<? super Object> listChangeListener) {
            fail("Not in use");
        }

        @Override
        public ReadOnlyIntegerProperty sizeProperty() {
            fail("Not in use");
            return null;
        }

        @Override
        public ReadOnlyBooleanProperty emptyProperty() {
            fail("Not in use");
            return null;
        }
    }
}
