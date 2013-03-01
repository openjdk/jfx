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
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueStub;
import javafx.collections.FXCollections;

import org.junit.Before;
import org.junit.Test;

public class ReadOnlyBooleanPropertyTest {
    
    private static final boolean DEFAULT = false;

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testHashCode() {
		final Object bean = new Object();
		final String name = "Name1";
		
		final int golden1 = new ReadOnlyBooleanPropertyStub(bean, name).hashCode();
		assertEquals(golden1, golden1);
		assertEquals(golden1, new ReadOnlyBooleanPropertyStub(bean, name).hashCode());
		
		final int golden2 = new ReadOnlyBooleanPropertyStub(null, name).hashCode();
		assertEquals(golden2, golden2);
		assertEquals(golden2, new ReadOnlyBooleanPropertyStub(null, name).hashCode());
        
        final int golden3 = new ReadOnlyBooleanPropertyStub(bean, "").hashCode();
        assertEquals(golden3, golden3);
        assertEquals(golden3, new ReadOnlyBooleanPropertyStub(bean, "").hashCode());
        
        final int golden4 = new ReadOnlyBooleanPropertyStub(null, "").hashCode();
        assertEquals(golden4, golden4);
        
        final int golden5 = new ReadOnlyBooleanPropertyStub(bean, null).hashCode();
        assertEquals(golden5, golden5);
        assertEquals(golden5, new ReadOnlyBooleanPropertyStub(bean, null).hashCode());
        
        final int golden6 = new ReadOnlyBooleanPropertyStub(null, null).hashCode();
        assertEquals(golden6, golden6);
	}

	@Test
	public void testEqualsObject() {
		final Object bean1 = new Object();
		final Object bean2 = new Object();
		final String name1 = "Name1";
		final String name2 = "Name2";
		
		final ReadOnlyBooleanProperty golden1 = new ReadOnlyBooleanPropertyStub(bean1, name1);
		assertTrue(golden1.equals(golden1));
		assertTrue(golden1.equals(new ReadOnlyBooleanPropertyStub(bean1, name1)));
		assertFalse(golden1.equals(new ReadOnlyBooleanPropertyStub(bean2, name1)));
		assertFalse(golden1.equals(new ReadOnlyBooleanPropertyStub(bean1, name2)));
		assertFalse(golden1.equals(new ReadOnlyBooleanPropertyStub(null, name1)));
		assertFalse(golden1.equals(new ReadOnlyBooleanPropertyStub(bean1, "")));
		assertFalse(golden1.equals(new ReadOnlyBooleanPropertyStub(null, "")));
		assertFalse(golden1.equals(bean1));
		assertFalse(golden1.equals(null));
		
        final ReadOnlyBooleanProperty golden2 = new ReadOnlyBooleanPropertyStub(bean1, "");
        assertTrue(golden2.equals(golden2));
        assertFalse(golden2.equals(new ReadOnlyBooleanPropertyStub(bean1, "")));
        assertFalse(golden2.equals(new ReadOnlyBooleanPropertyStub(null, "")));
        assertFalse(golden2.equals(new ReadOnlyBooleanPropertyStub(bean1, name1)));
        assertFalse(golden2.equals(bean1));
        assertFalse(golden2.equals(null));
        
		final ReadOnlyBooleanProperty golden3 = new ReadOnlyBooleanPropertyStub(null, name1);
		assertTrue(golden3.equals(golden3));
		assertFalse(golden3.equals(new ReadOnlyBooleanPropertyStub(null, name1)));
		assertFalse(golden3.equals(new ReadOnlyBooleanPropertyStub(bean1, name1)));
		assertFalse(golden3.equals(new ReadOnlyBooleanPropertyStub(null, "")));
		assertFalse(golden3.equals(bean1));
		assertFalse(golden3.equals(null));
		
		final ReadOnlyBooleanProperty golden4 = new ReadOnlyBooleanPropertyStub(null, "");
		assertTrue(golden4.equals(golden4));
		assertFalse(golden4.equals(new ReadOnlyBooleanPropertyStub(null, name1)));
		assertFalse(golden4.equals(new ReadOnlyBooleanPropertyStub(bean1, "")));
		assertFalse(golden4.equals(new ReadOnlyBooleanPropertyStub(null, "")));
		assertFalse(golden4.equals(bean1));
		assertFalse(golden4.equals(null));
		
        final ReadOnlyBooleanProperty golden5 = new ReadOnlyBooleanPropertyStub(bean1, null);
        assertTrue(golden5.equals(golden5));
        assertFalse(golden5.equals(new ReadOnlyBooleanPropertyStub(bean1, null)));
        assertFalse(golden5.equals(new ReadOnlyBooleanPropertyStub(null, "")));
        assertFalse(golden5.equals(new ReadOnlyBooleanPropertyStub(bean1, name1)));
        assertFalse(golden5.equals(bean1));
        assertFalse(golden5.equals(null));
        
        final ReadOnlyBooleanProperty golden6 = new ReadOnlyBooleanPropertyStub(null, null);
        assertTrue(golden6.equals(golden6));
        assertFalse(golden6.equals(new ReadOnlyBooleanPropertyStub(null, name1)));
        assertFalse(golden6.equals(new ReadOnlyBooleanPropertyStub(bean1, "")));
        assertFalse(golden6.equals(new ReadOnlyBooleanPropertyStub(null, "")));
        assertFalse(golden6.equals(bean1));
        assertFalse(golden6.equals(null));
	}

	@Test
	public void testToString() {
        final ReadOnlyBooleanProperty v1 = new ReadOnlyBooleanPropertyStub(null, "");
        assertEquals("ReadOnlyBooleanProperty [value: " + DEFAULT + "]", v1.toString());
        
        final ReadOnlyBooleanProperty v2 = new ReadOnlyBooleanPropertyStub(null, null);
        assertEquals("ReadOnlyBooleanProperty [value: " + DEFAULT + "]", v2.toString());
        
		final Object bean = new Object();
		final String name = "My name";
        final ReadOnlyBooleanProperty v3 = new ReadOnlyBooleanPropertyStub(bean, name);
        assertEquals("ReadOnlyBooleanProperty [bean: " + bean.toString() + ", name: My name, value: " + DEFAULT + "]", v3.toString());
        
        final ReadOnlyBooleanProperty v4 = new ReadOnlyBooleanPropertyStub(bean, "");
        assertEquals("ReadOnlyBooleanProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v4.toString());
        
        final ReadOnlyBooleanProperty v5 = new ReadOnlyBooleanPropertyStub(bean, null);
        assertEquals("ReadOnlyBooleanProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v5.toString());
        
        final ReadOnlyBooleanProperty v6 = new ReadOnlyBooleanPropertyStub(null, name);
        assertEquals("ReadOnlyBooleanProperty [name: My name, value: " + DEFAULT + "]", v6.toString());
        
	}

    @Test
    public void testAsObject() {
        final ReadOnlyBooleanWrapper valueModel = new ReadOnlyBooleanWrapper();
        final ReadOnlyObjectProperty<Boolean> exp = valueModel.getReadOnlyProperty().asObject();

        assertEquals(Boolean.FALSE, exp.get());
        valueModel.set(true);
        assertEquals(Boolean.TRUE, exp.get());
        valueModel.set(false);
        assertEquals(Boolean.FALSE, exp.get());
    }
    
    @Test
    public void testObjectToBoolean() {
        final ReadOnlyObjectWrapper<Boolean> valueModel = new ReadOnlyObjectWrapper<Boolean>();
        final ReadOnlyBooleanProperty exp = ReadOnlyBooleanProperty.readOnlyBooleanProperty(valueModel.getReadOnlyProperty());
        

        assertEquals(false, exp.get());
        valueModel.set(true);
        assertEquals(true, exp.get());
        valueModel.set(false);
        assertEquals(false, exp.get());
    }


	private static class ReadOnlyBooleanPropertyStub extends ReadOnlyBooleanProperty {
		
		private final Object bean;
		private final String name;
		
		private ReadOnlyBooleanPropertyStub(Object bean, String name) {
			this.bean = bean;
			this.name = name;
		}

		@Override public Object getBean() { return bean; }
		@Override public String getName() { return name; }
		@Override public boolean get() { return false; }

        @Override
        public void addListener(ChangeListener<? super Boolean> listener) {
        }

        @Override
        public void removeListener(ChangeListener<? super Boolean> listener) {
        }

        @Override
        public void addListener(InvalidationListener listener) {
        }

        @Override
        public void removeListener(InvalidationListener listener) {
        }
		
	}

}
