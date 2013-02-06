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
//package javafx.beans.property;
//
//import static org.junit.Assert.assertEquals;
//import javafx.binding.expression.BooleanExpression;
//import javafx.binding.expression.DoubleExpression;
//import javafx.binding.expression.FloatExpression;
//import javafx.binding.expression.IntegerExpression;
//import javafx.binding.expression.LongExpression;
//import javafx.binding.expression.ObjectExpression;
//import javafx.binding.expression.StringExpression;
//
//import org.junit.Before;
//import org.junit.Test;
//
//public class PropertiesTest {
//	
//	private static final double EPSILON_DOUBLE = 1e-12;
//	private static final float EPSILON_FLOAT = 1e-6f;
//
//	@Before
//	public void setUp() throws Exception {
//	}
//
//	@Test
//	public void testUnmodifiablePropertyBooleanProperty() {
//		final boolean value1 = true;
//		final boolean value2 = false;
//		final BooleanProperty source = new SimpleBooleanProperty(value1);
//		final BooleanExpression target = Properties.unmodifiableProperty(source);
//		
//		assertEquals(value1, target.get());
//		source.set(value2);
//		assertEquals(value2, target.get());
//		source.set(value1);
//		
//		assertEquals(value1, target.getValue());
//		source.set(value2);
//		assertEquals(value2, target.getValue());
//		source.set(value1);
//	}
//
//	@Test
//	public void testUnmodifiablePropertyDoubleProperty() {
//		final double value1 = Math.PI;
//		final double value2 = -Math.E;
//		final DoubleProperty source = new SimpleDoubleProperty(value1);
//		final DoubleExpression target = Properties.unmodifiableProperty(source);
//		
//		assertEquals(value1, target.get(), EPSILON_DOUBLE);
//		source.set(value2);
//		assertEquals(value2, target.get(), EPSILON_DOUBLE);
//		source.set(value1);
//		
//		assertEquals(value1, target.getValue(), EPSILON_DOUBLE);
//		source.set(value2);
//		assertEquals(value2, target.getValue(), EPSILON_DOUBLE);
//		source.set(value1);
//	}
//
//	@Test
//	public void testUnmodifiablePropertyFloatProperty() {
//		final float value1 = (float)Math.PI;
//		final float value2 = (float)-Math.E;
//		final FloatProperty source = new SimpleFloatProperty(value1);
//		final FloatExpression target = Properties.unmodifiableProperty(source);
//		
//		assertEquals(value1, target.get(), EPSILON_FLOAT);
//		source.set(value2);
//		assertEquals(value2, target.get(), EPSILON_FLOAT);
//		source.set(value1);
//		
//		assertEquals(value1, target.getValue(), EPSILON_FLOAT);
//		source.set(value2);
//		assertEquals(value2, target.getValue(), EPSILON_FLOAT);
//		source.set(value1);
//	}
//
//	@Test
//	public void testUnmodifiablePropertyIntegerProperty() {
//		final int value1 = 42;
//		final int value2 = 12345;
//		final IntegerProperty source = new SimpleIntegerProperty(value1);
//		final IntegerExpression target = Properties.unmodifiableProperty(source);
//		
//		assertEquals(value1, target.get());
//		source.set(value2);
//		assertEquals(value2, target.get());
//		source.set(value1);
//		
//		assertEquals(Integer.valueOf(value1), target.getValue());
//		source.set(value2);
//		assertEquals(Integer.valueOf(value2), target.getValue());
//		source.set(value1);
//	}
//
//	@Test
//	public void testUnmodifiablePropertyLongProperty() {
//		final long value1 = 98765432123456789L;
//		final long value2 = -1234567890987654321L;
//		final LongProperty source = new SimpleLongProperty(value1);
//		final LongExpression target = Properties.unmodifiableProperty(source);
//		
//		assertEquals(value1, target.get());
//		source.set(value2);
//		assertEquals(value2, target.get());
//		source.set(value1);
//		
//		assertEquals(Long.valueOf(value1), target.getValue());
//		source.set(value2);
//		assertEquals(Long.valueOf(value2), target.getValue());
//		source.set(value1);
//	}
//
//	@Test
//	public void testUnmodifiablePropertyObjectPropertyOfT() {
//		final Object value1 = new Object();
//		final Object value2 = new Object();
//		final ObjectProperty<Object> source = new SimpleObjectProperty<Object>(value1);
//		final ObjectExpression<Object> target = Properties.unmodifiableProperty(source);
//		
//		assertEquals(value1, target.get());
//		source.set(value2);
//		assertEquals(value2, target.get());
//		source.set(value1);
//		
//		assertEquals(value1, target.getValue());
//		source.set(value2);
//		assertEquals(value2, target.getValue());
//		source.set(value1);
//	}
//
//	@Test
//	public void testUnmodifiablePropertyStringProperty() {
//		final String value1 = "Hello World";
//		final String value2 = "Goodbye";
//		final StringProperty source = new SimpleStringProperty(value1);
//		final StringExpression target = Properties.unmodifiableProperty(source);
//		
//		assertEquals(value1, target.get());
//		source.set(value2);
//		assertEquals(value2, target.get());
//		source.set(value1);
//		
//		assertEquals(value1, target.getValue());
//		source.set(value2);
//		assertEquals(value2, target.getValue());
//		source.set(value1);
//	}
//
//}
