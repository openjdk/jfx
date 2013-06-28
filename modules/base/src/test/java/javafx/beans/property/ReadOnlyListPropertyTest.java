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
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReadOnlyListPropertyTest {

    private static final Object DEFAULT = null;

    @Before
    public void setUp() throws Exception {
    }


    @Test
    public void testToString() {
        final ReadOnlyListProperty<Object> v1 = new ReadOnlyListPropertyStub(null, "");
        assertEquals("ReadOnlyListProperty [value: " + DEFAULT + "]", v1.toString());
        
        final ReadOnlyListProperty<Object> v2 = new ReadOnlyListPropertyStub(null, null);
        assertEquals("ReadOnlyListProperty [value: " + DEFAULT + "]", v2.toString());
        
        final Object bean = new Object();
        final String name = "My name";
        final ReadOnlyListProperty<Object> v3 = new ReadOnlyListPropertyStub(bean, name);
        assertEquals("ReadOnlyListProperty [bean: " + bean.toString() + ", name: My name, value: " + DEFAULT + "]", v3.toString());
        
        final ReadOnlyListProperty<Object> v4 = new ReadOnlyListPropertyStub(bean, "");
        assertEquals("ReadOnlyListProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v4.toString());
        
        final ReadOnlyListProperty<Object> v5 = new ReadOnlyListPropertyStub(bean, null);
        assertEquals("ReadOnlyListProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v5.toString());
        
        final ReadOnlyListProperty<Object> v6 = new ReadOnlyListPropertyStub(null, name);
        assertEquals("ReadOnlyListProperty [name: My name, value: " + DEFAULT + "]", v6.toString());
        
    }
    
    private static class ReadOnlyListPropertyStub extends ReadOnlyListProperty<Object> {
        
        private final Object bean;
        private final String name;
        
        private ReadOnlyListPropertyStub(Object bean, String name) {
            this.bean = bean;
            this.name = name;
        }

        @Override public Object getBean() { return bean; }
        @Override public String getName() { return name; }
        @Override public ObservableList<Object> get() { return null; }

        @Override
        public void addListener(ChangeListener<? super ObservableList<Object>> listener) {
        }

        @Override
        public void removeListener(ChangeListener<? super ObservableList<Object>> listener) {
        }

        @Override
        public void addListener(InvalidationListener listener) {
        }

        @Override
        public void removeListener(InvalidationListener listener) {
        }

        @Override
        public void addListener(ListChangeListener<? super Object> listChangeListener) {
        }

        @Override
        public void removeListener(ListChangeListener<? super Object> listChangeListener) {
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
