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
package com.sun.javafx.binding;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.IllegalFormatException;
import java.util.Locale;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.binding.DependencyUtils;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("WebTest")
public class StringFormatterTest {

	private double double0;
	private float float0;
	private long long0;
	private int int0;
	private boolean boolean0;
	private String string0;
	private Date date0;

	private double double1;
	private float float1;
	private long long1;
	private int int1;
	private boolean boolean1;
	private String string1;
	private Date date1;

	private DoubleProperty doubleV;
	private FloatProperty floatV;
	private LongProperty longV;
	private IntegerProperty intV;
	private BooleanProperty booleanV;
	private StringProperty stringV;
	private ObjectProperty<Date> dateV;

	@Before
	public void setUp() {
		double0 = 90.1243987;
		float0 = -897.4509f;
		long0 = -88793289779238972L;
		int0 = -9872498;
		boolean0 = false;
		string0 = "Hello";
		date0 = new Date(0);

		double1 = 23341.9823;
		float1 = 989823.8723f;
		long1 = 98234892892389233L;
		int1 = -897237894;
		boolean1 = true;
		string1 = "Goodbye";
		date1 = new Date();

		doubleV = new SimpleDoubleProperty(double0);
		floatV = new SimpleFloatProperty(float0);
		longV = new SimpleLongProperty(long0);
		intV = new SimpleIntegerProperty(int0);
		booleanV = new SimpleBooleanProperty(boolean0);
		stringV = new SimpleStringProperty(string0);
		dateV = new SimpleObjectProperty<Date>(date0);
	}

	@Test
	public void testConvert() {
		StringExpression s = Bindings.convert(doubleV);
		DependencyUtils.checkDependencies(((StringBinding)s).getDependencies(), doubleV);
		assertEquals(Double.toString(double0), s.get());
		doubleV.set(double1);
		assertEquals(Double.toString(double1), s.get());

		s = Bindings.convert(floatV);
		DependencyUtils.checkDependencies(((StringBinding)s).getDependencies(), floatV);
		assertEquals(Float.toString(float0), s.get());
		floatV.set(float1);
		assertEquals(Float.toString(float1), s.get());

		s = Bindings.convert(longV);
		DependencyUtils.checkDependencies(((StringBinding)s).getDependencies(), longV);
		assertEquals(Long.toString(long0), s.get());
		longV.set(long1);
		assertEquals(Long.toString(long1), s.get());

		s = Bindings.convert(intV);
		DependencyUtils.checkDependencies(((StringBinding)s).getDependencies(), intV);
		assertEquals(Integer.toString(int0), s.get());
		intV.set(int1);
		assertEquals(Integer.toString(int1), s.get());

		s = Bindings.convert(booleanV);
		DependencyUtils.checkDependencies(((StringBinding)s).getDependencies(), booleanV);
		assertEquals(Boolean.toString(boolean0), s.get());
		booleanV.set(boolean1);
		assertEquals(Boolean.toString(boolean1), s.get());

		s = Bindings.convert(dateV);
		DependencyUtils.checkDependencies(((StringBinding)s).getDependencies(), dateV);
		assertEquals(date0.toString(), s.get());
		dateV.set(date1);
		assertEquals(date1.toString(), s.get());
		dateV.set(null);
		assertEquals("null", s.get());

		s = Bindings.convert(stringV);
		assertEquals(stringV, s);
	}

	@Test(expected=NullPointerException.class)
	public void testConvert_Null() {
		Bindings.convert(null);
	}

	@Test
	public void testConcat() {
		// empty
		StringExpression s = Bindings.concat();
		assertEquals("", s.get());
		s = Bindings.concat(new Object[0]);
		assertEquals("", s.get());

		// one object
		s = Bindings.concat(date1);
		assertEquals(date1.toString(), s.get());

		// one variable
		s = Bindings.concat(dateV);
		DependencyUtils.checkDependencies(((StringBinding)s).getDependencies(), dateV);
		assertEquals(date0.toString(), s.get());
		dateV.set(date1);
		assertEquals(date1.toString(), s.get());
		dateV.set(null);
		assertEquals("null", s.get());
		dateV.set(date0);

		// two objects
		s = Bindings.concat(date0, date1);
		assertEquals(date0.toString() + date1.toString(), s.get());

		// test all
		s = Bindings.concat(doubleV, double1, floatV, float1, longV, long1, intV, int1, booleanV, boolean1, stringV, string1, dateV, date1);
		DependencyUtils.checkDependencies(((StringBinding)s).getDependencies(), doubleV, floatV, longV, intV, booleanV, stringV, dateV);
		assertEquals("" + double0 + double1 + float0 + float1 + long0 + long1 + int0 + int1+ boolean0 + boolean1 + string0 + string1 + date0 + date1, s.get());
        doubleV.set(double1);
        floatV.set(float1);
        longV.set(long1);
        intV.set(int1);
        booleanV.set(boolean1);
        stringV.set(string1);
        dateV.set(date1);
        assertEquals("" + double1 + double1 + float1 + float1 + long1 + long1 + int1 + int1 + boolean1 + boolean1 + string1 + string1 + date1 + date1, s.get());
        stringV.set(null);
        dateV.set(null);
        assertEquals("" + double1 + double1 + float1 + float1 + long1 + long1 + int1 + int1 + boolean1 + boolean1 + "null" + string1 + "null" + date1, s.get());
	}

	@Test
	public void testConvertWithDefaultLocale() {
		// empty
		StringExpression s = Bindings.format("Empty String");
		assertEquals("Empty String", s.get());

		final Locale defaultLocale = Locale.getDefault();
		try {
			// German Locale
			Locale.setDefault(Locale.GERMAN);
			// one object
			s = Bindings.format("Date: %tc", date1);
			assertEquals(String.format(Locale.GERMAN, "Date: %tc", date1), s.get());
			// one variable
			s = Bindings.format("Date: %tc", dateV);
			DependencyUtils.checkDependencies(((StringBinding)s).getDependencies(), dateV);
			assertEquals(String.format(Locale.GERMAN, "Date: %tc", date0), s.get());
            dateV.set(date1);
            assertEquals(String.format(Locale.GERMAN, "Date: %tc", date1), s.get());
            dateV.set(null);
            assertEquals("Date: null", s.get());
            dateV.set(date0);

			// test all
			s = Bindings.format("%8.3e, %8.3e, %6.3f, %6.3f, %d, %d, %d, %d, %s, %s, %s, %s, %tc, %tc",
					doubleV, double1, floatV, float1, longV, long1, intV, int1, booleanV, boolean1, stringV, string1, dateV, date1);
			DependencyUtils.checkDependencies(((StringBinding)s).getDependencies(), doubleV, floatV, longV, intV, booleanV, stringV, dateV);
			assertEquals(String.format(Locale.GERMAN, "%8.3e, %8.3e, %6.3f, %6.3f, %d, %d, %d, %d, %s, %s, %s, %s, %tc, %tc",
					double0, double1, float0, float1, long0, long1, int0, int1, boolean0, boolean1, string0, string1, date0, date1), s.get());
			doubleV.set(double1);
			floatV.set(float1);
			longV.set(long1);
			intV.set(int1);
			booleanV.set(boolean1);
            stringV.set(string1);
            dateV.set(date1);
            assertEquals(String.format(Locale.GERMAN, "%8.3e, %8.3e, %6.3f, %6.3f, %d, %d, %d, %d, %s, %s, %s, %s, %tc, %tc",
                    double1, double1, float1, float1, long1, long1, int1, int1, boolean1, boolean1, string1, string1, date1, date1), s.get());
            stringV.set(null);
            dateV.set(null);
            assertEquals(String.format(Locale.GERMAN, "%8.3e, %8.3e, %6.3f, %6.3f, %d, %d, %d, %d, %s, %s, %s, %s, %s, %tc",
                    double1, double1, float1, float1, long1, long1, int1, int1, boolean1, boolean1, "null", string1, "null", date1), s.get());

			// US Locale
			Locale.setDefault(Locale.US);
			doubleV.set(double0);
			floatV.set(float0);
			longV.set(long0);
			intV.set(int0);
			booleanV.set(boolean0);
			stringV.set(string0);
			dateV.set(date0);
			// one object
			s = Bindings.format("Date: %tc", date1);
			assertEquals(String.format(Locale.US, "Date: %tc", date1), s.get());
			// one variable
			s = Bindings.format("Date: %tc", dateV);
			DependencyUtils.checkDependencies(((StringBinding)s).getDependencies(), dateV);
			assertEquals(String.format(Locale.US, "Date: %tc", date0), s.get());
			dateV.set(date1);
			assertEquals(String.format(Locale.US, "Date: %tc", date1), s.get());
			dateV.set(null);
			assertEquals("Date: null", s.get());
			dateV.set(date0);

			// test all
			s = Bindings.format("%8.3e, %8.3e, %6.3f, %6.3f, %d, %d, %d, %d, %s, %s, %s, %s, %tc, %tc",
					doubleV, double1, floatV, float1, longV, long1, intV, int1, booleanV, boolean1, stringV, string1, dateV, date1);
			DependencyUtils.checkDependencies(((StringBinding)s).getDependencies(), doubleV, floatV, longV, intV, booleanV, stringV, dateV);
			assertEquals(String.format(Locale.US, "%8.3e, %8.3e, %6.3f, %6.3f, %d, %d, %d, %d, %s, %s, %s, %s, %tc, %tc",
					double0, double1, float0, float1, long0, long1, int0, int1, boolean0, boolean1, string0, string1, date0, date1), s.get());
			doubleV.set(double1);
			floatV.set(float1);
			longV.set(long1);
			intV.set(int1);
			booleanV.set(boolean1);
            stringV.set(string1);
            dateV.set(date1);
            assertEquals(String.format(Locale.US, "%8.3e, %8.3e, %6.3f, %6.3f, %d, %d, %d, %d, %s, %s, %s, %s, %tc, %tc",
                    double1, double1, float1, float1, long1, long1, int1, int1, boolean1, boolean1, string1, string1, date1, date1), s.get());
            stringV.set(null);
            dateV.set(null);
            assertEquals(String.format(Locale.US, "%8.3e, %8.3e, %6.3f, %6.3f, %d, %d, %d, %d, %s, %s, %s, %s, %s, %tc",
                    double1, double1, float1, float1, long1, long1, int1, int1, boolean1, boolean1, "null", string1, "null", date1), s.get());
		} finally {
			Locale.setDefault(defaultLocale);
		}
	}

	@Test(expected=NullPointerException.class)
	public void testConvertWithDefaultLocale_Null() {
		Bindings.format(null);
	}

	@Test(expected=IllegalFormatException.class)
	public void testConvertWithDefaultLocale_IllegalObject() {
		Bindings.format("%tc", double0);
	}

	@Test(expected=IllegalFormatException.class)
	public void testConvertWithDefaultLocale_IllegalValueModel() {
		Bindings.format("%tc", doubleV);
	}

	@Test
	public void testConvertWithCustomLocale() {
		// German Locale
		// empty
		StringExpression s = Bindings.format(Locale.GERMAN, "Empty String");
		assertEquals("Empty String", s.get());
		// one object
		s = Bindings.format(Locale.GERMAN, "Date: %tc", date1);
		assertEquals(String.format(Locale.GERMAN, "Date: %tc", date1), s.get());
		// one variable
		s = Bindings.format(Locale.GERMAN, "Date: %tc", dateV);
		DependencyUtils.checkDependencies(((StringBinding)s).getDependencies(), dateV);
		assertEquals(String.format(Locale.GERMAN, "Date: %tc", date0), s.get());
		dateV.set(date1);
		assertEquals(String.format(Locale.GERMAN, "Date: %tc", date1), s.get());
        dateV.set(null);
        assertEquals("Date: null", s.get());
        dateV.set(date0);

		// test all
		s = Bindings.format(Locale.GERMAN, "%8.3e, %8.3e, %6.3f, %6.3f, %d, %d, %d, %d, %s, %s, %s, %s, %tc, %tc",
				doubleV, double1, floatV, float1, longV, long1, intV, int1, booleanV, boolean1, stringV, string1, dateV, date1);
		DependencyUtils.checkDependencies(((StringBinding)s).getDependencies(), doubleV, floatV, longV, intV, booleanV, stringV, dateV);
		assertEquals(String.format(Locale.GERMAN, "%8.3e, %8.3e, %6.3f, %6.3f, %d, %d, %d, %d, %s, %s, %s, %s, %tc, %tc",
				double0, double1, float0, float1, long0, long1, int0, int1, boolean0, boolean1, string0, string1, date0, date1), s.get());
		doubleV.set(double1);
		floatV.set(float1);
		longV.set(long1);
		intV.set(int1);
		booleanV.set(boolean1);
        stringV.set(string1);
        dateV.set(date1);
        assertEquals(String.format(Locale.GERMAN, "%8.3e, %8.3e, %6.3f, %6.3f, %d, %d, %d, %d, %s, %s, %s, %s, %tc, %tc",
                double1, double1, float1, float1, long1, long1, int1, int1, boolean1, boolean1, string1, string1, date1, date1), s.get());
        stringV.set(null);
        dateV.set(null);
        assertEquals(String.format(Locale.GERMAN, "%8.3e, %8.3e, %6.3f, %6.3f, %d, %d, %d, %d, %s, %s, %s, %s, %s, %tc",
                double1, double1, float1, float1, long1, long1, int1, int1, boolean1, boolean1, "null", string1, "null", date1), s.get());

		// US Locale
		doubleV.set(double0);
		floatV.set(float0);
		longV.set(long0);
		intV.set(int0);
		booleanV.set(boolean0);
		stringV.set(string0);
		dateV.set(date0);
		// empty
		s = Bindings.format(Locale.US, "Empty String");
		assertEquals("Empty String", s.get());
		// one object
		s = Bindings.format(Locale.US, "Date: %tc", date1);
		assertEquals(String.format(Locale.US, "Date: %tc", date1), s.get());
		// one variable
		s = Bindings.format(Locale.US, "Date: %tc", dateV);
		DependencyUtils.checkDependencies(((StringBinding)s).getDependencies(), dateV);
		assertEquals(String.format(Locale.US, "Date: %tc", date0), s.get());
		dateV.set(date1);
		assertEquals(String.format(Locale.US, "Date: %tc", date1), s.get());
        dateV.set(null);
        assertEquals("Date: null", s.get());
        dateV.set(date0);

		// test all
		s = Bindings.format(Locale.US, "%8.3e, %8.3e, %6.3f, %6.3f, %d, %d, %d, %d, %s, %s, %s, %s, %tc, %tc",
				doubleV, double1, floatV, float1, longV, long1, intV, int1, booleanV, boolean1, stringV, string1, dateV, date1);
		DependencyUtils.checkDependencies(((StringBinding)s).getDependencies(), doubleV, floatV, longV, intV, booleanV, stringV, dateV);
		assertEquals(String.format(Locale.US, "%8.3e, %8.3e, %6.3f, %6.3f, %d, %d, %d, %d, %s, %s, %s, %s, %tc, %tc",
				double0, double1, float0, float1, long0, long1, int0, int1, boolean0, boolean1, string0, string1, date0, date1), s.get());
		doubleV.set(double1);
		floatV.set(float1);
		longV.set(long1);
		intV.set(int1);
		booleanV.set(boolean1);
		stringV.set(string1);
		dateV.set(date1);
		assertEquals(String.format(Locale.US, "%8.3e, %8.3e, %6.3f, %6.3f, %d, %d, %d, %d, %s, %s, %s, %s, %tc, %tc",
				double1, double1, float1, float1, long1, long1, int1, int1, boolean1, boolean1, string1, string1, date1, date1), s.get());
        stringV.set(null);
        dateV.set(null);
        assertEquals(String.format(Locale.US, "%8.3e, %8.3e, %6.3f, %6.3f, %d, %d, %d, %d, %s, %s, %s, %s, %s, %tc",
                double1, double1, float1, float1, long1, long1, int1, int1, boolean1, boolean1, "null", string1, "null", date1), s.get());
	}

	@Test(expected=NullPointerException.class)
	public void testConvertWithCustomLocale_Null() {
		Bindings.format(Locale.US, null);
	}

	@Test(expected=IllegalFormatException.class)
	public void testConvertWithCustomLocale_IllegalObject() {
		Bindings.format(Locale.US, "%tc", double0);
	}

	@Test(expected=IllegalFormatException.class)
	public void testConvertWithCustomLocale_IllegalValueModel() {
		Bindings.format(Locale.US, "%tc", doubleV);
	}
}
