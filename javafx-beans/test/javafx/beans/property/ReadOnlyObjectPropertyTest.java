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

public class ReadOnlyObjectPropertyTest {

    private static final Object DEFAULT = null;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testHashCode() {
        final Object bean = new Object();
        final String name = "Name1";
        
        final int golden1 = new ReadOnlyObjectPropertyStub(bean, name).hashCode();
        assertEquals(golden1, golden1);
        assertEquals(golden1, new ReadOnlyObjectPropertyStub(bean, name).hashCode());
        
        final int golden2 = new ReadOnlyObjectPropertyStub(null, name).hashCode();
        assertEquals(golden2, golden2);
        assertEquals(golden2, new ReadOnlyObjectPropertyStub(null, name).hashCode());
        
        final int golden3 = new ReadOnlyObjectPropertyStub(bean, "").hashCode();
        assertEquals(golden3, golden3);
        assertEquals(golden3, new ReadOnlyObjectPropertyStub(bean, "").hashCode());
        
        final int golden4 = new ReadOnlyObjectPropertyStub(null, "").hashCode();
        assertEquals(golden4, golden4);
        
        final int golden5 = new ReadOnlyObjectPropertyStub(bean, null).hashCode();
        assertEquals(golden5, golden5);
        assertEquals(golden5, new ReadOnlyObjectPropertyStub(bean, null).hashCode());
        
        final int golden6 = new ReadOnlyObjectPropertyStub(null, null).hashCode();
        assertEquals(golden6, golden6);
    }

    @Test
    public void testEqualsObject() {
        final Object bean1 = new Object();
        final Object bean2 = new Object();
        final String name1 = "Name1";
        final String name2 = "Name2";
        
        final ReadOnlyObjectProperty<Object> golden1 = new ReadOnlyObjectPropertyStub(bean1, name1);
        assertTrue(golden1.equals(golden1));
        assertTrue(golden1.equals(new ReadOnlyObjectPropertyStub(bean1, name1)));
        assertFalse(golden1.equals(new ReadOnlyObjectPropertyStub(bean2, name1)));
        assertFalse(golden1.equals(new ReadOnlyObjectPropertyStub(bean1, name2)));
        assertFalse(golden1.equals(new ReadOnlyObjectPropertyStub(null, name1)));
        assertFalse(golden1.equals(new ReadOnlyObjectPropertyStub(bean1, "")));
        assertFalse(golden1.equals(new ReadOnlyObjectPropertyStub(null, "")));
        assertFalse(golden1.equals(bean1));
        assertFalse(golden1.equals(null));
        
        final ReadOnlyObjectProperty<Object> golden2 = new ReadOnlyObjectPropertyStub(bean1, "");
        assertTrue(golden2.equals(golden2));
        assertFalse(golden2.equals(new ReadOnlyObjectPropertyStub(bean1, "")));
        assertFalse(golden2.equals(new ReadOnlyObjectPropertyStub(null, "")));
        assertFalse(golden2.equals(new ReadOnlyObjectPropertyStub(bean1, name1)));
        assertFalse(golden2.equals(bean1));
        assertFalse(golden2.equals(null));
        
        final ReadOnlyObjectProperty<Object> golden3 = new ReadOnlyObjectPropertyStub(null, name1);
        assertTrue(golden3.equals(golden3));
        assertFalse(golden3.equals(new ReadOnlyObjectPropertyStub(null, name1)));
        assertFalse(golden3.equals(new ReadOnlyObjectPropertyStub(bean1, name1)));
        assertFalse(golden3.equals(new ReadOnlyObjectPropertyStub(null, "")));
        assertFalse(golden3.equals(bean1));
        assertFalse(golden3.equals(null));
        
        final ReadOnlyObjectProperty<Object> golden4 = new ReadOnlyObjectPropertyStub(null, "");
        assertTrue(golden4.equals(golden4));
        assertFalse(golden4.equals(new ReadOnlyObjectPropertyStub(null, name1)));
        assertFalse(golden4.equals(new ReadOnlyObjectPropertyStub(bean1, "")));
        assertFalse(golden4.equals(new ReadOnlyObjectPropertyStub(null, "")));
        assertFalse(golden4.equals(bean1));
        assertFalse(golden4.equals(null));
        
        final ReadOnlyObjectProperty<Object> golden5 = new ReadOnlyObjectPropertyStub(bean1, null);
        assertTrue(golden5.equals(golden5));
        assertFalse(golden5.equals(new ReadOnlyObjectPropertyStub(bean1, null)));
        assertFalse(golden5.equals(new ReadOnlyObjectPropertyStub(null, "")));
        assertFalse(golden5.equals(new ReadOnlyObjectPropertyStub(bean1, name1)));
        assertFalse(golden5.equals(bean1));
        assertFalse(golden5.equals(null));
        
        final ReadOnlyObjectProperty<Object> golden6 = new ReadOnlyObjectPropertyStub(null, null);
        assertTrue(golden6.equals(golden6));
        assertFalse(golden6.equals(new ReadOnlyObjectPropertyStub(null, name1)));
        assertFalse(golden6.equals(new ReadOnlyObjectPropertyStub(bean1, "")));
        assertFalse(golden6.equals(new ReadOnlyObjectPropertyStub(null, "")));
        assertFalse(golden6.equals(bean1));
        assertFalse(golden6.equals(null));
    }

    @Test
    public void testToString() {
        final ReadOnlyObjectProperty<Object> v1 = new ReadOnlyObjectPropertyStub(null, "");
        assertEquals("ReadOnlyObjectProperty [value: " + DEFAULT + "]", v1.toString());
        
        final ReadOnlyObjectProperty<Object> v2 = new ReadOnlyObjectPropertyStub(null, null);
        assertEquals("ReadOnlyObjectProperty [value: " + DEFAULT + "]", v2.toString());
        
        final Object bean = new Object();
        final String name = "My name";
        final ReadOnlyObjectProperty<Object> v3 = new ReadOnlyObjectPropertyStub(bean, name);
        assertEquals("ReadOnlyObjectProperty [bean: " + bean.toString() + ", name: My name, value: " + DEFAULT + "]", v3.toString());
        
        final ReadOnlyObjectProperty<Object> v4 = new ReadOnlyObjectPropertyStub(bean, "");
        assertEquals("ReadOnlyObjectProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v4.toString());
        
        final ReadOnlyObjectProperty<Object> v5 = new ReadOnlyObjectPropertyStub(bean, null);
        assertEquals("ReadOnlyObjectProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v5.toString());
        
        final ReadOnlyObjectProperty<Object> v6 = new ReadOnlyObjectPropertyStub(null, name);
        assertEquals("ReadOnlyObjectProperty [name: My name, value: " + DEFAULT + "]", v6.toString());
        
    }
    
    private static class ReadOnlyObjectPropertyStub extends ReadOnlyObjectProperty<Object> {
        
        private final Object bean;
        private final String name;
        
        private ReadOnlyObjectPropertyStub(Object bean, String name) {
            this.bean = bean;
            this.name = name;
        }

        @Override public Object getBean() { return bean; }
        @Override public String getName() { return name; }
        @Override public Object get() { return null; }

        @Override
        public void addListener(ChangeListener<? super Object> listener) {
        }

        @Override
        public void removeListener(ChangeListener<? super Object> listener) {
        }

        @Override
        public void addListener(InvalidationListener listener) {
        }

        @Override
        public void removeListener(InvalidationListener listener) {
        }
        
    }

}
