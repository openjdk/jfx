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

public class ReadOnlyStringPropertyTest {

    private static final String DEFAULT = null;

    @Before
    public void setUp() throws Exception {
    }


    @Test
    public void testToString() {
        final ReadOnlyStringProperty v1 = new ReadOnlyStringPropertyStub(null, "");
        assertEquals("ReadOnlyStringProperty [value: " + DEFAULT + "]", v1.toString());
        
        final ReadOnlyStringProperty v2 = new ReadOnlyStringPropertyStub(null, null);
        assertEquals("ReadOnlyStringProperty [value: " + DEFAULT + "]", v2.toString());
        
        final Object bean = new Object();
        final String name = "My name";
        final ReadOnlyStringProperty v3 = new ReadOnlyStringPropertyStub(bean, name);
        assertEquals("ReadOnlyStringProperty [bean: " + bean.toString() + ", name: My name, value: " + DEFAULT + "]", v3.toString());
        
        final ReadOnlyStringProperty v4 = new ReadOnlyStringPropertyStub(bean, "");
        assertEquals("ReadOnlyStringProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v4.toString());
        
        final ReadOnlyStringProperty v5 = new ReadOnlyStringPropertyStub(bean, null);
        assertEquals("ReadOnlyStringProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v5.toString());
        
        final ReadOnlyStringProperty v6 = new ReadOnlyStringPropertyStub(null, name);
        assertEquals("ReadOnlyStringProperty [name: My name, value: " + DEFAULT + "]", v6.toString());
        
    }
    
    private static class ReadOnlyStringPropertyStub extends ReadOnlyStringProperty {
        
        private final Object bean;
        private final String name;
        
        private ReadOnlyStringPropertyStub(Object bean, String name) {
            this.bean = bean;
            this.name = name;
        }

        @Override public Object getBean() { return bean; }
        @Override public String getName() { return name; }
        @Override public String get() { return null; }

        @Override
        public void addListener(ChangeListener<? super String> listener) {
        }

        @Override
        public void removeListener(ChangeListener<? super String> listener) {
        }

        @Override
        public void addListener(InvalidationListener listener) {
        }

        @Override
        public void removeListener(InvalidationListener listener) {
        }
        
    }

}
