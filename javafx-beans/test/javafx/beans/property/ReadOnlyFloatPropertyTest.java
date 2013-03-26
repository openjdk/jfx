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

public class ReadOnlyFloatPropertyTest {

    private static final float DEFAULT = 0.0f;
    private static final float EPSILON = 1e-6f;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testToString() {
        final ReadOnlyFloatProperty v1 = new ReadOnlyFloatPropertyStub(null, "");
        assertEquals("ReadOnlyFloatProperty [value: " + DEFAULT + "]", v1.toString());
        
        final ReadOnlyFloatProperty v2 = new ReadOnlyFloatPropertyStub(null, null);
        assertEquals("ReadOnlyFloatProperty [value: " + DEFAULT + "]", v2.toString());
        
        final Object bean = new Object();
        final String name = "My name";
        final ReadOnlyFloatProperty v3 = new ReadOnlyFloatPropertyStub(bean, name);
        assertEquals("ReadOnlyFloatProperty [bean: " + bean.toString() + ", name: My name, value: " + DEFAULT + "]", v3.toString());
        
        final ReadOnlyFloatProperty v4 = new ReadOnlyFloatPropertyStub(bean, "");
        assertEquals("ReadOnlyFloatProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v4.toString());
        
        final ReadOnlyFloatProperty v5 = new ReadOnlyFloatPropertyStub(bean, null);
        assertEquals("ReadOnlyFloatProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v5.toString());
        
        final ReadOnlyFloatProperty v6 = new ReadOnlyFloatPropertyStub(null, name);
        assertEquals("ReadOnlyFloatProperty [name: My name, value: " + DEFAULT + "]", v6.toString());
        
    }
    
    @Test
    public void testAsObject() {
        final ReadOnlyFloatWrapper valueModel = new ReadOnlyFloatWrapper();
        final ReadOnlyObjectProperty<Float> exp = valueModel.getReadOnlyProperty().asObject();

        assertEquals(0.0, exp.getValue(), EPSILON);
        valueModel.set(-4354.3f);
        assertEquals(-4354.3f, exp.getValue(), EPSILON);
        valueModel.set(5e11f);
        assertEquals(5e11f, exp.getValue(), EPSILON);
    }
    
    @Test
    public void testObjectToFloat() {
        final ReadOnlyObjectWrapper<Float> valueModel = new ReadOnlyObjectWrapper<Float>();
        final ReadOnlyFloatProperty exp = ReadOnlyFloatProperty.readOnlyFloatProperty(valueModel.getReadOnlyProperty());
        

        assertEquals(0.0, exp.floatValue(), EPSILON);
        valueModel.set(-4354.3f);
        assertEquals(-4354.3f, exp.floatValue(), EPSILON);
        valueModel.set(5e11f);
        assertEquals(5e11f, exp.floatValue(), EPSILON);
    }
    
    private static class ReadOnlyFloatPropertyStub extends ReadOnlyFloatProperty {
        
        private final Object bean;
        private final String name;
        
        private ReadOnlyFloatPropertyStub(Object bean, String name) {
            this.bean = bean;
            this.name = name;
        }

        @Override public Object getBean() { return bean; }
        @Override public String getName() { return name; }
        @Override public float get() { return 0.0f; }

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
