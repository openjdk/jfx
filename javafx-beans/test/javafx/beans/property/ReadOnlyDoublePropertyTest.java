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
import javafx.beans.value.ChangeListener;

import org.junit.Before;
import org.junit.Test;

public class ReadOnlyDoublePropertyTest {

    private static final double DEFAULT = 0.0;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testHashCode() {
        final Object bean = new Object();
        final String name = "Name1";
        
        final int golden1 = new ReadOnlyDoublePropertyStub(bean, name).hashCode();
        assertEquals(golden1, golden1);
        assertEquals(golden1, new ReadOnlyDoublePropertyStub(bean, name).hashCode());
        
        final int golden2 = new ReadOnlyDoublePropertyStub(null, name).hashCode();
        assertEquals(golden2, golden2);
        assertEquals(golden2, new ReadOnlyDoublePropertyStub(null, name).hashCode());
        
        final int golden3 = new ReadOnlyDoublePropertyStub(bean, "").hashCode();
        assertEquals(golden3, golden3);
        assertEquals(golden3, new ReadOnlyDoublePropertyStub(bean, "").hashCode());
        
        final int golden4 = new ReadOnlyDoublePropertyStub(null, "").hashCode();
        assertEquals(golden4, golden4);
        
        final int golden5 = new ReadOnlyDoublePropertyStub(bean, null).hashCode();
        assertEquals(golden5, golden5);
        assertEquals(golden5, new ReadOnlyDoublePropertyStub(bean, null).hashCode());
        
        final int golden6 = new ReadOnlyDoublePropertyStub(null, null).hashCode();
        assertEquals(golden6, golden6);
    }

    @Test
    public void testEqualsObject() {
        final Object bean1 = new Object();
        final Object bean2 = new Object();
        final String name1 = "Name1";
        final String name2 = "Name2";
        
        final ReadOnlyDoubleProperty golden1 = new ReadOnlyDoublePropertyStub(bean1, name1);
        assertTrue(golden1.equals(golden1));
        assertTrue(golden1.equals(new ReadOnlyDoublePropertyStub(bean1, name1)));
        assertFalse(golden1.equals(new ReadOnlyDoublePropertyStub(bean2, name1)));
        assertFalse(golden1.equals(new ReadOnlyDoublePropertyStub(bean1, name2)));
        assertFalse(golden1.equals(new ReadOnlyDoublePropertyStub(null, name1)));
        assertFalse(golden1.equals(new ReadOnlyDoublePropertyStub(bean1, "")));
        assertFalse(golden1.equals(new ReadOnlyDoublePropertyStub(null, "")));
        assertFalse(golden1.equals(bean1));
        assertFalse(golden1.equals(null));
        
        final ReadOnlyDoubleProperty golden2 = new ReadOnlyDoublePropertyStub(bean1, "");
        assertTrue(golden2.equals(golden2));
        assertFalse(golden2.equals(new ReadOnlyDoublePropertyStub(bean1, "")));
        assertFalse(golden2.equals(new ReadOnlyDoublePropertyStub(null, "")));
        assertFalse(golden2.equals(new ReadOnlyDoublePropertyStub(bean1, name1)));
        assertFalse(golden2.equals(bean1));
        assertFalse(golden2.equals(null));
        
        final ReadOnlyDoubleProperty golden3 = new ReadOnlyDoublePropertyStub(null, name1);
        assertTrue(golden3.equals(golden3));
        assertFalse(golden3.equals(new ReadOnlyDoublePropertyStub(null, name1)));
        assertFalse(golden3.equals(new ReadOnlyDoublePropertyStub(bean1, name1)));
        assertFalse(golden3.equals(new ReadOnlyDoublePropertyStub(null, "")));
        assertFalse(golden3.equals(bean1));
        assertFalse(golden3.equals(null));
        
        final ReadOnlyDoubleProperty golden4 = new ReadOnlyDoublePropertyStub(null, "");
        assertTrue(golden4.equals(golden4));
        assertFalse(golden4.equals(new ReadOnlyDoublePropertyStub(null, name1)));
        assertFalse(golden4.equals(new ReadOnlyDoublePropertyStub(bean1, "")));
        assertFalse(golden4.equals(new ReadOnlyDoublePropertyStub(null, "")));
        assertFalse(golden4.equals(bean1));
        assertFalse(golden4.equals(null));
        
        final ReadOnlyDoubleProperty golden5 = new ReadOnlyDoublePropertyStub(bean1, null);
        assertTrue(golden5.equals(golden5));
        assertFalse(golden5.equals(new ReadOnlyDoublePropertyStub(bean1, null)));
        assertFalse(golden5.equals(new ReadOnlyDoublePropertyStub(null, "")));
        assertFalse(golden5.equals(new ReadOnlyDoublePropertyStub(bean1, name1)));
        assertFalse(golden5.equals(bean1));
        assertFalse(golden5.equals(null));
        
        final ReadOnlyDoubleProperty golden6 = new ReadOnlyDoublePropertyStub(null, null);
        assertTrue(golden6.equals(golden6));
        assertFalse(golden6.equals(new ReadOnlyDoublePropertyStub(null, name1)));
        assertFalse(golden6.equals(new ReadOnlyDoublePropertyStub(bean1, "")));
        assertFalse(golden6.equals(new ReadOnlyDoublePropertyStub(null, "")));
        assertFalse(golden6.equals(bean1));
        assertFalse(golden6.equals(null));
    }

    @Test
    public void testToString() {
        final ReadOnlyDoubleProperty v1 = new ReadOnlyDoublePropertyStub(null, "");
        assertEquals("ReadOnlyDoubleProperty [value: " + DEFAULT + "]", v1.toString());
        
        final ReadOnlyDoubleProperty v2 = new ReadOnlyDoublePropertyStub(null, null);
        assertEquals("ReadOnlyDoubleProperty [value: " + DEFAULT + "]", v2.toString());
        
        final Object bean = new Object();
        final String name = "My name";
        final ReadOnlyDoubleProperty v3 = new ReadOnlyDoublePropertyStub(bean, name);
        assertEquals("ReadOnlyDoubleProperty [bean: " + bean.toString() + ", name: My name, value: " + DEFAULT + "]", v3.toString());
        
        final ReadOnlyDoubleProperty v4 = new ReadOnlyDoublePropertyStub(bean, "");
        assertEquals("ReadOnlyDoubleProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v4.toString());
        
        final ReadOnlyDoubleProperty v5 = new ReadOnlyDoublePropertyStub(bean, null);
        assertEquals("ReadOnlyDoubleProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v5.toString());
        
        final ReadOnlyDoubleProperty v6 = new ReadOnlyDoublePropertyStub(null, name);
        assertEquals("ReadOnlyDoubleProperty [name: My name, value: " + DEFAULT + "]", v6.toString());
        
    }
    
    private static class ReadOnlyDoublePropertyStub extends ReadOnlyDoubleProperty {
        
        private final Object bean;
        private final String name;
        
        private ReadOnlyDoublePropertyStub(Object bean, String name) {
            this.bean = bean;
            this.name = name;
        }

        @Override public Object getBean() { return bean; }
        @Override public String getName() { return name; }
        @Override public double get() { return 0.0; }

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
