/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Test;

public class SimpleDoublePropertyTest {
	
	private static final Object DEFAULT_BEAN = null;
	private static final String DEFAULT_NAME = "";
    private static final double DEFAULT_VALUE = 0.0;
    private static final double VALUE_1 = -Math.E;
    private static final double EPSILON = 1e-12;

	@Test
	public void testConstructor_NoArguments() {
		final DoubleProperty v = new SimpleDoubleProperty();
		assertEquals(DEFAULT_BEAN, v.getBean());
		assertEquals(DEFAULT_NAME, v.getName());
		assertEquals(DEFAULT_VALUE, v.get(), EPSILON);
	}

	@Test
	public void testConstructor_InitialValue() {
		final DoubleProperty v1 = new SimpleDoubleProperty(VALUE_1);
		assertEquals(DEFAULT_BEAN, v1.getBean());
		assertEquals(DEFAULT_NAME, v1.getName());
		assertEquals(VALUE_1, v1.get(), EPSILON);

		final DoubleProperty v2 = new SimpleDoubleProperty(DEFAULT_VALUE);
		assertEquals(DEFAULT_BEAN, v2.getBean());
		assertEquals(DEFAULT_NAME, v2.getName());
		assertEquals(DEFAULT_VALUE, v2.get(), EPSILON);
	}

	@Test
	public void testConstructor_Bean_Name() {
		final Object bean = new Object();
		final String name = "My name";
		final DoubleProperty v = new SimpleDoubleProperty(bean, name);
		assertEquals(bean, v.getBean());
		assertEquals(name, v.getName());
		assertEquals(DEFAULT_VALUE, v.get(), EPSILON);
		
		final DoubleProperty v2 = new SimpleDoubleProperty(bean, null);
        assertEquals(bean, v2.getBean());
        assertEquals(DEFAULT_NAME, v2.getName());
        assertEquals(DEFAULT_VALUE, v2.get(), EPSILON);
	}

	@Test
	public void testConstructor_Bean_Name_InitialValue() {
		final Object bean = new Object();
		final String name = "My name";
		final DoubleProperty v1 = new SimpleDoubleProperty(bean, name, VALUE_1);
		assertEquals(bean, v1.getBean());
		assertEquals(name, v1.getName());
		assertEquals(VALUE_1, v1.get(), EPSILON);

		final DoubleProperty v2 = new SimpleDoubleProperty(bean, name, DEFAULT_VALUE);
		assertEquals(bean, v2.getBean());
		assertEquals(name, v2.getName());
		assertEquals(DEFAULT_VALUE, v2.get(), EPSILON);
        
        final DoubleProperty v3 = new SimpleDoubleProperty(bean, null, VALUE_1);
        assertEquals(bean, v3.getBean());
        assertEquals(DEFAULT_NAME, v3.getName());
        assertEquals(VALUE_1, v3.get(), EPSILON);
        
        final DoubleProperty v4 = new SimpleDoubleProperty(bean, null, DEFAULT_VALUE);
        assertEquals(bean, v4.getBean());
        assertEquals(DEFAULT_NAME, v4.getName());
        assertEquals(DEFAULT_VALUE, v4.get(), EPSILON);
	}
}
