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
