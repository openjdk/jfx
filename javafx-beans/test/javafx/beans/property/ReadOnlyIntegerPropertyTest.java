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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.value.ChangeListener;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class ReadOnlyIntegerPropertyTest {

    private static final int DEFAULT = 0;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testToString() {
        final ReadOnlyIntegerProperty v1 = new ReadOnlyIntegerPropertyStub(null, "");
        assertEquals("ReadOnlyIntegerProperty [value: " + DEFAULT + "]", v1.toString());
        
        final ReadOnlyIntegerProperty v2 = new ReadOnlyIntegerPropertyStub(null, null);
        assertEquals("ReadOnlyIntegerProperty [value: " + DEFAULT + "]", v2.toString());
        
        final Object bean = new Object();
        final String name = "My name";
        final ReadOnlyIntegerProperty v3 = new ReadOnlyIntegerPropertyStub(bean, name);
        assertEquals("ReadOnlyIntegerProperty [bean: " + bean.toString() + ", name: My name, value: " + DEFAULT + "]", v3.toString());
        
        final ReadOnlyIntegerProperty v4 = new ReadOnlyIntegerPropertyStub(bean, "");
        assertEquals("ReadOnlyIntegerProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v4.toString());
        
        final ReadOnlyIntegerProperty v5 = new ReadOnlyIntegerPropertyStub(bean, null);
        assertEquals("ReadOnlyIntegerProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v5.toString());
        
        final ReadOnlyIntegerProperty v6 = new ReadOnlyIntegerPropertyStub(null, name);
        assertEquals("ReadOnlyIntegerProperty [name: My name, value: " + DEFAULT + "]", v6.toString());
        
    }
    
    @Test
    public void testAsObject() {
        final ReadOnlyIntegerWrapper valueModel = new ReadOnlyIntegerWrapper();
        final ReadOnlyObjectProperty<Integer> exp = valueModel.getReadOnlyProperty().asObject();

        assertEquals(Integer.valueOf(0), exp.getValue());
        valueModel.set(-4354);
        assertEquals(Integer.valueOf(-4354), exp.getValue());
        valueModel.set(5);
        assertEquals(Integer.valueOf(5), exp.getValue());
    }
    
    @Test
    public void testObjectToInteger() {
        final ReadOnlyObjectWrapper<Integer> valueModel = new ReadOnlyObjectWrapper<Integer>();
        final ReadOnlyIntegerProperty exp = ReadOnlyIntegerProperty.readOnlyIntegerProperty(valueModel.getReadOnlyProperty());

        assertEquals(0, exp.intValue());
        valueModel.set(-4354);
        assertEquals(-4354, exp.intValue());
        valueModel.set(5);
        assertEquals(5, exp.intValue());
    }
    
    private static class ReadOnlyIntegerPropertyStub extends ReadOnlyIntegerProperty {
        
        private final Object bean;
        private final String name;
        
        private ReadOnlyIntegerPropertyStub(Object bean, String name) {
            this.bean = bean;
            this.name = name;
        }

        @Override public Object getBean() { return bean; }
        @Override public String getName() { return name; }
        @Override public int get() { return 0; }

        @Override
        public void addListener(ChangeListener<? super Number> listener) {
        }

        @Override
        public void removeListener(ChangeListener<? super Number> listener) {
        }

        @Override
        public void addListener(InvalidationListener listener) {
        }

        @Override
        public void removeListener(InvalidationListener listener) {
        }
        
    }

}
