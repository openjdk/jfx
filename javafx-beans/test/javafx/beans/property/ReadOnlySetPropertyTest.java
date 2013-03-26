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
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReadOnlySetPropertyTest {

    private static final Object DEFAULT = null;

    @Before
    public void setUp() throws Exception {
    }


    @Test
    public void testToString() {
        final ReadOnlySetProperty<Object> v1 = new ReadOnlySetPropertyStub(null, "");
        assertEquals("ReadOnlySetProperty [value: " + DEFAULT + "]", v1.toString());
        
        final ReadOnlySetProperty<Object> v2 = new ReadOnlySetPropertyStub(null, null);
        assertEquals("ReadOnlySetProperty [value: " + DEFAULT + "]", v2.toString());
        
        final Object bean = new Object();
        final String name = "My name";
        final ReadOnlySetProperty<Object> v3 = new ReadOnlySetPropertyStub(bean, name);
        assertEquals("ReadOnlySetProperty [bean: " + bean.toString() + ", name: My name, value: " + DEFAULT + "]", v3.toString());
        
        final ReadOnlySetProperty<Object> v4 = new ReadOnlySetPropertyStub(bean, "");
        assertEquals("ReadOnlySetProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v4.toString());
        
        final ReadOnlySetProperty<Object> v5 = new ReadOnlySetPropertyStub(bean, null);
        assertEquals("ReadOnlySetProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v5.toString());
        
        final ReadOnlySetProperty<Object> v6 = new ReadOnlySetPropertyStub(null, name);
        assertEquals("ReadOnlySetProperty [name: My name, value: " + DEFAULT + "]", v6.toString());
        
    }
    
    private static class ReadOnlySetPropertyStub extends ReadOnlySetProperty<Object> {
        
        private final Object bean;
        private final String name;
        
        private ReadOnlySetPropertyStub(Object bean, String name) {
            this.bean = bean;
            this.name = name;
        }

        @Override public Object getBean() { return bean; }
        @Override public String getName() { return name; }
        @Override public ObservableSet<Object> get() { return null; }

        @Override
        public void addListener(ChangeListener<? super ObservableSet<Object>> listener) {
        }

        @Override
        public void removeListener(ChangeListener<? super ObservableSet<Object>> listener) {
        }

        @Override
        public void addListener(InvalidationListener listener) {
        }

        @Override
        public void removeListener(InvalidationListener listener) {
        }

        @Override
        public void addListener(SetChangeListener<? super Object> listChangeListener) {
        }

        @Override
        public void removeListener(SetChangeListener<? super Object> listChangeListener) {
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
