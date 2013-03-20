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

import org.junit.Before;
import org.junit.Test;

public class ReadOnlyLongPropertyTest {

    private static final long DEFAULT = 0L;

    @Before
    public void setUp() throws Exception {
    }


    @Test
    public void testToString() {
        final ReadOnlyLongProperty v1 = new ReadOnlyLongPropertyStub(null, "");
        assertEquals("ReadOnlyLongProperty [value: " + DEFAULT + "]", v1.toString());
        
        final ReadOnlyLongProperty v2 = new ReadOnlyLongPropertyStub(null, null);
        assertEquals("ReadOnlyLongProperty [value: " + DEFAULT + "]", v2.toString());
        
        final Object bean = new Object();
        final String name = "My name";
        final ReadOnlyLongProperty v3 = new ReadOnlyLongPropertyStub(bean, name);
        assertEquals("ReadOnlyLongProperty [bean: " + bean.toString() + ", name: My name, value: " + DEFAULT + "]", v3.toString());
        
        final ReadOnlyLongProperty v4 = new ReadOnlyLongPropertyStub(bean, "");
        assertEquals("ReadOnlyLongProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v4.toString());
        
        final ReadOnlyLongProperty v5 = new ReadOnlyLongPropertyStub(bean, null);
        assertEquals("ReadOnlyLongProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v5.toString());
        
        final ReadOnlyLongProperty v6 = new ReadOnlyLongPropertyStub(null, name);
        assertEquals("ReadOnlyLongProperty [name: My name, value: " + DEFAULT + "]", v6.toString());
        
    }
    
    @Test
    public void testAsObject() {
        final ReadOnlyLongWrapper valueModel = new ReadOnlyLongWrapper();
        final ReadOnlyObjectProperty<Long> exp = valueModel.getReadOnlyProperty().asObject();

        assertEquals(Long.valueOf(0L), exp.getValue());
        valueModel.set(-4354L);
        assertEquals(Long.valueOf(-4354L), exp.getValue());
        valueModel.set(5L);
        assertEquals(Long.valueOf(5L), exp.getValue());
    }
    
    @Test
    public void testObjectToLong() {
        final ReadOnlyObjectWrapper<Long> valueModel = new ReadOnlyObjectWrapper<Long>();
        final ReadOnlyLongProperty exp = ReadOnlyLongProperty.readOnlyLongProperty(valueModel.getReadOnlyProperty());
        
        assertEquals(0L, exp.longValue());
        valueModel.set(-4354L);
        assertEquals(-4354L, exp.longValue());
        valueModel.set(5L);
        assertEquals(5L, exp.longValue());
    }
    
    private static class ReadOnlyLongPropertyStub extends ReadOnlyLongProperty {
        
        private final Object bean;
        private final String name;
        
        private ReadOnlyLongPropertyStub(Object bean, String name) {
            this.bean = bean;
            this.name = name;
        }

        @Override public Object getBean() { return bean; }
        @Override public String getName() { return name; }
        @Override public long get() { return 0L; }

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
